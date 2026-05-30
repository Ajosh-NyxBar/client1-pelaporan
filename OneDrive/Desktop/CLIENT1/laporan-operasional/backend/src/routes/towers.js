const express = require('express');
const router  = express.Router();
const { authMiddleware, roleMiddleware } = require('../middleware/auth');
const {
  getAllTowers, createTower, updateTower, deleteTower,
} = require('../controllers/towerController');

// Semua role yang sudah login bisa GET daftar tower (teknisi butuh untuk dropdown)
router.get('/',         authMiddleware, getAllTowers);

// Admin only: CRUD
router.post('/',        authMiddleware, roleMiddleware('admin'), createTower);
router.put('/:id',      authMiddleware, roleMiddleware('admin'), updateTower);
router.delete('/:id',   authMiddleware, roleMiddleware('admin'), deleteTower);

module.exports = router;
