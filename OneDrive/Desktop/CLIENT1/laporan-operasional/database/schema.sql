-- ============================================================
--  DATABASE: db_laporan_ops
--  Sistem Pelaporan Operasional
--  Version: 1.0.0
-- ============================================================

CREATE DATABASE IF NOT EXISTS db_laporan_ops
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE db_laporan_ops;

-- ── TABEL USERS ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  UNIQUE NOT NULL     COMMENT 'Username login',
    password    VARCHAR(255) NOT NULL             COMMENT 'Bcrypt hash',
    name        VARCHAR(100) NOT NULL             COMMENT 'Nama lengkap',
    role        ENUM('teknisi','admin','helpdesk') NOT NULL DEFAULT 'teknisi',
    is_active   TINYINT(1) NOT NULL DEFAULT 1,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_role     (role)
) ENGINE=InnoDB COMMENT='Data pengguna sistem';

-- ── TABEL REPORTS ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reports (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    report_code      VARCHAR(20) UNIQUE NOT NULL  COMMENT 'Kode unik laporan, mis: RPT-20240101-1234',
    teknisi_id       INT NOT NULL,
    jenis_pekerjaan  VARCHAR(100) NOT NULL,
    lokasi           VARCHAR(255) NOT NULL,
    waktu_kerja      DATETIME NOT NULL,
    deskripsi        TEXT NOT NULL,
    status           ENUM('menunggu','disetujui','ditolak') NOT NULL DEFAULT 'menunggu',
    validated_by     INT NULL,
    validated_at     TIMESTAMP NULL,
    catatan_validasi TEXT NULL,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (teknisi_id)  REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (validated_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_teknisi  (teknisi_id),
    INDEX idx_status   (status),
    INDEX idx_created  (created_at)
) ENGINE=InnoDB COMMENT='Laporan pekerjaan teknisi';

-- ── TABEL FOTO LAPORAN ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS report_photos (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    report_id   INT NOT NULL,
    photo_path  VARCHAR(500) NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE,
    INDEX idx_report (report_id)
) ENGINE=InnoDB COMMENT='Foto pendukung laporan';

-- ── KOLOM TINDAK LANJUT (UC-17) ───────────────────────────────
-- Jalankan secara manual jika database sudah ada:
-- ALTER TABLE reports ADD COLUMN tindak_lanjut TEXT NULL AFTER catatan_validasi;
-- ALTER TABLE reports ADD COLUMN tindak_lanjut_by INT NULL AFTER tindak_lanjut;
-- ALTER TABLE reports ADD COLUMN tindak_lanjut_at TIMESTAMP NULL AFTER tindak_lanjut_by;

-- ── VIEW: Detail Laporan ──────────────────────────────────────
CREATE OR REPLACE VIEW v_reports_detail AS
SELECT
    r.id, r.report_code, r.jenis_pekerjaan, r.lokasi, r.waktu_kerja,
    r.deskripsi, r.status, r.catatan_validasi,
    r.tindak_lanjut, r.tindak_lanjut_at,
    r.created_at, r.validated_at,
    u.name      AS teknisi_name,
    u.username  AS teknisi_username,
    v.name      AS validator_name,
    h.name      AS helpdesk_name
FROM reports r
JOIN  users u ON r.teknisi_id        = u.id
LEFT JOIN users v ON r.validated_by      = v.id
LEFT JOIN users h ON r.tindak_lanjut_by  = h.id;
