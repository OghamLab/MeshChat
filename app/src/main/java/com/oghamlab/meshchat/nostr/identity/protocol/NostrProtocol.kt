package com.oghamlab.meshchat.nostr.identity.protocol



import org.json.JSONArray

object NostrProtocol {

    fun subscribe(
        subscriptionId: String,
        filter: NostrFilter
    ): String {
        require(subscriptionId.isNotBlank())
        require(subscriptionId.length <= 64)

        return JSONArray()
            .put("REQ")
            .put(subscriptionId)
            .put(filter.toJson())
            .toString()
    }

    fun close(subscriptionId: String): String {
        return JSONArray()
            .put("CLOSE")
            .put(subscriptionId)
            .toString()
    }

    fun publish(event: NostrEvent): String {
        return JSONArray()
            .put("EVENT")
            .put(event.toJson())
            .toString()
    }
}
