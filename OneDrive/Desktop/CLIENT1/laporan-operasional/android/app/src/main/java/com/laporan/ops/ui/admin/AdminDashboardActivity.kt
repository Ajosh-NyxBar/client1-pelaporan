package com.laporan.ops.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.laporan.ops.adapter.LaporanAdapter
import com.laporan.ops.api.RetrofitClient
import com.laporan.ops.databinding.ActivityAdminDashboardBinding
import com.laporan.ops.ui.LoginActivity
import com.laporan.ops.ui.ProfileActivity
import com.laporan.ops.utils.SessionManager
import kotlinx.coroutines.launch

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var b: ActivityAdminDashboardBinding
    private lateinit var session: SessionManager
    private lateinit var adapter: LaporanAdapter
    private var currentFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(b.root)
        session = SessionManager.getInstance(this)

        b.tvNama.text = "Halo, ${session.getName()} 🛡️"
        b.tvRole.text = "Admin / Team Leader"

        adapter = LaporanAdapter { r ->
            startActivity(Intent(this, DetailLaporanActivity::class.java).putExtra("REPORT_ID", r.id))
        }
        b.rvLaporan.layoutManager = LinearLayoutManager(this)
        b.rvLaporan.adapter = adapter

        listOf("Semua" to null, "Menunggu" to "menunggu", "Disetujui" to "disetujui", "Ditolak" to "ditolak")
            .forEach { (label, value) ->
                b.chipGroup.addView(Chip(this).apply {
                    text = label; isCheckable = true; isChecked = (value == currentFilter)
                    setOnClickListener { currentFilter = value; loadReports() }
                })
            }
        (b.chipGroup.getChildAt(0) as? Chip)?.isChecked = true

        b.swipeRefresh.setOnRefreshListener { loadAll() }

        // UC-14/09/10: Navigasi ke Kelola User
        b.btnKelolaUser.setOnClickListener {
            startActivity(Intent(this, KelolaUserActivity::class.java))
        }

        // Navigasi ke Kelola Tower (kategori lokasi)
        b.btnKelolaTower.setOnClickListener {
            startActivity(Intent(this, KelolaTowerActivity::class.java))
        }

        // UC-02: Navigasi ke Profil
        b.btnProfil.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        b.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle("Logout").setMessage("Yakin keluar?")
                .setPositiveButton("Ya") { _, _ ->
                    session.clearSession()
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }.setNegativeButton("Batal", null).show()
        }
        loadAll()
    }

    override fun onResume() { super.onResume(); loadAll() }

    private fun loadAll() { loadStats(); loadReports() }

    private fun loadStats() {
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getDashboardStats()
                if (r.isSuccessful) r.body()?.data?.let { s ->
                    b.tvTotal.text     = s.total.toString()
                    b.tvMenunggu.text  = s.menunggu.toString()
                    b.tvDisetujui.text = s.disetujui.toString()
                    b.tvDitolak.text   = s.ditolak.toString()
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadReports() {
        lifecycleScope.launch {
            b.progressBar.visibility = View.VISIBLE
            try {
                val r = RetrofitClient.instance.getAllReports(status = currentFilter)
                if (r.isSuccessful) {
                    val list = r.body()?.data?.reports ?: emptyList()
                    adapter.submitList(list)
                    b.tvEmpty.visibility   = if (list.isEmpty()) View.VISIBLE else View.GONE
                    b.rvLaporan.visibility = if (list.isEmpty()) View.GONE   else View.VISIBLE
                }
            } catch (_: Exception) {}
            finally { b.progressBar.visibility = View.GONE; b.swipeRefresh.isRefreshing = false }
        }
    }
}
