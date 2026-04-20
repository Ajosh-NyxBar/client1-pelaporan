package com.laporan.ops.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.laporan.ops.R
import com.laporan.ops.adapter.UserAdapter
import com.laporan.ops.api.RetrofitClient
import com.laporan.ops.databinding.ActivityKelolaUserBinding
import com.laporan.ops.model.*
import kotlinx.coroutines.launch

class KelolaUserActivity : AppCompatActivity() {

    private lateinit var b: ActivityKelolaUserBinding
    private lateinit var adapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityKelolaUserBinding.inflate(layoutInflater)
        setContentView(b.root)

        setSupportActionBar(b.toolbar)
        supportActionBar?.apply { setDisplayHomeAsUpEnabled(true); title = "Kelola User" }
        b.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        adapter = UserAdapter { user -> showUserOptionsDialog(user) }
        b.rvUsers.layoutManager = LinearLayoutManager(this)
        b.rvUsers.adapter = adapter

        b.swipeRefresh.setOnRefreshListener { loadUsers() }
        b.fabTambah.setOnClickListener { showAddUserDialog() }

        loadUsers()
    }

    private fun loadUsers() {
        b.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.instance.getAllUsers()
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val users = resp.body()!!.data ?: emptyList()
                    adapter.submitList(users)
                    b.tvUserCount.text = "${users.size} user terdaftar"
                    b.layoutEmpty.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
                    b.rvUsers.visibility = if (users.isEmpty()) View.GONE else View.VISIBLE
                }
            } catch (e: Exception) {
                snack("Gagal memuat data: ${e.message}")
            } finally {
                b.progressBar.visibility = View.GONE
                b.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showAddUserDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_tambah_user, null)
        val etName     = dialogView.findViewById<TextInputEditText>(R.id.etName)
        val etUsername  = dialogView.findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword = dialogView.findViewById<TextInputEditText>(R.id.etPassword)
        val spRole     = dialogView.findViewById<Spinner>(R.id.spinnerRole)

        val roles = arrayOf("teknisi", "admin", "helpdesk")
        spRole.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)

        MaterialAlertDialogBuilder(this)
            .setTitle("👤 Tambah User Baru")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val name     = etName.text?.toString()?.trim().orEmpty()
                val username = etUsername.text?.toString()?.trim().orEmpty()
                val password = etPassword.text?.toString()?.trim().orEmpty()
                val role     = roles[spRole.selectedItemPosition]

                if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    snack("Semua field wajib diisi!")
                    return@setPositiveButton
                }
                if (password.length < 6) {
                    snack("Password minimal 6 karakter!")
                    return@setPositiveButton
                }

                doCreateUser(CreateUserRequest(username, password, name, role))
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun doCreateUser(req: CreateUserRequest) {
        b.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.instance.createUser(req)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    snack("✅ ${resp.body()!!.message}")
                    loadUsers()
                } else {
                    snack(resp.body()?.message ?: "Gagal menambahkan user.")
                }
            } catch (e: Exception) {
                snack("Error: ${e.message}")
            } finally {
                b.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showUserOptionsDialog(user: UserDetail) {
        val options = arrayOf("✏️ Edit Nama", "🔄 Ganti Role", "🔑 Reset Password",
            if (user.isActive == 1) "🚫 Nonaktifkan" else "✅ Aktifkan",
            "🗑️ Hapus")

        MaterialAlertDialogBuilder(this)
            .setTitle("${user.name} (@${user.username})")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditNameDialog(user)
                    1 -> showChangeRoleDialog(user)
                    2 -> showResetPasswordDialog(user)
                    3 -> doToggleActive(user)
                    4 -> confirmDelete(user)
                }
            }
            .show()
    }

    private fun showEditNameDialog(user: UserDetail) {
        val input = TextInputEditText(this).apply {
            setText(user.name); setPadding(48, 24, 48, 8)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Nama")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isNotEmpty()) doUpdateUser(user.id, UpdateUserRequest(name = name))
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun showChangeRoleDialog(user: UserDetail) {
        val roles = arrayOf("teknisi", "admin", "helpdesk")
        val current = roles.indexOf(user.role).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle("Ganti Role")
            .setSingleChoiceItems(roles, current) { dialog, which ->
                doUpdateUser(user.id, UpdateUserRequest(role = roles[which]))
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun showResetPasswordDialog(user: UserDetail) {
        val input = TextInputEditText(this).apply {
            hint = "Password baru (min 6 karakter)"; setPadding(48, 24, 48, 8)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Reset Password — ${user.name}")
            .setView(input)
            .setPositiveButton("Reset") { _, _ ->
                val pass = input.text?.toString()?.trim().orEmpty()
                if (pass.length >= 6) doUpdateUser(user.id, UpdateUserRequest(password = pass))
                else snack("Password minimal 6 karakter!")
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun doToggleActive(user: UserDetail) {
        val newState = user.isActive != 1
        doUpdateUser(user.id, UpdateUserRequest(isActive = newState))
    }

    private fun confirmDelete(user: UserDetail) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus User")
            .setMessage("Yakin ingin menonaktifkan ${user.name}?")
            .setPositiveButton("Ya, Hapus") { _, _ -> doDeleteUser(user.id) }
            .setNegativeButton("Batal", null).show()
    }

    private fun doUpdateUser(id: Int, req: UpdateUserRequest) {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.instance.updateUser(id, req)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    snack("✅ ${resp.body()!!.message}")
                    loadUsers()
                } else {
                    snack(resp.body()?.message ?: "Gagal memperbarui.")
                }
            } catch (e: Exception) { snack("Error: ${e.message}") }
        }
    }

    private fun doDeleteUser(id: Int) {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.instance.deleteUser(id)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    snack("✅ ${resp.body()!!.message}")
                    loadUsers()
                } else {
                    snack(resp.body()?.message ?: "Gagal menghapus.")
                }
            } catch (e: Exception) { snack("Error: ${e.message}") }
        }
    }

    private fun snack(msg: String) = Snackbar.make(b.root, msg, Snackbar.LENGTH_LONG).show()
}
