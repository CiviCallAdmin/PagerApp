package com.example.pagerproject

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MessageAdapter(private val context: Context, private var messages: List<MessageResponse>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profilePic: ImageView = view.findViewById(R.id.profilePic)
        val userName: TextView = view.findViewById(R.id.userName)
        val timeRec: TextView = view.findViewById(R.id.timeRec)
        val messageTxt: TextView = view.findViewById(R.id.messageTxt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.message_item, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]

        holder.userName.text = message.user_name
        holder.timeRec.text = message.sent_at
        holder.messageTxt.text = message.location

        // Load profile picture using Glide
        Glide.with(context)
            .load("http://192.168.254.163/V4/Others/Kurt/PagerSql/${message.profile_pic}")
            .placeholder(R.drawable.user) // Placeholder image
            .into(holder.profilePic)
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    fun updateMessages(newMessages: List<MessageResponse>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}
