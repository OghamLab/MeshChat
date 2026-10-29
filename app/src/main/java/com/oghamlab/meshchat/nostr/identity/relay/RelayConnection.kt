
package com.oghamlab.meshchat.nostr.identity.relay

import com.oghamlab.meshchat.nostr.identity.protocol.NostrEvent
import com.oghamlab.meshchat.nostr.identity.protocol.NostrFilter
import com.oghamlab.meshchat.nostr.identity.protocol.NostrProtocol

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import kotlin.math.min

class RelayConnection(
    val url: String,
    private val client: OkHttpClient,
    private val scope: CoroutineScope
) {
    private val _info = MutableStateFlow(RelayInfo(url))
    val info: StateFlow<RelayInfo> = _info.asStateFlow()

    private val _events = MutableSharedFlow<RelayEvent>(
        extraBufferCapacity = 64
    )
    val events = _events.asSharedFlow()

    private val _acks = MutableSharedFlow<RelayAcknowledgment>(
        extraBufferCapacity = 16
    )
    val acknowledgments = _acks.asSharedFlow()

    private var socket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var generation = 0
    private var active = false
    private var attempts = 0

    private val subscriptions =
        mutableMapOf<String, NostrFilter>()

    fun connect() {
        if (active) return
        active = true
        attempts = 0
        openConnection()
    }

    private fun openConnection() {
        if (!active) return

        val token = ++generation

        _info.value = _info.value.copy(
            status = if (attempts == 0)
                RelayStatus.CONNECTING
            else RelayStatus.RECONNECTING,
            lastError = null,
            historyComplete = false
        )

        val request = Request.Builder()
            .url(url)
            .build()

        socket = client.newWebSocket(
            request,
            object : WebSocketListener() {

                override fun onOpen(
                    webSocket: WebSocket,
                    response: Response
                ) {
                    if (!active || token != generation) {
                        webSocket.close(1000, null)
                        return
                    }

                    attempts = 0
                    _info.value = _info.value.copy(
                        status = RelayStatus.CONNECTED,
                        lastError = null
                    )

                    subscriptions.forEach { (id, filter) ->
                        webSocket.send(
                            NostrProtocol.subscribe(id, filter)
                        )
                    }
                }

                override fun onMessage(
                    webSocket: WebSocket,
                    text: String
                ) {
                    if (active && token == generation) {
                        handleMessage(text)
                    }
                }

                override fun onFailure(
                    webSocket: WebSocket,
                    t: Throwable,
                    response: Response?
                ) {
                    if (active && token == generation) {
                        scheduleReconnect(
                            t.message ?: "Connection failed"
                        )
                    }
                }

                override fun onClosed(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String
                ) {
                    if (active && token == generation) {
                        if (code == 4000) {
                            active = false
                            _info.value = _info.value.copy(
                                status = RelayStatus.ERROR,
                                lastError = "Relay refused reconnection"
                            )
                        } else {
                            scheduleReconnect(
                                "Connection closed: $code"
                            )
                        }
                    }
                }
            }
        )
    }

    private fun handleMessage(text: String) {
        if (text.length > 256_000) return

        try {
            val message = JSONArray(text)
            if (message.length() == 0) return

            when (message.getString(0)) {
                "EVENT" -> {
                    if (message.length() != 3) return
                    val subscriptionId = message.getString(1)
                    if (!subscriptions.containsKey(subscriptionId)) {
                        return
                    }

                    val event = NostrEvent.fromJson(
                        message.getJSONObject(2)
                    )

                    // Important: events are unverified at this stage.
                    // Never decrypt, render as trusted, or forward them.
                    _info.value = _info.value.copy(
                        receivedEvents =
                            _info.value.receivedEvents + 1
                    )

                    _events.tryEmit(RelayEvent(url, event))
                }

                "EOSE" -> {
                    if (message.length() < 2) return
                    if (subscriptions.containsKey(
                            message.getString(1)
                        )
                    ) {
                        _info.value = _info.value.copy(
                            historyComplete = true
                        )
                    }
                }

                "OK" -> {
                    if (message.length() < 4) return
                    _acks.tryEmit(
                        RelayAcknowledgment(
                            relayUrl = url,
                            eventId = message.getString(1),
                            accepted = message.getBoolean(2),
                            message = message.getString(3)
                        )
                    )
                }

                "NOTICE" -> {
                    if (message.length() >= 2) {
                        _info.value = _info.value.copy(
                            lastError = message.getString(1)
                                .take(200)
                        )
                    }
                }

                "CLOSED" -> {
                    if (message.length() >= 3) {
                        _info.value = _info.value.copy(
                            lastError = message.getString(2)
                                .take(200),
                            historyComplete = false
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore malformed relay messages.
        }
    }

    fun subscribe(id: String, filter: NostrFilter) {
        require(id.isNotBlank() && id.length <= 64)
        subscriptions[id] = filter

        if (_info.value.status == RelayStatus.CONNECTED) {
            socket?.send(NostrProtocol.subscribe(id, filter))
        }
    }

    fun unsubscribe(id: String) {
        subscriptions.remove(id)

        if (_info.value.status == RelayStatus.CONNECTED) {
            socket?.send(NostrProtocol.close(id))
        }
    }

    fun publish(event: NostrEvent): Boolean {
        if (_info.value.status != RelayStatus.CONNECTED) {
            return false
        }
        return socket?.send(NostrProtocol.publish(event)) == true
    }

    private fun scheduleReconnect(reason: String) {
        if (!active) return

        _info.value = _info.value.copy(
            status = RelayStatus.RECONNECTING,
            lastError = reason,
            historyComplete = false
        )

        reconnectJob?.cancel()

        val delayMs = min(
            60_000L,
            1_000L shl min(attempts, 6)
        )
        attempts++

        reconnectJob = scope.launch {
            delay(delayMs)
            if (active) openConnection()
        }
    }

    fun disconnect() {
        active = false
        generation++
        reconnectJob?.cancel()
        socket?.close(1000, "Client disconnected")
        socket = null

        _info.value = _info.value.copy(
            status = RelayStatus.DISCONNECTED,
            lastError = null,
            historyComplete = false
        )
    }
}
