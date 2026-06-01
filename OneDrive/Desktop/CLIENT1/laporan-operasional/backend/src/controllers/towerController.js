const db = require('../config/database');

// GET /api/towers
// Query: ?active=1 untuk hanya yang aktif (default semua untuk admin, hanya aktif untuk teknisi)
const getAllTowers = async (req, res) => {
  try {
    // Teknisi/helpdesk hanya melihat tower aktif. Admin bisa lihat semua.
    const adminView = req.user.role === 'admin' && req.query.all === '1';
    const where = adminView ? '' : 'WHERE is_active = 1';

    const [rows] = await db.execute(
      `SELECT id, nama, alamat, is_active, created_at, updated_at
       FROM towers ${where}
       ORDER BY nama ASC`
    );
    res.json({ success: true, data: rows });
  } catch (err) {
    console.error('getAllTowers error:', err);
    res.status(500).json({ success: false, message: 'Terjadi kesalahan pada server.' });
  }
};

// POST /api/towers  (admin only)
const createTower = async (req, res) => {
  try {
    const { nama, alamat } = req.body;
    if (!nama || !nama.trim()) {
      return res.status(400).json({ success: false, message: 'Nama tower wajib diisi.' });
    }

    const namaTrim = nama.trim();
    const alamatVal = (alamat && alamat.trim()) ? alamat.trim() : null;

    // Cek nama unik
    const [dup] = await db.execute('SELECT id FROM towers WHERE nama = ?', [namaTrim]);
    if (dup.length > 0) {
      return res.status(409).json({ success: false, message: 'Nama tower sudah digunakan.' });
    }

    const [result] = await db.execute(
      'INSERT INTO towers (nama, alamat) VALUES (?, ?)',
      [namaTrim, alamatVal]
    );

    res.status(201).json({
      success: true,
      message: `Tower "${namaTrim}" berhasil ditambahkan.`,
      data: { id: result.insertId, nama: namaTrim, alamat: alamatVal, is_active: 1 },
    });
  } catch (err) {
    console.error('createTower error:', err);
    res.status(500).json({ success: false, message: 'Gagal menambahkan tower.' });
  }
};

// PUT /api/towers/:id  (admin only)
const updateTower = async (req, res) => {
  try {
    const { nama, alamat, is_active } = req.body;
    const towerId = req.params.id;

    const [rows] = await db.execute('SELECT * FROM towers WHERE id = ?', [towerId]);
    if (!rows.length) {
      return res.status(404).json({ success: false, message: 'Tower tidak ditemukan.' });
    }

    const updates = [];
    const params  = [];

    if (nama !== undefined && nama.trim()) {
      // Cek duplikat (kecuali milik tower itu sendiri)
      const [dup] = await db.execute(
        'SELECT id FROM towers WHERE nama = ? AND id <> ?',
        [nama.trim(), towerId]
      );
      if (dup.length > 0) {
        return res.status(409).json({ success: false, message: 'Nama tower sudah digunakan.' });
      }
      updates.push('nama = ?');
      params.push(nama.trim());
    }
    if (alamat !== undefined) {
      updates.push('alamat = ?');
      params.push(alamat && alamat.trim() ? alamat.trim() : null);
    }
    if (is_active !== undefined) {
      updates.push('is_active = ?');
      params.push(is_active ? 1 : 0);
    }

    if (updates.length === 0) {
      return res.status(400).json({ success: false, message: 'Tidak ada data yang diubah.' });
    }

    params.push(towerId);
    await db.execute(`UPDATE towers SET ${updates.join(', ')} WHERE id = ?`, params);

    const [updated] = await db.execute(
      'SELECT id, nama, alamat, is_active, created_at, updated_at FROM towers WHERE id = ?',
      [towerId]
    );

    res.json({
      success: true,
      message: 'Data tower berhasil diperbarui.',
      data: updated[0],
    });
  } catch (err) {
    console.error('updateTower error:', err);
    res.status(500).json({ success: false, message: 'Gagal memperbarui tower.' });
  }
};

// DELETE /api/towers/:id  (admin only — soft delete via is_active=0)
const deleteTower = async (req, res) => {
  try {
    const towerId = req.params.id;

    const [rows] = await db.execute('SELECT * FROM towers WHERE id = ?', [towerId]);
    if (!rows.length) {
      return res.status(404).json({ success: false, message: 'Tower tidak ditemukan.' });
    }

    // Soft delete: nonaktifkan supaya laporan historis tetap referensi
    await db.execute('UPDATE towers SET is_active = 0 WHERE id = ?', [towerId]);

    res.json({
      success: true,
      message: `Tower "${rows[0].nama}" berhasil dinonaktifkan.`,
    });
  } catch (err) {
    console.error('deleteTower error:', err);
    res.status(500).json({ success: false, message: 'Gagal menghapus tower.' });
  }
};

module.exports = { getAllTowers, createTower, updateTower, deleteTower };
