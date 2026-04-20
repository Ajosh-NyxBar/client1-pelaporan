const express = require('express');
const router  = express.Router();
const bcrypt  = require('bcryptjs');
const db = require('../config/database');
const { authMiddleware, roleMiddleware } = require('../middleware/auth');

// GET /api/users  — daftar semua user (admin only)
router.get('/', authMiddleware, roleMiddleware('admin'), async (req, res) => {
  try {
    const [users] = await db.execute(
      'SELECT id, username, name, role, is_active, created_at FROM users ORDER BY role, name'
    );
    res.json({ success: true, data: users });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Server error.' });
  }
});

// POST /api/users  — tambah user (admin only: input data teknisi)
router.post('/', authMiddleware, roleMiddleware('admin'), async (req, res) => {
  try {
    const { username, password, name, role } = req.body;

    if (!username || !password || !name) {
      return res.status(400).json({
        success: false,
        message: 'Username, password, dan nama harus diisi.',
      });
    }

    const validRoles = ['teknisi', 'admin', 'helpdesk'];
    const userRole = validRoles.includes(role) ? role : 'teknisi';

    if (password.length < 6) {
      return res.status(400).json({ success: false, message: 'Password minimal 6 karakter.' });
    }

    // Cek apakah username sudah dipakai
    const [existing] = await db.execute('SELECT id FROM users WHERE username = ?', [username]);
    if (existing.length > 0) {
      return res.status(409).json({ success: false, message: 'Username sudah digunakan.' });
    }

    const hash = await bcrypt.hash(password, 10);
    const [result] = await db.execute(
      'INSERT INTO users (username, password, name, role) VALUES (?, ?, ?, ?)',
      [username, hash, name, userRole]
    );

    res.status(201).json({
      success: true,
      message: `User ${username} berhasil ditambahkan.`,
      data: { id: result.insertId, username, name, role: userRole },
    });
  } catch (err) {
    console.error('Create user error:', err);
    res.status(500).json({ success: false, message: 'Gagal menambahkan user.' });
  }
});

// PUT /api/users/:id  — edit data user (admin only)
router.put('/:id', authMiddleware, roleMiddleware('admin'), async (req, res) => {
  try {
    const { name, role, is_active, password } = req.body;
    const userId = req.params.id;

    // Cek user ada
    const [rows] = await db.execute('SELECT * FROM users WHERE id = ?', [userId]);
    if (!rows.length) {
      return res.status(404).json({ success: false, message: 'User tidak ditemukan.' });
    }

    // Build dynamic update
    const updates = [];
    const params  = [];

    if (name !== undefined && name.trim()) {
      updates.push('name = ?');
      params.push(name.trim());
    }
    if (role !== undefined) {
      const validRoles = ['teknisi', 'admin', 'helpdesk'];
      if (validRoles.includes(role)) {
        updates.push('role = ?');
        params.push(role);
      }
    }
    if (is_active !== undefined) {
      updates.push('is_active = ?');
      params.push(is_active ? 1 : 0);
    }
    if (password !== undefined && password.length >= 6) {
      const hash = await bcrypt.hash(password, 10);
      updates.push('password = ?');
      params.push(hash);
    }

    if (updates.length === 0) {
      return res.status(400).json({ success: false, message: 'Tidak ada data yang diubah.' });
    }

    params.push(userId);
    await db.execute(`UPDATE users SET ${updates.join(', ')} WHERE id = ?`, params);

    // Ambil data user terbaru
    const [updated] = await db.execute(
      'SELECT id, username, name, role, is_active, created_at FROM users WHERE id = ?',
      [userId]
    );

    res.json({
      success: true,
      message: 'Data user berhasil diperbarui.',
      data: updated[0],
    });
  } catch (err) {
    console.error('Update user error:', err);
    res.status(500).json({ success: false, message: 'Gagal memperbarui data user.' });
  }
});

// DELETE /api/users/:id  — hapus user (admin only, soft delete via is_active)
router.delete('/:id', authMiddleware, roleMiddleware('admin'), async (req, res) => {
  try {
    const userId = req.params.id;

    // Tidak bisa menghapus diri sendiri
    if (parseInt(userId) === req.user.id) {
      return res.status(400).json({ success: false, message: 'Tidak bisa menghapus akun sendiri.' });
    }

    const [rows] = await db.execute('SELECT * FROM users WHERE id = ?', [userId]);
    if (!rows.length) {
      return res.status(404).json({ success: false, message: 'User tidak ditemukan.' });
    }

    // Soft delete: non-aktifkan user
    await db.execute('UPDATE users SET is_active = 0 WHERE id = ?', [userId]);

    res.json({
      success: true,
      message: `User ${rows[0].username} berhasil dinonaktifkan.`,
    });
  } catch (err) {
    console.error('Delete user error:', err);
    res.status(500).json({ success: false, message: 'Gagal menghapus user.' });
  }
});

module.exports = router;
