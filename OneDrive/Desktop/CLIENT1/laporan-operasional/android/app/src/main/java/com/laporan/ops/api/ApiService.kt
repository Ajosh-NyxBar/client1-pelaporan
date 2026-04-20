package com.laporan.ops.api

import com.laporan.ops.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ─────────────────────────────────────────────────
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("auth/profile")
    suspend fun getProfile(): Response<ProfileResponse>

    @PUT("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<GenericResponse>

    // ── Reports ──────────────────────────────────────────────
    @GET("reports/stats")
    suspend fun getDashboardStats(): Response<StatsResponse>

    @GET("reports")
    suspend fun getAllReports(
        @Query("status") status: String? = null,
        @Query("page")   page:   Int    = 1,
        @Query("limit")  limit:  Int    = 50
    ): Response<ReportsResponse>

    @GET("reports/{id}")
    suspend fun getReportById(@Path("id") id: Int): Response<SingleReportResponse>

    @Multipart
    @POST("reports")
    suspend fun createReport(
        @Part("jenis_pekerjaan") jenisPekerjaan: RequestBody,
        @Part("lokasi")          lokasi:          RequestBody,
        @Part("waktu_kerja")     waktuKerja:      RequestBody,
        @Part("deskripsi")       deskripsi:       RequestBody,
        @Part photos: List<MultipartBody.Part>?
    ): Response<CreateReportResponse>

    @PATCH("reports/{id}/validate")
    suspend fun validateReport(
        @Path("id") id: Int,
        @Body request: ValidateRequest
    ): Response<ValidateResponse>

    @PATCH("reports/{id}/follow-up")
    suspend fun addFollowUp(
        @Path("id") id: Int,
        @Body request: FollowUpRequest
    ): Response<FollowUpResponse>

    // ── Users (Admin only) ───────────────────────────────────
    @GET("users")
    suspend fun getAllUsers(): Response<UsersResponse>

    @POST("users")
    suspend fun createUser(@Body request: CreateUserRequest): Response<CreateUserResponse>

    @PUT("users/{id}")
    suspend fun updateUser(
        @Path("id") id: Int,
        @Body request: UpdateUserRequest
    ): Response<UpdateUserResponse>

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: Int): Response<GenericResponse>
}
