-- ============================================================
--  MIGRATION: Tambah fitur Kategori Tower (Lokasi)
--  Jalankan pada database PRODUCTION yang sudah ada datanya.
--  Aman dieksekusi berulang (pakai IF NOT EXISTS / cek dulu).
--
--  Cara pakai:
--    mysql -u root -p db_laporan_ops < migration_towers.sql
-- ============================================================

USE db_laporan_ops;

-- 1. Tabel towers
CREATE TABLE IF NOT EXISTS towers (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nama        VARCHAR(150) NOT NULL UNIQUE,
    alamat      VARCHAR(255) NULL,
    is_active   TINYINT(1) NOT NULL DEFAULT 1,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_nama   (nama),
    INDEX idx_active (is_active)
) ENGINE=InnoDB COMMENT='Daftar tower (kategori lokasi laporan)';

-- 2. Pastikan kolom tindak_lanjut* ada (UC-17). Jika belum, tambahkan.
SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reports' AND COLUMN_NAME = 'tindak_lanjut');
SET @sql = IF(@c = 0,
  'ALTER TABLE reports ADD COLUMN tindak_lanjut TEXT NULL AFTER catatan_validasi',
  'SELECT "kolom tindak_lanjut sudah ada"');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reports' AND COLUMN_NAME = 'tindak_lanjut_by');
SET @sql = IF(@c = 0,
  'ALTER TABLE reports ADD COLUMN tindak_lanjut_by INT NULL AFTER tindak_lanjut',
  'SELECT "kolom tindak_lanjut_by sudah ada"');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reports' AND COLUMN_NAME = 'tindak_lanjut_at');
SET @sql = IF(@c = 0,
  'ALTER TABLE reports ADD COLUMN tindak_lanjut_at TIMESTAMP NULL AFTER tindak_lanjut_by',
  'SELECT "kolom tindak_lanjut_at sudah ada"');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 3. Tambah kolom tower_id di reports (cek dulu kolom belum ada)
SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reports' AND COLUMN_NAME = 'tower_id');
SET @sql = IF(@c = 0,
  'ALTER TABLE reports ADD COLUMN tower_id INT NULL AFTER lokasi',
  'SELECT "kolom tower_id sudah ada"');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 4. Index tower_id
SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reports' AND INDEX_NAME = 'idx_tower');
SET @sql = IF(@c = 0,
  'ALTER TABLE reports ADD INDEX idx_tower (tower_id)',
  'SELECT "index idx_tower sudah ada"');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 5. Foreign key tower_id → towers.id (ON DELETE SET NULL)
SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reports'
            AND CONSTRAINT_NAME = 'fk_reports_tower');
SET @sql = IF(@c = 0,
  'ALTER TABLE reports
     ADD CONSTRAINT fk_reports_tower
     FOREIGN KEY (tower_id) REFERENCES towers(id) ON DELETE SET NULL',
  'SELECT "FK fk_reports_tower sudah ada"');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 6. Update view v_reports_detail
CREATE OR REPLACE VIEW v_reports_detail AS
SELECT
    r.id, r.report_code, r.jenis_pekerjaan, r.lokasi, r.waktu_kerja,
    r.deskripsi, r.status, r.catatan_validasi,
    r.tindak_lanjut, r.tindak_lanjut_at,
    r.created_at, r.validated_at,
    r.tower_id,
    t.nama      AS tower_nama,
    t.alamat    AS tower_alamat,
    u.name      AS teknisi_name,
    u.username  AS teknisi_username,
    v.name      AS validator_name,
    h.name      AS helpdesk_name
FROM reports r
JOIN  users u ON r.teknisi_id        = u.id
LEFT JOIN users  v ON r.validated_by      = v.id
LEFT JOIN users  h ON r.tindak_lanjut_by  = h.id
LEFT JOIN towers t ON r.tower_id          = t.id;

-- 7. Seed tower default. INSERT IGNORE: aman jika sudah ada.
INSERT IGNORE INTO towers (nama) VALUES
  ('Tower BTS Reremi'),
  ('Tower BTS Sanggeng'),
  ('Tower BTS Padarni'),
  ('Tower BTS Amban'),
  ('Tower BTS Wosi'),
  ('Tower BTS Susweni'),
  ('Tower BTS Ayambori'),
  ('Tower BTS Sowi');

SELECT 'Migration towers selesai ✅' AS status;
