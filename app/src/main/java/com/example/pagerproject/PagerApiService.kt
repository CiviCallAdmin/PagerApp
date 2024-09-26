package com.example.pagerproject

import retrofit2.Call
import retrofit2.http.*

interface PagerApiService {
    @FormUrlEncoded
    @POST("kurt_api.php?action=sendMessage")
    fun sendMessage(
        @Field("sender_id") senderId: Int,
        @Field("receiver_id") receiverId: Int,
        @Field("message") message: String
    ): Call<Response>

    @GET("kurt_api.php?action=getMessages")
    fun getMessages(
        @Query("device_id") deviceId: Int
    ): Call<List<MessageResponse>>

    @FormUrlEncoded
    @POST("kurt_token.php")
    fun registerDevice(
        @Field("user_name") userName: String,
        @Field("profile_pic") profilePic: String?,
        @Field("device_token") deviceToken: String,
        @Field("department") department: String
    ): Call<Void>


}
