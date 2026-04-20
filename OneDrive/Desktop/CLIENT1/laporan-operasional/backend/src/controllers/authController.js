const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const db = require('../config/database');

// POST /api/auth/login
const login = async (req, res) => {
  try {
    const { username, password } = req.body;

    if (!username || !password) {
      return res.status(400).json({
        success: false,
        message: 'Username dan password harus diisi.',
      });
    }

    const [rows] = await db.execute(
      'SELECT * FROM users WHERE username = ? AND is_active = 1',
      [username]
    );

    if (rows.length === 0) {
      return res.status(401).json({
        success: false,
        message: 'Username atau password salah.',
      });
    }

    const user = rows[0];
    const isMatch = await bcrypt.compare(password, user.password);

    if (!isMatch) {
      return res.status(401).json({
        success: false,
        message: 'Username atau password salah.',
      });
    }

    const token = jwt.sign(
      { id: user.id, username: user.username, name: user.name, role: user.role },
      process.env.JWT_SECRET,
      { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
    );

    res.json({
      success: true,
      message: 'Login berhasil!',
      data: {
        token,
        user: { id: user.id, username: user.username, name: user.name, role: user.role },
      },
    });
  } catch (err) {
    console.error('Login error:', err);
    res.status(500).json({ success: false, message: 'Terjadi kesalahan pada server.' });
  }
};

// GET /api/auth/profile
const getProfile = async (req, res) => {
  try {
    const [rows] = await db.execute(
      'SELECT id, username, name, role, created_at FROM users WHERE id = ?',
      [req.user.id]
    );
    if (!rows.length) {
      return res.status(404).json({ success: false, message: 'User tidak ditemukan.' });
    }
    res.json({ success: true, data: rows[0] });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Terjadi kesalahan pada server.' });
  }
};

// PUT /api/auth/change-password
const changePassword = async (req, res) => {
  try {
    const { old_password, new_password } = req.body;

    if (!old_password || !new_password) {
      return res.status(400).json({ success: false, message: 'Password lama dan baru harus diisi.' });
    }
    if (new_password.length < 6) {
      return res.status(400).json({ success: false, message: 'Password baru minimal 6 karakter.' });
    }

    const [rows] = await db.execute('SELECT password FROM users WHERE id = ?', [req.user.id]);
    const isMatch = await bcrypt.compare(old_password, rows[0].password);

    if (!isMatch) {
      return res.status(401).json({ success: false, message: 'Password lama tidak sesuai.' });
    }

    const hashed = await bcrypt.hash(new_password, 10);
    await db.execute('UPDATE users SET password = ? WHERE id = ?', [hashed, req.user.id]);

    res.json({ success: true, message: 'Password berhasil diubah.' });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Terjadi kesalahan pada server.' });
  }
};

module.exports = { login, getProfile, changePassword };
