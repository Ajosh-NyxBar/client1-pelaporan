package com.laporan.ops.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.laporan.ops.api.RetrofitClient
import com.laporan.ops.databinding.ActivityProfileBinding
import com.laporan.ops.model.ChangePasswordRequest
import com.laporan.ops.utils.SessionManager
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var b: ActivityProfileBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(b.root)
        session = SessionManager.getInstance(this)

        setSupportActionBar(b.toolbar)
        supportActionBar?.apply { setDisplayHomeAsUpEnabled(true); title = "Profil Saya" }
        b.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        loadProfile()
        setupChangePassword()
    }

    private fun loadProfile() {
        // Show from local session first
        b.tvName.text     = session.getName() ?: "–"
        b.tvUsername.text  = "@${session.getUsername()}"

        val role = session.getRole() ?: "teknisi"
        b.chipRole.text = role.replaceFirstChar { it.uppercase() }

        // Also fetch from server to get latest data
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.instance.getProfile()
                if (resp.isSuccessful && resp.body()?.success == true) {
                    resp.body()?.data?.let { user ->
                        b.tvName.text    = user.name
                        b.tvUsername.text = "@${user.username}"
                        b.chipRole.text  = user.role.replaceFirstChar { it.uppercase() }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun setupChangePassword() {
        b.btnChangePass.setOnClickListener {
            val oldPass     = b.etOldPass.text?.toString()?.trim().orEmpty()
            val newPass     = b.etNewPass.text?.toString()?.trim().orEmpty()
            val confirmPass = b.etConfirmPass.text?.toString()?.trim().orEmpty()

            var valid = true
            if (oldPass.isEmpty()) { b.tilOldPass.error = "Wajib diisi"; valid = false }
            else b.tilOldPass.error = null

            if (newPass.length < 6) { b.tilNewPass.error = "Minimal 6 karakter"; valid = false }
            else b.tilNewPass.error = null

            if (confirmPass != newPass) { b.tilConfirmPass.error = "Tidak cocok"; valid = false }
            else b.tilConfirmPass.error = null

            if (valid) doChangePassword(oldPass, newPass)
        }
    }

    private fun doChangePassword(oldPass: String, newPass: String) {
        b.progressBar.visibility = View.VISIBLE
        b.btnChangePass.isEnabled = false

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.instance.changePassword(
                    ChangePasswordRequest(oldPass, newPass)
                )
                if (resp.isSuccessful && resp.body()?.success == true) {
                    snack("✅ Password berhasil diubah!")
                    b.etOldPass.text?.clear()
                    b.etNewPass.text?.clear()
                    b.etConfirmPass.text?.clear()
                } else {
                    snack(resp.body()?.message ?: "Gagal mengubah password.")
                }
            } catch (e: Exception) {
                snack("Error: ${e.message}")
            } finally {
                b.progressBar.visibility = View.GONE
                b.btnChangePass.isEnabled = true
            }
        }
    }

    private fun snack(msg: String) = Snackbar.make(b.root, msg, Snackbar.LENGTH_LONG).show()
}
