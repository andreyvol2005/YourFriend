package com.example.yourfriend.data

enum class FriendGender {
    FEMALE,
    MALE
}

data class FriendProfile(
    val name: String,
    val gender: FriendGender
) {
    val initial: String
        get() = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
}
