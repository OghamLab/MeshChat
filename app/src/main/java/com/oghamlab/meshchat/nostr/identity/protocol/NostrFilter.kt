package com.oghamlab.meshchat.nostr.identity.protocol



import org.json.JSONArray
import org.json.JSONObject

data class NostrFilter(
    val kinds: List<Int>? = null,
    val authors: List<String>? = null,
    val since: Long? = null,
    val limit: Int? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        kinds?.let {
            put("kinds", JSONArray(it))
        }

        authors?.let {
            put("authors", JSONArray(it))
        }

        since?.let {
            put("since", it)
        }

        limit?.let {
            put("limit", it)
        }
    }
}
