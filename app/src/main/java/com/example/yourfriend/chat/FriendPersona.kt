package com.example.yourfriend.chat

import com.example.yourfriend.data.FriendGender
import com.example.yourfriend.data.FriendProfile

object FriendPersona {

    fun greeting(profile: FriendProfile): String {
        val glad = if (profile.gender == FriendGender.FEMALE) "рада" else "рад"
        return "Привет. Я ${profile.name}. Я $glad, что ты здесь. С чего начнём?"
    }
}
