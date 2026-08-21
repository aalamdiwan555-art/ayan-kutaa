package com.autopilot.driver

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class LoginRequest(val email: String, val password: String)
data class SignupRequest(
    val name: String,
    val email: String,
    val phone: String,
    val password: String
)
data class EmailRequest(val email: String)
data class LoginResponse(
    val token: String?,
    val email: String?,
    val isAdmin: Boolean = false
)
data class AdminVerifyResponse(val isAdmin: Boolean = false)

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<LoginResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: EmailRequest): Response<Unit>

    @GET("admin/verify")
    suspend fun verifyAdmin(@Header("Authorization") authorization: String): Response<AdminVerifyResponse>
}

object ApiClient {
    fun authApi(): AuthApi? {
        val baseUrl = BuildConfig.API_BASE_URL.trim().let {
            if (it.endsWith("/")) it else "$it/"
        }
        if (baseUrl == "/") return null
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }
}