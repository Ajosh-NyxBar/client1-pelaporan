package com.laporan.ops.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.laporan.ops.api.RetrofitClient
import com.laporan.ops.databinding.ActivityLoginBinding
import com.laporan.ops.model.LoginRequest
import com.laporan.ops.ui.admin.AdminDashboardActivity
import com.laporan.ops.ui.helpdesk.HelpdeskDashboardActivity
import com.laporan.ops.ui.teknisi.TeknisiDashboardActivity
import com.laporan.ops.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        session = SessionManager.getInstance(this)

        // Auto-login jika sudah ada sesi
        if (session.isLoggedIn()) {
            session.initToken()
            navigateToDashboard(session.getRole() ?: "teknisi")
            return
        }

        b.btnLogin.setOnClickListener {
            val username = b.etUsername.text?.toString()?.trim().orEmpty()
            val password = b.etPassword.text?.toString()?.trim().orEmpty()

            var valid = true
            if (username.isEmpty()) { b.tilUsername.error = "Username harus diisi"; valid = false }
            else b.tilUsername.error = null
            if (password.isEmpty()) { b.tilPassword.error = "Password harus diisi"; valid = false }
            else b.tilPassword.error = null

            if (valid) doLogin(username, password)
        }
    }

    private fun doLogin(username: String, password: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.login(LoginRequest(username, password))
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()!!.data!!
                    session.saveSession(data.token, data.user)
                    snack("Selamat datang, ${data.user.name}!")
                    navigateToDashboard(data.user.role)
                } else {
                    snack(response.body()?.message ?: "Username atau password salah.")
                }
            } catch (e: Exception) {
                snack("Gagal terhubung ke server. Periksa koneksi Anda.")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun navigateToDashboard(role: String) {
        val intent = when (role) {
            "admin"    -> Intent(this, AdminDashboardActivity::class.java)
            "helpdesk" -> Intent(this, HelpdeskDashboardActivity::class.java)
            else       -> Intent(this, TeknisiDashboardActivity::class.java)
        }.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
        startActivity(intent)
        finish()
    }

    private fun setLoading(on: Boolean) {
        b.progressBar.visibility = if (on) View.VISIBLE else View.GONE
        b.btnLogin.isEnabled = !on
        b.btnLogin.text = if (on) "Memproses..." else "MASUK"
        b.etUsername.isEnabled = !on
        b.etPassword.isEnabled = !on
    }

    private fun snack(msg: String) = Snackbar.make(b.root, msg, Snackbar.LENGTH_LONG).show()
}
