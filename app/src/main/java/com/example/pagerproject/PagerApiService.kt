package com.example.pagerproject

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface PagerApiService {
//    @FormUrlEncoded
//    @POST("kurt_api.php?action=sendMessage")
//    fun sendMessage(
//        @Field("sender_id") senderId: Int,
//        @Field("receiver_id") receiverId: Int,
//        @Field("message") message: String
//    ): Call<Response>
//
//    @GET("kurt_api.php?action=getMessages")
//    fun getMessages(
//        @Query("device_id") deviceId: Int
//    ): Call<List<MessageResponse>>

    @FormUrlEncoded
    @POST("kurt_token.php")
    fun registerDevice(
        @Field("user_name") userName: String,
        @Field("profile_pic") profilePic: String?,
        @Field("device_token") deviceToken: String,
        @Field("idNumber") idNumber: String
    ): Call<Void>
    @Multipart
    @POST("kurt_profile.php")
    fun saveProfile(
        @Part("device_token") deviceToken: RequestBody,
        @Part("user_name") userName: RequestBody,
        @Part("idNumber") idNumber: RequestBody,
        @Part profile_pic: MultipartBody.Part? // Image is optional
    ): Call<ApiResponse>

    @GET("kurt_fetchUser.php") // Adjust the URL as needed
    fun fetchUsers(): Call<List<UserResponse>> // Assuming UserResponse matches the JSON structure


    @GET("kurt_profileFetch.php")
    fun getUserData(@Query("device_token") deviceToken: String): Call<UserDataResponse>

    @FormUrlEncoded
    @POST("kurt_sendMessage.php")
    fun sendMessage(
        @Field("device_token") deviceToken: String,
        @Field("receiver_device_id") receiverDeviceId: Int,
        @Field("location") messageText: String
    ): Call<ApiResponse>

    @GET("kurt_fetchMessage.php")
    fun fetchMessages(@Query("device_token") deviceToken: String): Call<List<MessageResponse>>
    @GET("kurt_updateMessageStatus.php")
    fun updateMessageStatus(@Query("message_id") messageId: Int): Call<Void>

    @GET("kurt_pushNotif.php")
    fun pushNotif(@Query("device_token") deviceToken: String): Call<NotificationResponse>
}
