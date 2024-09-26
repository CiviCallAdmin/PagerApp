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


