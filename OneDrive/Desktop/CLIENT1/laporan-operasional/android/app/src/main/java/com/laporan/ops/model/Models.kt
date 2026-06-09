package com.laporan.ops.model

import com.google.gson.annotations.SerializedName

// ── Auth ─────────────────────────────────────────────────────

data class LoginRequest(val username: String, val password: String)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val data: LoginData?
)

data class LoginData(val token: String, val user: User)

data class User(
    val id: Int,
    val username: String,
    val name: String,
    val role: String,
    val jabatan: String? = null,
    @SerializedName("no_hp") val noHp: String? = null,
    val alamat: String? = null,
    val email: String? = null
)

data class ChangePasswordRequest(
    @SerializedName("old_password") val oldPassword: String,
    @SerializedName("new_password") val newPassword: String
)

data class ProfileResponse(
    val success: Boolean,
    val data: User?
)

// ── Generic Response ─────────────────────────────────────────

data class GenericResponse(val success: Boolean, val message: String)

// ── Report ───────────────────────────────────────────────────

data class Report(
    val id: Int,
    @SerializedName("report_code")      val reportCode: String,
    @SerializedName("teknisi_id")       val teknisiId: Int,
    @SerializedName("teknisi_name")     val teknisiName: String?,
    @SerializedName("teknisi_username") val teknisiUsername: String?,
    @SerializedName("jenis_pekerjaan")  val jenisPekerjaan: String,
    val lokasi: String,
    @SerializedName("tower_id")         val towerId: Int?,
    @SerializedName("tower_nama")       val towerNama: String?,
    @SerializedName("tower_alamat")     val towerAlamat: String?,
    @SerializedName("waktu_kerja")      val waktuKerja: String,
    val deskripsi: String,
    val status: String,
    @SerializedName("validated_by")     val validatedBy: Int?,
    @SerializedName("validator_name")   val validatorName: String?,
    @SerializedName("validated_at")     val validatedAt: String?,
    @SerializedName("catatan_validasi") val catatanValidasi: String?,
    @SerializedName("tindak_lanjut")    val tindakLanjut: String?,
    @SerializedName("tindak_lanjut_by") val tindakLanjutBy: Int?,
    @SerializedName("tindak_lanjut_at") val tindakLanjutAt: String?,
    @SerializedName("helpdesk_name")    val helpdeskName: String?,
    @SerializedName("created_at")       val createdAt: String,
    @SerializedName("photo_count")      val photoCount: Int = 0,
    val photos: List<ReportPhoto>? = null
)

data class ReportPhoto(
    val id: Int,
    @SerializedName("report_id")  val reportId: Int,
    @SerializedName("photo_path") val photoPath: String
)

data class ReportsResponse(val success: Boolean, val message: String?, val data: ReportsData?)
data class ReportsData(val reports: List<Report>, val pagination: Pagination)
data class Pagination(val total: Int, val page: Int, val limit: Int, val totalPages: Int)

data class SingleReportResponse(val success: Boolean, val message: String?, val data: Report?)

data class DashboardStats(val total: Int, val menunggu: Int, val disetujui: Int, val ditolak: Int)
data class StatsResponse(val success: Boolean, val data: DashboardStats?)

data class CreateReportResponse(val success: Boolean, val message: String, val data: CreateReportData?)
data class CreateReportData(
    val id: Int,
    @SerializedName("report_code") val reportCode: String,
    val status: String
)

data class ValidateRequest(val action: String, val catatan: String?)
data class ValidateResponse(val success: Boolean, val message: String)

// ── Follow Up (Helpdesk — UC-17) ─────────────────────────────
data class FollowUpRequest(
    @SerializedName("tindak_lanjut") val tindakLanjut: String
)
data class FollowUpResponse(val success: Boolean, val message: String)

// ── Users (Admin management) ─────────────────────────────────

data class UserDetail(
    val id: Int,
    val username: String,
    val name: String,
    val role: String,
    val jabatan: String? = null,
    @SerializedName("no_hp") val noHp: String? = null,
    val alamat: String? = null,
    val email: String? = null,
    @SerializedName("is_active")  val isActive: Int,
    @SerializedName("created_at") val createdAt: String?
)

data class UsersResponse(val success: Boolean, val data: List<UserDetail>?)

data class CreateUserRequest(
    val username: String,
    val password: String,
    val name: String,
    val role: String = "teknisi",
    val jabatan: String? = null,
    @SerializedName("no_hp") val noHp: String? = null,
    val alamat: String? = null,
    val email: String? = null
)

data class CreateUserResponse(
    val success: Boolean,
    val message: String,
    val data: UserDetail?
)

data class UpdateUserRequest(
    val name: String? = null,
    val role: String? = null,
    @SerializedName("is_active") val isActive: Boolean? = null,
    val password: String? = null,
    val jabatan: String? = null,
    @SerializedName("no_hp") val noHp: String? = null,
    val alamat: String? = null,
    val email: String? = null
)

data class UpdateUserResponse(
    val success: Boolean,
    val message: String,
    val data: UserDetail?
)

// ── Towers (Kategori Lokasi) ─────────────────────────────────

data class Tower(
    val id: Int,
    val nama: String,
    val alamat: String?,
    @SerializedName("is_active")  val isActive: Int = 1,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
) {
    override fun toString(): String = nama  // dipakai langsung di Spinner
}

data class TowersResponse(val success: Boolean, val data: List<Tower>?)

data class CreateTowerRequest(
    val nama: String,
    val alamat: String? = null
)

data class UpdateTowerRequest(
    val nama: String? = null,
    val alamat: String? = null,
    @SerializedName("is_active") val isActive: Boolean? = null
)

data class TowerResponse(
    val success: Boolean,
    val message: String,
    val data: Tower?
)
