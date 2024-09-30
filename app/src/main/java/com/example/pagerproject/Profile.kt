package com.example.pagerproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pagerproject.databinding.ActivityProfileBinding
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Profile : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.profile) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Load existing user data when the activity is created
        loadUserData()

        // Handle the save button click
        binding.savebtn.setOnClickListener {
            val userName = binding.fullName.text.toString()
            val department = binding.deptCategory.text.toString()

            // Retrieve the current device token
            getDeviceToken { deviceToken ->
                // Validate user input
                if (userName.isNotBlank() && department.isNotBlank() && deviceToken.isNotBlank()) {
                    // Call the PHP script to save/update user data
                    saveUserData(deviceToken, userName, department)
                } else {
                    Toast.makeText(this, "Please fill in all fields and ensure the token is valid", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Back button click listener
        binding.backbtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.animate_fade_enter, R.anim.animate_fade_exit)

        }
    }

    private fun loadUserData() {
        getDeviceToken { deviceToken ->
            // Call the API to fetch existing user data
            val apiService = RetrofitClient.instance
            apiService.getUserData(deviceToken).enqueue(object : Callback<UserDataResponse> {
                override fun onResponse(call: Call<UserDataResponse>, response: Response<UserDataResponse>) {
                    if (response.isSuccessful) {
                        val userData = response.body()
                        if (userData != null && userData.success) {
                            // Populate the fields with existing user data
                            binding.fullName.setText(userData.user_name)
                            binding.userName.setText(userData.user_name)
                            binding.deptCategory.setText(userData.department)
                        } else {
                            Log.e(TAG, userData?.message ?: "No user data found.")
                        }
                    } else {
                        Log.e(TAG, "Failed to load user data. Response code: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<UserDataResponse>, t: Throwable) {
                    Log.e(TAG, "Error: ${t.message}")
                }
            })
        }
    }

    private fun saveUserData(deviceToken: String, userName: String, department: String) {
        val apiService = RetrofitClient.instance

        apiService.saveProfile(deviceToken, userName, department).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null) {
                        Toast.makeText(this@Profile, apiResponse.message, Toast.LENGTH_SHORT).show()
                        Log.d(TAG, apiResponse.message) // Log the message from the response
                    }
                } else {
                    Log.e(TAG, "Failed to save profile. Response code: ${response.code()}")
                    Toast.makeText(this@Profile, "Failed to save profile. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e(TAG, "Error: ${t.message}")
                Toast.makeText(this@Profile, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getDeviceToken(callback: (String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Get the current FCM token
                val token = task.result
                callback(token)
            } else {
                Log.e(TAG, "Fetching FCM registration token failed", task.exception)
                callback("") // Return an empty string in case of failure
            }
        }
    }

    companion object {
        const val TAG = "Profile"
    }
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.animate_fade_enter, R.anim.animate_fade_exit)
    }
}
