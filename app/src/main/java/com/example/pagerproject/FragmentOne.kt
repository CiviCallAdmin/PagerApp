package com.example.pagerproject

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.firebase.messaging.FirebaseMessaging

class FragmentOne : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var noPostsImage: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_one, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        noPostsImage = view.findViewById(R.id.noPostsImage)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        messageAdapter = MessageAdapter(requireContext(), emptyList())
        recyclerView.adapter = messageAdapter

        // Get the device token and fetch messages
        getDeviceToken { token ->
            if (token.isNotEmpty()) {
                fetchMessages(token)
            } else {
                Toast.makeText(requireContext(), "Failed to retrieve device token", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchMessages(deviceToken: String) {
        progressBar.visibility = View.VISIBLE
        RetrofitClient.instance.fetchMessages(deviceToken).enqueue(object : Callback<List<MessageResponse>> {
            override fun onResponse(call: Call<List<MessageResponse>>, response: Response<List<MessageResponse>>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    response.body()?.let { messages ->
                        if (messages.isNotEmpty()) {
                            messageAdapter.updateMessages(messages)
                            noPostsImage.visibility = View.GONE
                        } else {
                            noPostsImage.visibility = View.VISIBLE
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to fetch messages", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<MessageResponse>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getDeviceToken(callback: (String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                callback(token ?: "")
            } else {
                Log.e("FragmentOne", "Fetching FCM registration token failed", task.exception)
                callback("")
            }
        }
    }
}
