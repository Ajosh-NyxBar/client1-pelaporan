const db = require('../config/database');

// Generate kode laporan unik
const generateCode = () => {
  const d = new Date();
  const ymd = `${d.getFullYear()}${String(d.getMonth()+1).padStart(2,'0')}${String(d.getDate()).padStart(2,'0')}`;
  return `RPT-${ymd}-${Math.floor(Math.random() * 9000) + 1000}`;
};

// GET /api/reports/stats
const getDashboardStats = async (req, res) => {
  try {
    const where  = req.user.role === 'teknisi' ? 'WHERE teknisi_id = ?' : '';
    const params = req.user.role === 'teknisi' ? [req.user.id] : [];
    const [rows] = await db.execute(`
      SELECT
        COUNT(*) as total,
        SUM(CASE WHEN status='menunggu'  THEN 1 ELSE 0 END) as menunggu,
        SUM(CASE WHEN status='disetujui' THEN 1 ELSE 0 END) as disetujui,
        SUM(CASE WHEN status='ditolak'   THEN 1 ELSE 0 END) as ditolak
      FROM reports ${where}
    `, params);
    res.json({ success: true, data: rows[0] });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Terjadi kesalahan pada server.' });
  }
};

// GET /api/reports
const getAllReports = async (req, res) => {
  try {
    const { status } = req.query;
    const page  = parseInt(req.query.page)  || 1;
    const limit = parseInt(req.query.limit) || 20;
    const offset = (page - 1) * limit;

    let where  = 'WHERE 1=1';
    const params = [];

    if (req.user.role === 'teknisi') {
      where += ' AND r.teknisi_id = ?';
      params.push(req.user.id);
    }
    if (status) {
      where += ' AND r.status = ?';
      params.push(status);
    }

    // Gunakan query() bukan execute() — prepared statements punya bug
    // dengan LIMIT/OFFSET pada beberapa konfigurasi MySQL
    const [reports] = await db.query(
      `SELECT r.*, u.name AS teknisi_name, u.username AS teknisi_username,
             v.name AS validator_name,
             (SELECT COUNT(*) FROM report_photos WHERE report_id = r.id) AS photo_count
      FROM reports r
      JOIN users u ON r.teknisi_id = u.id
      LEFT JOIN users v ON r.validated_by = v.id
      ${where}
      ORDER BY r.created_at DESC
      LIMIT ${limit} OFFSET ${offset}`,
      params
    );

    const [count] = await db.execute(
      `SELECT COUNT(*) AS total FROM reports r ${where}`, params
    );

    res.json({
      success: true,
      data: {
        reports,
        pagination: {
          total: count[0].total,
          page,
          limit,
          totalPages: Math.ceil(count[0].total / limit),
        },
      },
    });
  } catch (err) {
    console.error('getAllReports error:', err);
    res.status(500).json({ success: false, message: 'Terjadi kesalahan pada server.' });
  }
};

// GET /api/reports/:id
const getReportById = async (req, res) => {
  try {
    const [rows] = await db.execute(`
      SELECT r.*, u.name AS teknisi_name, u.username AS teknisi_username,
             v.name AS validator_name,
             h.name AS helpdesk_name
      FROM reports r
      JOIN users u ON r.teknisi_id = u.id
      LEFT JOIN users v ON r.validated_by = v.id
      LEFT JOIN users h ON r.tindak_lanjut_by = h.id
      WHERE r.id = ?
    `, [req.params.id]);

    if (!rows.length) {
      return res.status(404).json({ success: false, message: 'Laporan tidak ditemukan.' });
    }

    const report = rows[0];

    if (req.user.role === 'teknisi' && report.teknisi_id !== req.user.id) {
      return res.status(403).json({ success: false, message: 'Akses ditolak.' });
    }

    const [photos] = await db.execute(
      'SELECT * FROM report_photos WHERE report_id = ?', [req.params.id]
    );
    report.photos = photos;

    res.json({ success: true, data: report });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Terjadi kesalahan pada server.' });
  }
};

