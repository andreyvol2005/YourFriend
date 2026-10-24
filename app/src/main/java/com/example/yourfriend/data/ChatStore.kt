package com.example.yourfriend.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ChatStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadProfile(): FriendProfile? {
        if (!prefs.getBoolean(KEY_ONBOARDED, false)) return null
        val name = prefs.getString(KEY_NAME, null)?.trim().orEmpty()
        val gender = prefs.getString(KEY_GENDER, null)
        if (name.isEmpty() || gender == null) return null

        val parsedGender = runCatching { FriendGender.valueOf(gender) }.getOrNull() ?: return null
        return FriendProfile(name = name, gender = parsedGender)
    }

    fun saveProfile(profile: FriendProfile) {
        prefs.edit()
            .putBoolean(KEY_ONBOARDED, true)
            .putString(KEY_NAME, profile.name)
            .putString(KEY_GENDER, profile.gender.name)
            .apply()
    }

    fun loadMessages(): MutableList<ChatMessage> {
        val raw = prefs.getString(KEY_MESSAGES, null) ?: return mutableListOf()
        return runCatching {
            val array = JSONArray(raw)
            val result = mutableListOf<ChatMessage>()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val text = item.optString("text")
                if (text.isBlank()) continue
                result += ChatMessage(
                    id = item.optLong("id", index.toLong()),
                    text = text,
                    fromUser = item.optBoolean("fromUser"),
                    timestamp = item.optLong("timestamp", System.currentTimeMillis())
                )
            }
            result
        }.getOrElse { mutableListOf() }
    }

    fun saveMessages(messages: List<ChatMessage>) {
        val trimmed = messages.takeLast(MAX_MESSAGES)
        val array = JSONArray()
        trimmed.forEach { message ->
            array.put(
                JSONObject()
                    .put("id", message.id)
                    .put("text", message.text)
                    .put("fromUser", message.fromUser)
                    .put("timestamp", message.timestamp)
            )
        }
        prefs.edit().putString(KEY_MESSAGES, array.toString()).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "you_friend_store"
        private const val KEY_ONBOARDED = "onboarded"
        private const val KEY_NAME = "friend_name"
        private const val KEY_GENDER = "friend_gender"
        private const val KEY_MESSAGES = "messages"
        private const val MAX_MESSAGES = 250
    }
}
