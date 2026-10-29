
package com.oghamlab.meshchat.nostr.identity.relay

import com.oghamlab.meshchat.nostr.identity.protocol.NostrEvent

enum class RelayStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

data class RelayInfo(
    val url: String,
    val status: RelayStatus = RelayStatus.DISCONNECTED,
    val lastError: String? = null,
    val receivedEvents: Int = 0,
    val historyComplete: Boolean = false
)

data class RelayEvent(
    val relayUrl: String,
    val event: NostrEvent
)

data class RelayAcknowledgment(
    val relayUrl: String,
    val eventId: String,
    val accepted: Boolean,
    val message: String
)
