package com.example.pagerproject

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FragmentTwo : Fragment() {

    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var messageField: TextInputEditText
    private lateinit var userAdapter: UserAdapter
    private var userList: List<UserResponse> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_two, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        autoCompleteTextView = view.findViewById(R.id.idFrom)
        messageField = view.findViewById(R.id.messageField)
        val sendButton: Button = view.findViewById(R.id.send_button) // Initialize the send button
        val clearButton: Button = view.findViewById(R.id.clear_button)

        // Fetch users from the API
        fetchUsers()

        // Set listener for AutoCompleteTextView item selection
        autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            val selectedUser = userAdapter.getItem(position)
            autoCompleteTextView.setText("${selectedUser?.device_id} - ${selectedUser?.user_name}", false)
        }

        // Set listener for clear button
        clearButton.setOnClickListener {
            clearFields()
        }

        // Set listener for send button
        sendButton.setOnClickListener {
            sendMessage()
        }

        // Set a text change listener to filter the list as the user types
        autoCompleteTextView.addTextChangedListener { text ->
            userAdapter.filter.filter(text)
        }
    }

    private fun fetchUsers() {
        RetrofitClient.instance.fetchUsers().enqueue(object : Callback<List<UserResponse>> {
            override fun onResponse(call: Call<List<UserResponse>>, response: Response<List<UserResponse>>) {
                if (response.isSuccessful) {
                    response.body()?.let { users ->
                        userList = users
                        userAdapter = UserAdapter(requireContext(), users)
                        autoCompleteTextView.setAdapter(userAdapter)
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to fetch users", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<UserResponse>>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendMessage() {
        val selectedUserText = autoCompleteTextView.text.toString()
        if (selectedUserText.isEmpty() || messageField.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please select a user and enter a message.", Toast.LENGTH_SHORT).show()
            return
        }

        // Extract device_id from the selected user text
        val selectedDeviceId = selectedUserText.split(" - ")[0].toInt()
        val messageText = messageField.text.toString()

        // Show confirmation dialog
        showConfirmationDialog(selectedDeviceId, messageText)
    }

    private fun showConfirmationDialog(selectedDeviceId: Int, messageText: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Send Message")
        builder.setMessage("Are you sure you want to send this message?")
        builder.setPositiveButton("Yes") { _, _ ->
            // Get FCM token
            getDeviceToken { deviceToken ->
                if (deviceToken.isNotEmpty()) {
                    // Send the message using the retrieved token
                    RetrofitClient.instance.sendMessage(deviceToken, selectedDeviceId, messageText).enqueue(object : Callback<ApiResponse> {
                        override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                            if (response.isSuccessful && response.body() != null) {
                                Toast.makeText(requireContext(), response.body()!!.message, Toast.LENGTH_SHORT).show()
                                clearFields() // Clear fields after sending
                            } else {
                                Toast.makeText(requireContext(), "Failed to send message", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                            Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                } else {
                    Toast.makeText(requireContext(), "Failed to retrieve device token", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }


    private fun getDeviceToken(callback: (String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Get the current FCM token
                val token = task.result
                callback(token ?: "") // Pass the token or an empty string
            } else {
                Log.e("FragmentTwo", "Fetching FCM registration token failed", task.exception)
                callback("") // Return an empty string in case of failure
            }
        }
    }

    private fun clearFields() {
        autoCompleteTextView.text.clear()
        messageField.text?.clear()
    }
}
