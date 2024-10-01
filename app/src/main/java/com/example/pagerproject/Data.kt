package com.example.pagerproject

data class Response(
    val status: String,
    val message: String
)

data class DeviceInfo(
    val userName: String,
    val profilePic: String?,
    val deviceToken: String,
    val idNumber: String
)
data class ApiResponse(
    val success: Boolean,
    val message: String,
    val error: String? = null // Optional error message
)

data class UserDataResponse(
    val success: Boolean,
    val user_name: String,
    val idNumber: String,
    val profile_pic: String,
    val message: String? = null
)
data class UserResponse(
    val device_id: Int,
    val user_name: String,
    val profile_pic: String,
    val device_token: String,
    val idNumber: String
)
data class Message(
    val message_id: Int,
    val sender_device_id: Int,
    val receiver_device_id: Int,
    val location: String,
    val sent_at: String,
    val user_name: String,
    val profile_pic: String,
    var status: Boolean // true for read, false for unread
)
data class NotificationResponse(
    val title: String,
    val body: String,
    val messages: List<Message> // Assuming the response contains a list of messages
)
data class MessageResponse(
    val message_id: Int,
    val sender_device_id: Int,
    val receiver_device_id: Int,
    val location: String,
    val sent_at: String,
    val user_name: String,
    val profile_pic: String
)