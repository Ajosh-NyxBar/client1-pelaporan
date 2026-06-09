package com.laporan.ops.ui.admin

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.laporan.ops.adapter.PhotoAdapter
import com.laporan.ops.api.RetrofitClient
import com.laporan.ops.databinding.ActivityDetailLaporanBinding
import com.laporan.ops.model.FollowUpRequest
import com.laporan.ops.model.ValidateRequest
import com.laporan.ops.utils.SessionManager
import kotlinx.coroutines.launch

class DetailLaporanActivity : AppCompatActivity() {

    private lateinit var b: ActivityDetailLaporanBinding
    private lateinit var session: SessionManager
    private var reportId = -1

    // Base URL untuk foto (tanpa /api/) — diambil dari RetrofitClient
    private val photoBaseUrl: String
        get() = com.laporan.ops.api.RetrofitClient.PHOTO_BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityDetailLaporanBinding.inflate(layoutInflater)
        setContentView(b.root)
        session   = SessionManager.getInstance(this)
        reportId  = intent.getIntExtra("REPORT_ID", -1)

        setSupportActionBar(b.toolbar)
        supportActionBar?.apply { setDisplayHomeAsUpEnabled(true); title = "Detail Laporan" }
        b.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        if (reportId != -1) loadDetail()
    }

    private fun loadDetail() {
        b.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.instance.getReportById(reportId)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val r = resp.body()!!.data!!

                    b.tvCode.text       = r.reportCode
                    b.tvJenis.text      = r.jenisPekerjaan
                    b.tvLokasi.text     = buildString {
                        append(r.towerNama ?: r.lokasi)
                        if (!r.towerAlamat.isNullOrBlank()) {
                            append("\n")
                            append(r.towerAlamat)
                        }
                    }
                    b.tvWaktu.text      = r.waktuKerja
                    b.tvTeknisi.text    = "${r.teknisiName} (${r.teknisiUsername})"
                    b.tvDeskripsi.text  = r.deskripsi
                    b.tvTanggal.text    = r.createdAt
                    b.tvFotoCount.text  = "${r.photos?.size ?: 0} foto terlampir"

                    // ── Tampilkan foto-foto ────────────────────────
                    val photos = r.photos ?: emptyList()
                    if (photos.isNotEmpty()) {
                        b.cardFoto.visibility = View.VISIBLE
                        b.rvPhotos.layoutManager = LinearLayoutManager(
                            this@DetailLaporanActivity,
                            LinearLayoutManager.HORIZONTAL,
                            false
                        )
                        b.rvPhotos.adapter = PhotoAdapter(photos, photoBaseUrl) { fullUrl ->
                            showFullScreenPhoto(fullUrl)
                        }
                    }

                    val (statusText, statusColor) = when (r.status) {
                        "disetujui" -> "Disetujui oleh ${r.validatorName}" to getColor(android.R.color.holo_green_dark)
                        "ditolak"   -> "Ditolak oleh ${r.validatorName}"   to getColor(android.R.color.holo_red_dark)
                        else        -> "Menunggu Validasi"                  to getColor(android.R.color.holo_orange_dark)
                    }
                    b.tvStatus.text     = statusText
                    b.tvStatus.setTextColor(statusColor)

                    if (!r.catatanValidasi.isNullOrBlank()) {
                        b.cardCatatan.visibility    = View.VISIBLE
                        b.tvCatatan.text = r.catatanValidasi
                    }

                    // ── Tindak Lanjut: tampilkan jika sudah ada ────
                    if (!r.tindakLanjut.isNullOrBlank()) {
                        b.cardTindakLanjut.visibility = View.VISIBLE
                        b.tvTindakLanjut.text = r.tindakLanjut
                        val info = buildString {
                            append("Oleh: ${r.helpdeskName ?: "Helpdesk"}")
                            if (!r.tindakLanjutAt.isNullOrBlank()) append(" • ${r.tindakLanjutAt}")
                        }
                        b.tvTindakLanjutInfo.text = info
                    }

                    // ── Tindak Lanjut: input form (helpdesk, laporan disetujui, belum ada tindak lanjut) ────
                    val isHelpdesk  = session.getRole() == "helpdesk"
                    val isApproved  = r.status == "disetujui"
                    val noFollowUp  = r.tindakLanjut.isNullOrBlank()
                    if (isHelpdesk && isApproved && noFollowUp) {
                        b.cardInputTindakLanjut.visibility = View.VISIBLE
                        b.btnSimpanTindakLanjut.setOnClickListener {
                            val text = b.etTindakLanjut.text?.toString()?.trim().orEmpty()
                            if (text.isEmpty()) {
                                b.tilTindakLanjut.error = "Catatan wajib diisi"
                            } else {
                                b.tilTindakLanjut.error = null
                                doFollowUp(text)
                            }
                        }
                    }

                    // ── Validasi: admin buttons ────
                    val isAdmin   = session.getRole() == "admin"
                    val isPending = r.status == "menunggu"
                    b.cardValidasi.visibility = if (isAdmin && isPending) View.VISIBLE else View.GONE

                    if (isAdmin && isPending) {
                        b.btnApprove.setOnClickListener {
                            MaterialAlertDialogBuilder(this@DetailLaporanActivity)
                                .setTitle("Setujui Laporan")
                                .setMessage("Anda akan menyetujui laporan ini. Lanjutkan?")
                                .setPositiveButton("Ya, Setujui") { _, _ -> doValidate("approve", null) }
                                .setNegativeButton("Batal", null).show()
                        }
                        b.btnReject.setOnClickListener {
                            val input = TextInputEditText(this@DetailLaporanActivity).apply {
                                hint = "Tulis alasan penolakan..."
                                setPadding(48, 24, 48, 8)
                            }
                            MaterialAlertDialogBuilder(this@DetailLaporanActivity)
                                .setTitle("Tolak Laporan")
                                .setMessage("Berikan catatan penolakan:")
                                .setView(input)
                                .setPositiveButton("Tolak") { _, _ ->
                                    val catatan = input.text?.toString()?.trim().orEmpty()
                                    if (catatan.isEmpty()) snack("Catatan wajib diisi!")
                                    else doValidate("reject", catatan)
                                }
                                .setNegativeButton("Batal", null).show()
                        }
                    }
                }
            } catch (e: Exception) {
                snack("Gagal memuat detail laporan: ${e.message}")
            } finally {
                b.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showFullScreenPhoto(url: String) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val imageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(0xFF000000.toInt())
            setOnClickListener { dialog.dismiss() }
        }
        Glide.with(this).load(url).into(imageView)
        dialog.setContentView(imageView)
        dialog.show()
    }

    private fun doValidate(action: String, catatan: String?) {
        b.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.instance.validateReport(reportId, ValidateRequest(action, catatan))
                if (resp.isSuccessful && resp.body()?.success == true) {
                    snack(resp.body()!!.message)
                    loadDetail()
                } else {
                    snack(resp.body()?.message ?: "Validasi gagal.")
                }
            } catch (e: Exception) {
                snack("Error: ${e.message}")
            } finally {
                b.progressBar.visibility = View.GONE
            }
        }
    }

    private fun doFollowUp(text: String) {
        b.progressBar.visibility = View.VISIBLE
        b.btnSimpanTindakLanjut.isEnabled = false
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.instance.addFollowUp(reportId, FollowUpRequest(text))
                if (resp.isSuccessful && resp.body()?.success == true) {
                    snack(resp.body()!!.message)
                    loadDetail() // Reload to show the follow-up
                } else {
                    snack(resp.body()?.message ?: "Gagal menyimpan tindak lanjut.")
                }
            } catch (e: Exception) {
                snack("Error: ${e.message}")
            } finally {
                b.progressBar.visibility = View.GONE
                b.btnSimpanTindakLanjut.isEnabled = true
            }
        }
    }

    private fun snack(msg: String) = Snackbar.make(b.root, msg, Snackbar.LENGTH_LONG).show()
}
