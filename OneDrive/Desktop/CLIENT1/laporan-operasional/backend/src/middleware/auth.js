const jwt = require('jsonwebtoken');

/**
 * Middleware: verifikasi JWT token
 */
const authMiddleware = (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        success: false,
        message: 'Token tidak ditemukan. Silakan login terlebih dahulu.',
      });
    }

    const token = authHeader.split(' ')[1];
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    req.user = decoded;
    next();
  } catch (error) {
    if (error.name === 'TokenExpiredError') {
      return res.status(401).json({
        success: false,
        message: 'Token sudah kadaluarsa. Silakan login kembali.',
      });
    }
    return res.status(401).json({
      success: false,
      message: 'Token tidak valid.',
    });
  }
};

/**
 * Middleware: cek role pengguna
 */
const roleMiddleware = (...roles) => {
  return (req, res, next) => {
    if (!roles.includes(req.user.role)) {
      return res.status(403).json({
        success: false,
        message: `Akses ditolak. Hanya untuk role: ${roles.join(', ')}`,
      });
    }
    next();
  };
};

module.exports = { authMiddleware, roleMiddleware };
