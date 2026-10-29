
package com.oghamlab.meshchat.nostr.identity.relay


import com.oghamlab.meshchat.nostr.identity.protocol.NostrFilter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class RelayPool(
    private val scope: CoroutineScope
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    private val connections =
        mutableMapOf<String, RelayConnection>()

    private val jobs = mutableMapOf<String, List<Job>>()

    private val _relays =
        MutableStateFlow<List<RelayInfo>>(emptyList())

    val relays: StateFlow<List<RelayInfo>> =
        _relays.asStateFlow()

    private val _eventCount = MutableStateFlow(0)
    val eventCount = _eventCount.asStateFlow()

    private val seenIds = LinkedHashSet<String>()

    fun addRelay(url: String) {
        val normalized = url.trim()

        val uri = java.net.URI(normalized)
        require(uri.scheme == "wss")
        require(!uri.host.isNullOrBlank())
        require(uri.userInfo == null)
        require(uri.fragment == null)
        require(connections.size < 10)

        if (connections.containsKey(normalized)) return

        val connection = RelayConnection(
            normalized,
            client,
            scope
        )

        connections[normalized] = connection

        val stateJob = scope.launch {
            connection.info.collect {
                updateRelayStates()
            }
        }

        val eventJob = scope.launch {
            connection.events.collect { received ->
                // Counting only. Events are NOT verified yet.
                val id = received.event.id

                if (seenIds.add(id)) {
                    _eventCount.value++
                }

                if (seenIds.size > 2_000) {
                    val oldest = seenIds.first()
                    seenIds.remove(oldest)
                }
            }
        }

        jobs[normalized] = listOf(stateJob, eventJob)
        updateRelayStates()
        connection.connect()
    }

    fun removeRelay(url: String) {
        connections.remove(url)?.disconnect()
        jobs.remove(url)?.forEach { it.cancel() }
        updateRelayStates()
    }

    fun subscribePublicEvents() {
        val filter = NostrFilter(
            kinds = listOf(1),
            limit = 20
        )

        connections.values.forEach {
            it.subscribe("meshchat-test", filter)
        }
    }

    fun disconnectAll() {
        connections.values.forEach { it.disconnect() }
        updateRelayStates()
    }

    fun reconnectAll() {
        connections.values.forEach { it.connect() }
    }

    private fun updateRelayStates() {
        _relays.value = connections.values.map {
            it.info.value
        }
    }

    fun close() {
        connections.values.forEach { it.disconnect() }
        jobs.values.flatten().forEach { it.cancel() }
        connections.clear()
        jobs.clear()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}
