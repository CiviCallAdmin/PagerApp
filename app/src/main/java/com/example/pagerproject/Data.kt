package com.example.pagerproject

data class Response(
    val status: String,
    val message: String
)
data class MessageResponse(
    val message_id: Int,
    val sender: String,
    val message_text: String,
    val sent_at: String
)
data class DeviceInfo(
    val userName: String,
    val profilePic: String?,
    val deviceToken: String,
    val department: String
)
data class ApiResponse(
    val success: Boolean,
    val message: String,
    val error: String? = null // Optional error message
)

data class UserDataResponse(
    val success: Boolean,
    val user_name: String,
    val department: String,
    val profile_pic: String,
    val message: String? = null
)
data class UserResponse(
    val user_name: String,
    val profile_pic: String,
    val device_id: Int,
    val department: String
)
