package com.example.pagerproject

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FragmentTwo : Fragment() {

    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var location: AutoCompleteTextView
    private var userList: List<UserResponse> = emptyList()
    private var selectedUsers: MutableList<UserResponse> = mutableListOf() // Store selected users

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_two, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        autoCompleteTextView = view.findViewById(R.id.idFrom)
        location = view.findViewById(R.id.location)
        val sendButton: Button = view.findViewById(R.id.send_button)
        val clearButton: Button = view.findViewById(R.id.clear_button)

        val locations = listOf("Manila", "Cebu", "Palawan", "Batangas", "Pampanga")
        val locationAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, locations)
        location.setAdapter(locationAdapter)

        // Fetch users from the API
        fetchUsers()

        // Set listener for AutoCompleteTextView item selection
        autoCompleteTextView.setOnClickListener {
            showUserSelectionDialog()
        }

        // Set listener for clear button
        clearButton.setOnClickListener {
            clearFields()
        }

        // Set listener for send button
        sendButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun fetchUsers() {
        RetrofitClient.instance.fetchUsers().enqueue(object : Callback<List<UserResponse>> {
            override fun onResponse(call: Call<List<UserResponse>>, response: Response<List<UserResponse>>) {
                if (response.isSuccessful) {
                    response.body()?.let { users ->
                        userList = users
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

    private fun showUserSelectionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.popup_user_selection, null)
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val linearLayout = dialogView.findViewById<LinearLayout>(R.id.linear)
        val closeIcon = dialogView.findViewById<ImageView>(R.id.closeIcon)
        val btnSelect = dialogView.findViewById<Button>(R.id.btnSelectCampus)

        userList.forEach { user ->
            val checkboxLayout = layoutInflater.inflate(R.layout.dropdown_item, null)
            val profilePic: ImageView = checkboxLayout.findViewById(R.id.profilePic)
            val nameRec: TextView = checkboxLayout.findViewById(R.id.nameRec)
            val idNum: TextView = checkboxLayout.findViewById(R.id.idNum)
            val checkBox: CheckBox = checkboxLayout.findViewById(R.id.checkBox12)
            val mainLinear: LinearLayout = checkboxLayout.findViewById(R.id.mainLinear)

            nameRec.text = user.user_name
            idNum.text = user.idNumber

            // Load profile picture using Glide
            Glide.with(requireContext())
                .load("http://192.168.254.163/V4/Others/Kurt/PagerSql/${user.profile_pic}")
                .placeholder(R.drawable.user)
                .into(profilePic)

            // Set the initial checkbox state based on selectedUsers
            checkBox.isChecked = selectedUsers.contains(user)

            // Add checkbox and listener
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    if (!selectedUsers.contains(user)) { // Avoid duplicates
                        selectedUsers.add(user)
                    }
                } else {
                    selectedUsers.remove(user)
                }
            }

            // Add click listener to the mainLinear layout to toggle the checkbox
            mainLinear.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked // Toggle checkbox state
                if (checkBox.isChecked) {
                    if (!selectedUsers.contains(user)) { // Avoid duplicates
                        selectedUsers.add(user)
                    }
                } else {
                    selectedUsers.remove(user)
                }
            }

            linearLayout.addView(checkboxLayout)
        }

        closeIcon.setOnClickListener { alertDialog.dismiss() }
        btnSelect.setOnClickListener {
            updateAutoCompleteTextView()
            alertDialog.dismiss()
        }

        alertDialog.show()
    }


    private fun updateAutoCompleteTextView() {
        val selectedTexts = selectedUsers.joinToString(", ") { "${it.device_id} - ${it.user_name}" }
        autoCompleteTextView.setText(selectedTexts, false)
    }

    private fun sendMessage() {
        if (selectedUsers.isEmpty() || location.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please select users and enter a location.", Toast.LENGTH_SHORT).show()
            return
        }

        val messageText = location.text.toString()
        showConfirmationDialog(messageText)
    }

    private fun showConfirmationDialog(messageText: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Send Message")
        builder.setMessage("Are you sure you want to send this message to selected users?")
        builder.setPositiveButton("Yes") { _, _ ->
            selectedUsers.forEach { user ->
                getDeviceToken { deviceToken ->
                    if (deviceToken.isNotEmpty()) {
                        RetrofitClient.instance.sendMessage(deviceToken, user.device_id.toInt(), messageText).enqueue(object : Callback<ApiResponse> {
                            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                                if (response.isSuccessful && response.body() != null) {
                                    Toast.makeText(requireContext(), response.body()!!.message, Toast.LENGTH_SHORT).show()
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
            clearFields() // Clear fields after sending
        }
        builder.setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun getDeviceToken(callback: (String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
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
        location.text?.clear()
        selectedUsers.clear() // Clear selected users after sending
    }
}

