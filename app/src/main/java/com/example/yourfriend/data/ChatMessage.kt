package com.example.yourfriend.data

data class ChatMessage(
    val id: Long,
    val text: String,
    val fromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
