package com.laporan.ops.ui.teknisi

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
import com.laporan.ops.databinding.ActivityTeknisiDashboardBinding
import com.laporan.ops.ui.LoginActivity
import com.laporan.ops.ui.ProfileActivity
import com.laporan.ops.ui.admin.DetailLaporanActivity
import com.laporan.ops.utils.SessionManager
import kotlinx.coroutines.launch

class TeknisiDashboardActivity : AppCompatActivity() {

    private lateinit var b: ActivityTeknisiDashboardBinding
    private lateinit var session: SessionManager
    private lateinit var adapter: LaporanAdapter
    private var currentFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTeknisiDashboardBinding.inflate(layoutInflater)
        setContentView(b.root)
        session = SessionManager.getInstance(this)

        setupHeader()
        setupRecyclerView()
        setupFilters()
        b.swipeRefresh.setOnRefreshListener { loadAll() }
        b.fabTambah.setOnClickListener { startActivity(Intent(this, TambahLaporanActivity::class.java)) }
        b.btnLogout.setOnClickListener { confirmLogout() }

        // UC-02: Navigasi ke Profil (klik nama atau avatar)
        b.tvNama.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }

        loadAll()
    }

    override fun onResume() { super.onResume(); loadAll() }

    private fun setupHeader() {
        b.tvNama.text = "Halo, ${session.getName()} 👷"
        b.tvRole.text = "Teknisi"
    }

    private fun setupRecyclerView() {
        adapter = LaporanAdapter { report ->
            startActivity(Intent(this, DetailLaporanActivity::class.java).putExtra("REPORT_ID", report.id))
        }
        b.rvLaporan.layoutManager = LinearLayoutManager(this)
        b.rvLaporan.adapter = adapter
    }

    private fun setupFilters() {
        listOf("Semua" to null, "Menunggu" to "menunggu", "Disetujui" to "disetujui", "Ditolak" to "ditolak")
            .forEach { (label, value) ->
                val chip = Chip(this).apply {
                    text = label
                    isCheckable = true
                    isChecked = (value == currentFilter)
                    setOnClickListener { currentFilter = value; loadReports() }
                }
                b.chipGroup.addView(chip)
            }
        (b.chipGroup.getChildAt(0) as? Chip)?.isChecked = true
    }

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
            finally {
                b.progressBar.visibility = View.GONE
                b.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Yakin ingin keluar?")
            .setPositiveButton("Ya, Keluar") { _, _ ->
                session.clearSession()
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
