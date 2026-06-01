package com.laporan.ops.ui.teknisi

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.laporan.ops.api.RetrofitClient
import com.laporan.ops.databinding.ActivityTambahLaporanBinding
import com.laporan.ops.model.Tower
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class TambahLaporanActivity : AppCompatActivity() {

    private lateinit var b: ActivityTambahLaporanBinding
    private val selectedPhotos = mutableListOf<Uri>()
    private val selectedTime   = Calendar.getInstance()

    private val towers       = mutableListOf<Tower>()
    private var selectedTower: Tower? = null

    private val pickImages = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                data.clipData?.let { clip ->
                    for (i in 0 until minOf(clip.itemCount, 5)) selectedPhotos.add(clip.getItemAt(i).uri)
                } ?: data.data?.let { selectedPhotos.add(it) }
                updatePhotoUI()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTambahLaporanBinding.inflate(layoutInflater)
        setContentView(b.root)

        setSupportActionBar(b.toolbar)
        supportActionBar?.apply { setDisplayHomeAsUpEnabled(true); title = "Tambah Laporan" }
        b.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        updateDateTimeDisplay()

        b.btnPilihWaktu.setOnClickListener { pickDateTime() }
        b.btnPilihFoto.setOnClickListener  { openGallery() }
        b.btnClearFoto.setOnClickListener  { selectedPhotos.clear(); updatePhotoUI() }
        b.btnKirim.setOnClickListener      { submit() }

        // Klik dropdown lokasi tower
        b.actLokasi.setOnClickListener {
            if (towers.isEmpty()) loadTowers(showAfter = true) else b.actLokasi.showDropDown()
        }
        b.actLokasi.setOnItemClickListener { _, _, position, _ ->
            selectedTower = towers[position]
            b.tilLokasi.error = null
        }

        loadTowers()
    }

    private fun loadTowers(showAfter: Boolean = false) {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.instance.getAllTowers()
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val list = resp.body()!!.data ?: emptyList()
                    towers.clear()
                    towers.addAll(list)

                    val names = towers.map { it.nama }
                    val adapter = ArrayAdapter(
                        this@TambahLaporanActivity,
                        android.R.layout.simple_list_item_1,
                        names
                    )
                    b.actLokasi.setAdapter(adapter)

                    if (towers.isEmpty()) {
                        b.tilLokasi.helperText = "Belum ada tower. Hubungi admin untuk menambah tower."
                    } else {
                        b.tilLokasi.helperText = null
                        if (showAfter) b.actLokasi.showDropDown()
                    }
                } else {
                    snack(resp.body()?.success?.let { "Gagal memuat daftar tower." } ?: "Gagal memuat tower.")
                }
            } catch (e: Exception) {
                snack("Gagal memuat tower: ${e.message}")
            }
        }
    }

    private fun pickDateTime() {
        DatePickerDialog(this, { _, y, m, d ->
            selectedTime.set(y, m, d)
            TimePickerDialog(this, { _, h, min ->
                selectedTime.set(Calendar.HOUR_OF_DAY, h)
                selectedTime.set(Calendar.MINUTE, min)
                updateDateTimeDisplay()
            }, selectedTime.get(Calendar.HOUR_OF_DAY), selectedTime.get(Calendar.MINUTE), true).show()
        }, selectedTime.get(Calendar.YEAR), selectedTime.get(Calendar.MONTH), selectedTime.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateDateTimeDisplay() {
        b.tvWaktu.text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id")).format(selectedTime.time)
    }

    private fun openGallery() {
        pickImages.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            type = "image/*"
        })
    }

    private fun updatePhotoUI() {
        b.tvPhotoCount.text  = "${selectedPhotos.size}/5 foto dipilih"
        b.btnClearFoto.visibility = if (selectedPhotos.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun submit() {
        val jenis  = b.etJenis.text?.toString()?.trim().orEmpty()
        val desc   = b.etDeskripsi.text?.toString()?.trim().orEmpty()
        val tower  = selectedTower

        var ok = true
        if (jenis.isEmpty())  { b.tilJenis.error  = "Wajib diisi"; ok = false } else b.tilJenis.error  = null
        if (tower == null)    { b.tilLokasi.error = "Pilih lokasi tower"; ok = false } else b.tilLokasi.error = null
        if (desc.isEmpty())   { b.tilDeskripsi.error = "Wajib diisi"; ok = false } else b.tilDeskripsi.error = null
        if (!ok) return

        setLoading(true)
        lifecycleScope.launch {
            try {
                val fmt   = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val waktu = fmt.format(selectedTime.time)

                val parts = selectedPhotos.mapNotNull { uri ->
                    getFile(uri)?.let { file ->
                        // Detect actual MIME type from content resolver
                        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                        MultipartBody.Part.createFormData("photos", file.name,
                            file.asRequestBody(mimeType.toMediaType()))
                    }
                }

                val resp = RetrofitClient.instance.createReport(
                    jenis.toRequestBody("text/plain".toMediaType()),
                    tower!!.id.toString().toRequestBody("text/plain".toMediaType()),
                    waktu.toRequestBody("text/plain".toMediaType()),
                    desc.toRequestBody("text/plain".toMediaType()),
                    parts.ifEmpty { null }
                )

                if (resp.isSuccessful && resp.body()?.success == true) {
                    val code = resp.body()!!.data?.reportCode ?: ""
                    snack("✅ Laporan $code berhasil dikirim!")
                    finish()
                } else {
                    snack(resp.body()?.message ?: "Gagal mengirim laporan.")
                }
            } catch (e: Exception) {
                snack("Error: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun getFile(uri: Uri): File? {
        return try {
            val input = contentResolver.openInputStream(uri) ?: return null
            // Detect extension from MIME type
            val mimeType = contentResolver.getType(uri)
            val ext = when (mimeType) {
                "image/png"  -> ".png"
                "image/gif"  -> ".gif"
                "image/webp" -> ".webp"
                else         -> ".jpg"
            }
            val tmp = File.createTempFile("upload_", ext, cacheDir)
            tmp.outputStream().use { input.copyTo(it) }
            tmp
        } catch (_: Exception) {
            null
        }
    }

    private fun setLoading(on: Boolean) {
        b.progressBar.visibility = if (on) View.VISIBLE else View.GONE
        b.btnKirim.isEnabled = !on
        b.btnKirim.text = if (on) "Mengirim..." else "KIRIM LAPORAN"
    }

    private fun snack(msg: String) = Snackbar.make(b.root, msg, Snackbar.LENGTH_LONG).show()
}
