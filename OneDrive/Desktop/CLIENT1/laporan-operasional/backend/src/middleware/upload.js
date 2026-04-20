const multer = require('multer');
const path = require('path');
const fs = require('fs');

// Pastikan folder uploads tersedia
const uploadDir = path.join(__dirname, '../../uploads');
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadDir),
  filename: (req, file, cb) => {
    const uniqueSuffix = `${Date.now()}-${Math.round(Math.random() * 1e9)}`;
    cb(null, `photo-${uniqueSuffix}${path.extname(file.originalname) || '.jpg'}`);
  },
});

const fileFilter = (req, file, cb) => {
  // Izinkan berdasarkan MIME type
  const allowedMimes = [
    'image/jpeg',
    'image/jpg',
    'image/png',
    'image/gif',
    'image/webp',
    'image/*',                     // Android sering kirim MIME generic
    'application/octet-stream',    // Fallback dari beberapa device/client
  ];

  // Izinkan juga berdasarkan ekstensi file
  const allowedExts = ['.jpg', '.jpeg', '.png', '.gif', '.webp'];
  const ext = path.extname(file.originalname).toLowerCase();

  if (allowedMimes.includes(file.mimetype) || allowedExts.includes(ext)) {
    cb(null, true);
  } else {
    cb(new Error(`File tidak diizinkan: ${file.mimetype} (${file.originalname}). Hanya file gambar yang diizinkan (JPEG, PNG, GIF, WEBP).`), false);
  }
};

const upload = multer({
  storage,
  fileFilter,
  limits: { fileSize: parseInt(process.env.MAX_FILE_SIZE) || 5 * 1024 * 1024 },
});

module.exports = upload;
