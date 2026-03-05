package com.example.eventglow.common.cloudinary

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

data class CloudinaryUploadResponse(
    @SerializedName("secure_url") val secureUrl: String? = null,
    @SerializedName("public_id") val publicId: String? = null
)

data class CloudinaryDestroyResponse(
    @SerializedName("result") val result: String? = null
)

interface CloudinaryApi {
    @Multipart
    @POST("v1_1/{cloudName}/image/upload")
    suspend fun uploadImage(
        @Path("cloudName") cloudName: String,
        @Part file: MultipartBody.Part,
        @Part("upload_preset") uploadPreset: RequestBody,
        @Part("folder") folder: RequestBody,
        @Part("asset_folder") assetFolder: RequestBody,
        @Part("public_id") publicId: RequestBody
    ): CloudinaryUploadResponse

    @FormUrlEncoded
    @POST("v1_1/{cloudName}/image/destroy")
    suspend fun destroyImage(
        @Path("cloudName") cloudName: String,
        @Field("public_id") publicId: String,
        @Field("timestamp") timestamp: Long,
        @Field("api_key") apiKey: String,
        @Field("signature") signature: String
    ): CloudinaryDestroyResponse
}
