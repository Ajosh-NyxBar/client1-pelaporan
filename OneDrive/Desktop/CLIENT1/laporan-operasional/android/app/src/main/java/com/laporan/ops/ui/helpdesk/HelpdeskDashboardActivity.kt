package com.laporan.ops.ui.helpdesk

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.laporan.ops.adapter.LaporanAdapter
import com.laporan.ops.api.RetrofitClient
import com.laporan.ops.databinding.ActivityHelpdeskDashboardBinding
import com.laporan.ops.ui.LoginActivity
import com.laporan.ops.ui.ProfileActivity
import com.laporan.ops.ui.admin.DetailLaporanActivity
import com.laporan.ops.utils.SessionManager
import kotlinx.coroutines.launch

class HelpdeskDashboardActivity : AppCompatActivity() {

    private lateinit var b: ActivityHelpdeskDashboardBinding
    private lateinit var session: SessionManager
    private lateinit var adapter: LaporanAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHelpdeskDashboardBinding.inflate(layoutInflater)
        setContentView(b.root)
        session = SessionManager.getInstance(this)

        b.tvNama.text = "Halo, ${session.getName()} 🖥️"
        b.tvRole.text = "Helpdesk — Monitoring"

        adapter = LaporanAdapter { r ->
            startActivity(Intent(this, DetailLaporanActivity::class.java).putExtra("REPORT_ID", r.id))
        }
        b.rvLaporan.layoutManager = LinearLayoutManager(this)
        b.rvLaporan.adapter = adapter

        b.swipeRefresh.setOnRefreshListener { loadAll() }

        // UC-02: Navigasi ke Profil (klik nama)
        b.tvNama.setOnClickListener {
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
                // Helpdesk hanya melihat laporan yang sudah disetujui
                val r = RetrofitClient.instance.getAllReports(status = "disetujui")
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