// POST /api/reports  (teknisi & admin)
const createReport = async (req, res) => {
  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();

    const { jenis_pekerjaan, lokasi, waktu_kerja, deskripsi, teknisi_id } = req.body;

    if (!jenis_pekerjaan || !lokasi || !waktu_kerja || !deskripsi) {
      return res.status(400).json({
        success: false,
        message: 'Semua field wajib diisi: jenis pekerjaan, lokasi, waktu, deskripsi.',
      });
    }

    // Admin bisa menentukan teknisi_id, atau default ke diri sendiri
    const assignedTeknisiId = (req.user.role === 'admin' && teknisi_id) ? teknisi_id : req.user.id;

    const code = generateCode();
    const [result] = await conn.execute(`
      INSERT INTO reports (report_code, teknisi_id, jenis_pekerjaan, lokasi, waktu_kerja, deskripsi)
      VALUES (?, ?, ?, ?, ?, ?)
    `, [code, assignedTeknisiId, jenis_pekerjaan, lokasi, waktu_kerja, deskripsi]);

    if (req.files && req.files.length > 0) {
      for (const file of req.files) {
        await conn.execute(
          'INSERT INTO report_photos (report_id, photo_path) VALUES (?, ?)',
          [result.insertId, `/uploads/${file.filename}`]
        );
      }
    }

    await conn.commit();
    res.status(201).json({
      success: true,
      message: 'Laporan berhasil dikirim! Status: Menunggu Validasi.',
      data: { id: result.insertId, report_code: code, status: 'menunggu' },
    });
  } catch (err) {
    await conn.rollback();
    console.error(err);
    res.status(500).json({ success: false, message: 'Gagal menyimpan laporan.' });
  } finally {
    conn.release();
  }
};

// PATCH /api/reports/:id/validate  (admin only)
const validateReport = async (req, res) => {
  try {
    const { action, catatan } = req.body;

    if (!['approve', 'reject'].includes(action)) {
      return res.status(400).json({ success: false, message: "Action harus 'approve' atau 'reject'." });
    }
    if (action === 'reject' && !catatan) {
      return res.status(400).json({ success: false, message: 'Catatan wajib diisi jika laporan ditolak.' });
    }

    const [rows] = await db.execute('SELECT * FROM reports WHERE id = ?', [req.params.id]);
    if (!rows.length) {
      return res.status(404).json({ success: false, message: 'Laporan tidak ditemukan.' });
    }
    if (rows[0].status !== 'menunggu') {
      return res.status(400).json({ success: false, message: 'Laporan sudah divalidasi sebelumnya.' });
    }

    const newStatus = action === 'approve' ? 'disetujui' : 'ditolak';
    await db.execute(`
      UPDATE reports
      SET status = ?, validated_by = ?, validated_at = NOW(), catatan_validasi = ?
      WHERE id = ?
    `, [newStatus, req.user.id, catatan || null, req.params.id]);

    res.json({
      success: true,
      message: `Laporan berhasil ${action === 'approve' ? 'disetujui' : 'ditolak'}.`,
      data: { id: parseInt(req.params.id), status: newStatus },
    });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Terjadi kesalahan pada server.' });
  }
};

// PATCH /api/reports/:id/follow-up  (helpdesk only — UC-17: Tindak Lanjut)
const addFollowUp = async (req, res) => {
  try {
    const { tindak_lanjut } = req.body;

    if (!tindak_lanjut || !tindak_lanjut.trim()) {
      return res.status(400).json({ success: false, message: 'Catatan tindak lanjut wajib diisi.' });
    }

    const [rows] = await db.execute('SELECT * FROM reports WHERE id = ?', [req.params.id]);
    if (!rows.length) {
      return res.status(404).json({ success: false, message: 'Laporan tidak ditemukan.' });
    }
    if (rows[0].status !== 'disetujui') {
      return res.status(400).json({ success: false, message: 'Tindak lanjut hanya untuk laporan yang sudah disetujui.' });
    }

    await db.execute(`
      UPDATE reports
      SET tindak_lanjut = ?, tindak_lanjut_by = ?, tindak_lanjut_at = NOW()
      WHERE id = ?
    `, [tindak_lanjut.trim(), req.user.id, req.params.id]);

    res.json({
      success: true,
      message: 'Tindak lanjut berhasil disimpan.',
      data: { id: parseInt(req.params.id) },
    });
  } catch (err) {
    console.error('Follow-up error:', err);
    res.status(500).json({ success: false, message: 'Terjadi kesalahan pada server.' });
  }
};

module.exports = { getDashboardStats, getAllReports, getReportById, createReport, validateReport, addFollowUp };
