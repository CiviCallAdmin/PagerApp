package com.example.pagerproject

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MessageAdapter(private val context: Context, private var messages: List<MessageResponse>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profilePic: ImageView = view.findViewById(R.id.profilePic)
        val userName: TextView = view.findViewById(R.id.userName)
        val timeRec: TextView = view.findViewById(R.id.timeRec)
        val messageTxt: TextView = view.findViewById(R.id.messageTxt)
        val okButton: Button = view.findViewById(R.id.ok_button) // The OK button
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
            .placeholder(R.drawable.user) // Placeholder image in case the profile pic is not loaded
            .into(holder.profilePic)

        // Handle the OK button click to update message status
        holder.okButton.setOnClickListener {
            // Call API to update message status
            updateMessageStatus(message.message_id, position)
        }
    }

    private fun updateMessageStatus(messageId: Int, position: Int) {
        RetrofitClient.instance.updateMessageStatus(messageId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // Remove the message from the list once status is updated successfully
                    val updatedMessages = messages.toMutableList()
                    updatedMessages.removeAt(position)
                    messages = updatedMessages
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, updatedMessages.size)
                } else {
                    Toast.makeText(context, "Failed to update message", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    fun updateMessages(newMessages: List<MessageResponse>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}
