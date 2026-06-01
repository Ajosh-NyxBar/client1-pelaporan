package com.laporan.ops.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.laporan.ops.R
import com.laporan.ops.adapter.TowerAdapter
import com.laporan.ops.api.RetrofitClient
import com.laporan.ops.databinding.ActivityKelolaTowerBinding
import com.laporan.ops.model.CreateTowerRequest
import com.laporan.ops.model.Tower
import com.laporan.ops.model.UpdateTowerRequest
import kotlinx.coroutines.launch

class KelolaTowerActivity : AppCompatActivity() {

    private lateinit var b: ActivityKelolaTowerBinding
    private lateinit var adapter: TowerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityKelolaTowerBinding.inflate(layoutInflater)
        setContentView(b.root)

        setSupportActionBar(b.toolbar)
        supportActionBar?.apply { setDisplayHomeAsUpEnabled(true); title = "Kelola Tower" }
        b.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        adapter = TowerAdapter { tower -> showTowerOptionsDialog(tower) }
        b.rvTowers.layoutManager = LinearLayoutManager(this)
        b.rvTowers.adapter = adapter

        b.swipeRefresh.setOnRefreshListener { loadTowers() }
        b.fabTambah.setOnClickListener { showAddTowerDialog() }

        loadTowers()
    }

    private fun loadTowers() {
        b.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                // Admin lihat semua (termasuk nonaktif) → all=1
                val resp = RetrofitClient.instance.getAllTowers(all = "1")
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val towers = resp.body()!!.data ?: emptyList()
                    adapter.submitList(towers)
                    b.tvTowerCount.text = "${towers.size} tower terdaftar"
                    b.layoutEmpty.visibility = if (towers.isEmpty()) View.VISIBLE else View.GONE
                    b.rvTowers.visibility = if (towers.isEmpty()) View.GONE else View.VISIBLE
                } else {
                    snack(resp.body()?.success?.toString() ?: "Gagal memuat tower.")
                }
            } catch (e: Exception) {
                snack("Gagal memuat data: ${e.message}")
            } finally {
                b.progressBar.visibility = View.GONE
                b.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showAddTowerDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_tambah_tower, null)
        val etNama   = view.findViewById<TextInputEditText>(R.id.etNamaTower)
        val etAlamat = view.findViewById<TextInputEditText>(R.id.etAlamatTower)

        MaterialAlertDialogBuilder(this)
            .setTitle("🗼 Tambah Tower Baru")
            .setView(view)
            .setPositiveButton("Simpan") { _, _ ->
                val nama   = etNama.text?.toString()?.trim().orEmpty()
                val alamat = etAlamat.text?.toString()?.trim().orEmpty()
                if (nama.isEmpty()) {
                    snack("Nama tower wajib diisi!")
                    return@setPositiveButton
                }
                doCreateTower(CreateTowerRequest(nama, alamat.ifEmpty { null }))
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun doCreateTower(req: CreateTowerRequest) {
        b.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.instance.createTower(req)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    snack("✅ ${resp.body()!!.message}")
                    loadTowers()
                } else {
                    snack(resp.body()?.message ?: "Gagal menambahkan tower.")
                }
            } catch (e: Exception) {
                snack("Error: ${e.message}")
            } finally {
                b.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showTowerOptionsDialog(tower: Tower) {
        val options = arrayOf(
            "✏️ Edit Nama & Alamat",
            if (tower.isActive == 1) "🚫 Nonaktifkan" else "✅ Aktifkan",
            "🗑️ Hapus"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(tower.nama)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditTowerDialog(tower)
                    1 -> doToggleActive(tower)
                    2 -> confirmDelete(tower)
                }
            }
            .show()
    }

    private fun showEditTowerDialog(tower: Tower) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_tambah_tower, null)
        val etNama   = view.findViewById<TextInputEditText>(R.id.etNamaTower)
        val etAlamat = view.findViewById<TextInputEditText>(R.id.etAlamatTower)
        etNama.setText(tower.nama)
        etAlamat.setText(tower.alamat ?: "")

        MaterialAlertDialogBuilder(this)
            .setTitle("✏️ Edit Tower")
            .setView(view)
            .setPositiveButton("Simpan") { _, _ ->
                val nama   = etNama.text?.toString()?.trim().orEmpty()
                val alamat = etAlamat.text?.toString()?.trim().orEmpty()
                if (nama.isEmpty()) {
                    snack("Nama tower wajib diisi!")
                    return@setPositiveButton
                }
                doUpdateTower(tower.id, UpdateTowerRequest(nama = nama, alamat = alamat))
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun doToggleActive(tower: Tower) {
        val newState = tower.isActive != 1
        doUpdateTower(tower.id, UpdateTowerRequest(isActive = newState))
    }

    private fun confirmDelete(tower: Tower) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus Tower")
            .setMessage("Yakin ingin menonaktifkan tower \"${tower.nama}\"?\n\nLaporan lama yang menggunakan tower ini tetap akan tampil, hanya saja tower ini tidak akan muncul lagi di pilihan teknisi.")
            .setPositiveButton("Ya, Nonaktifkan") { _, _ -> doDeleteTower(tower.id) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun doUpdateTower(id: Int, req: UpdateTowerRequest) {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.instance.updateTower(id, req)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    snack("✅ ${resp.body()!!.message}")
                    loadTowers()
                } else {
                    snack(resp.body()?.message ?: "Gagal memperbarui tower.")
                }
            } catch (e: Exception) { snack("Error: ${e.message}") }
        }
    }

    private fun doDeleteTower(id: Int) {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.instance.deleteTower(id)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    snack("✅ ${resp.body()!!.message}")
                    loadTowers()
                } else {
                    snack(resp.body()?.message ?: "Gagal menghapus tower.")
                }
            } catch (e: Exception) { snack("Error: ${e.message}") }
        }
    }

    private fun snack(msg: String) = Snackbar.make(b.root, msg, Snackbar.LENGTH_LONG).show()
}
