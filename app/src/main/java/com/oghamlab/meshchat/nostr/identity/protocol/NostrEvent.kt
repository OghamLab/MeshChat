package com.oghamlab.meshchat.nostr.identity.protocol




import org.json.JSONArray
import org.json.JSONObject

data class NostrEvent(
    val id: String,
    val pubkey: String,
    val createdAt: Long,
    val kind: Int,
    val tags: List<List<String>>,
    val content: String,
    val sig: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("pubkey", pubkey)
        put("created_at", createdAt)
        put("kind", kind)

        val tagsArray = JSONArray()
        tags.forEach { tag ->
            val inner = JSONArray()
            tag.forEach { inner.put(it) }
            tagsArray.put(inner)
        }

        put("tags", tagsArray)
        put("content", content)
        put("sig", sig)
    }

    companion object {
        fun fromJson(json: JSONObject): NostrEvent {
            val rawTags = json.getJSONArray("tags")

            val tags = (0 until rawTags.length()).map { i ->
                val tag = rawTags.getJSONArray(i)

                (0 until tag.length()).map { j ->
                    tag.getString(j)
                }
            }

            return NostrEvent(
                id = json.getString("id"),
                pubkey = json.getString("pubkey"),
                createdAt = json.getLong("created_at"),
                kind = json.getInt("kind"),
                tags = tags,
                content = json.getString("content"),
                sig = json.getString("sig")
            )
        }
    }
}
