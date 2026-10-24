package com.example.yourfriend.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.yourfriend.R
import com.example.yourfriend.data.ChatMessage
import com.example.yourfriend.data.FriendGender
import com.example.yourfriend.data.FriendProfile
import com.example.yourfriend.ui.AvatarOrbView
import com.example.yourfriend.ui.TypingDotsView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private var friendProfile: FriendProfile? = null
    private var typing = false

    fun setProfile(profile: FriendProfile) {
        friendProfile = profile
        notifyItemRangeChanged(0, itemCount)
    }

    fun setMessages(items: List<ChatMessage>) {
        messages.clear()
        messages.addAll(items)
        typing = false
        notifyDataSetChanged()
    }

    fun addMessage(message: ChatMessage) {
        val insertIndex = messages.size
        messages += message
        notifyItemInserted(insertIndex)
    }

    fun setTyping(show: Boolean) {
        if (typing == show) return
        typing = show
        if (show) {
            notifyItemInserted(messages.size)
        } else {
            notifyItemRemoved(messages.size)
        }
    }

    fun clear() {
        messages.clear()
        typing = false
        notifyDataSetChanged()
    }

    fun snapshot(): List<ChatMessage> = messages.toList()

    fun lastMessage(): ChatMessage? = messages.lastOrNull()

    fun messageCount(): Int = messages.size

    override fun getItemCount(): Int = messages.size + if (typing) 1 else 0

    override fun getItemViewType(position: Int): Int {
        if (typing && position == messages.size) return TYPE_TYPING
        return if (messages[position].fromUser) TYPE_USER else TYPE_FRIEND
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> UserMessageHolder(inflater.inflate(R.layout.item_message_user, parent, false))
            TYPE_TYPING -> TypingHolder(inflater.inflate(R.layout.item_typing, parent, false))
            else -> FriendMessageHolder(inflater.inflate(R.layout.item_message_friend, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.itemView.alpha = 1f
        holder.itemView.translationY = 0f
        holder.itemView.scaleX = 1f
        holder.itemView.scaleY = 1f

        when (holder) {
            is UserMessageHolder -> {
                val message = messages[position]
                holder.text.text = message.text
                holder.time.text = timeFormat.format(Date(message.timestamp))
                holder.text.attachCopyOnLongPress(message.text)
            }
            is FriendMessageHolder -> {
                val message = messages[position]
                holder.text.text = message.text
                holder.time.text = timeFormat.format(Date(message.timestamp))
                friendProfile?.let { holder.avatar.setProfile(it.name, it.gender) }
                holder.text.attachCopyOnLongPress(message.text)
            }
            is TypingHolder -> {
                friendProfile?.let {
                    holder.avatar.setProfile(it.name, it.gender)
                    holder.dots.setAccent(accentFor(it.gender))
                }
            }
        }
    }

    private fun accentFor(gender: FriendGender): Int {
        return when (gender) {
            FriendGender.FEMALE -> 0xFFFF9ECB.toInt()
            FriendGender.MALE -> 0xFF8FE8FF.toInt()
        }
    }

    private fun TextView.attachCopyOnLongPress(text: String) {
        isLongClickable = true
        setOnLongClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("message", text))
            Toast.makeText(context, "Сообщение скопировано", Toast.LENGTH_SHORT).show()
            true
        }
    }

    class UserMessageHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.userText)
        val time: TextView = view.findViewById(R.id.userTime)
    }

    class FriendMessageHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.friendText)
        val time: TextView = view.findViewById(R.id.friendTime)
        val avatar: AvatarOrbView = view.findViewById(R.id.friendMiniAvatar)
    }

    class TypingHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dots: TypingDotsView = view.findViewById(R.id.typingDots)
        val avatar: AvatarOrbView = view.findViewById(R.id.typingAvatar)
    }

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_FRIEND = 1
        private const val TYPE_TYPING = 2
    }
}
