const express = require('express');
const router  = express.Router();
const { authMiddleware, roleMiddleware } = require('../middleware/auth');
const {
  getDashboardStats, getAllReports, getReportById, createReport, validateReport, addFollowUp,
} = require('../controllers/reportController');
const upload = require('../middleware/upload');

router.get('/stats',             authMiddleware, getDashboardStats);
router.get('/',                  authMiddleware, getAllReports);
router.get('/:id',               authMiddleware, getReportById);

// Teknisi DAN Admin bisa buat laporan (Admin = tambah kegiatan laporan harian)
router.post('/',                 authMiddleware, roleMiddleware('teknisi', 'admin'), (req, res, next) => {
  upload.array('photos', 5)(req, res, (err) => {
    if (err) {
      if (err.code === 'LIMIT_FILE_SIZE') {
        return res.status(400).json({ success: false, message: 'Ukuran file melebihi batas 5MB.' });
      }
      if (err.code === 'LIMIT_FILE_COUNT' || err.code === 'LIMIT_UNEXPECTED_FILE') {
        return res.status(400).json({ success: false, message: 'Maksimal 5 foto per laporan.' });
      }
      return res.status(400).json({ success: false, message: err.message || 'Gagal mengupload file.' });
    }
    next();
  });
}, createReport);

router.patch('/:id/validate',    authMiddleware, roleMiddleware('admin'), validateReport);
router.patch('/:id/follow-up',   authMiddleware, roleMiddleware('helpdesk'), addFollowUp);

module.exports = router;
