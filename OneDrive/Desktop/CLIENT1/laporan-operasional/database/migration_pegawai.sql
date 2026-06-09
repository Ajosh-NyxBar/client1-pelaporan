-- ============================================================
--  MIGRATION: Tambah data pegawai (jabatan, no_hp, alamat, email)
--  Jalankan pada database PRODUCTION yang sudah ada datanya.
--  Aman dieksekusi berulang (cek dulu sebelum ALTER).
--
--  Cara pakai:
--    mysql -u root -p db_laporan_ops < migration_pegawai.sql
-- ============================================================

USE db_laporan_ops;

-- 1. Kolom jabatan
SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'jabatan');
SET @sql = IF(@c = 0,
  'ALTER TABLE users ADD COLUMN jabatan VARCHAR(100) NULL AFTER role',
  'SELECT "kolom jabatan sudah ada"');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2. Kolom no_hp
SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'no_hp');
SET @sql = IF(@c = 0,
  'ALTER TABLE users ADD COLUMN no_hp VARCHAR(20) NULL AFTER jabatan',
  'SELECT "kolom no_hp sudah ada"');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 3. Kolom alamat
SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'alamat');
SET @sql = IF(@c = 0,
  'ALTER TABLE users ADD COLUMN alamat VARCHAR(255) NULL AFTER no_hp',
  'SELECT "kolom alamat sudah ada"');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 4. Kolom email
SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'email');
SET @sql = IF(@c = 0,
  'ALTER TABLE users ADD COLUMN email VARCHAR(120) NULL AFTER alamat',
  'SELECT "kolom email sudah ada"');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SELECT 'Migration data pegawai selesai' AS status;
