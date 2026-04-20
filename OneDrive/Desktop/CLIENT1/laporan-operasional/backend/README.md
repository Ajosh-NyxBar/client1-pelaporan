# 🖥️ Backend API — Laporan Operasional

REST API menggunakan **Node.js + Express + MySQL**

## ⚙️ Setup & Instalasi

### 1. Install dependensi
```bash
cd backend
npm install
```

### 2. Konfigurasi environment
```bash
cp .env.example .env
# Edit .env sesuai konfigurasi MySQL Anda
```

### 3. Buat database & tabel
```bash
mysql -u root -p < ../database/schema.sql
```

### 4. Isi data awal (seed)
```bash
npm run seed
```

### 5. Jalankan server
```bash
# Development (auto-reload)
npm run dev

# Production
npm start
```

Server berjalan di: **http://localhost:3000**

---

## 👤 Akun Default (setelah seed)

| Username    | Password     | Role     |
|-------------|--------------|----------|
| teknisi1    | password123  | teknisi  |
| teknisi2    | password123  | teknisi  |
| admin1      | password123  | admin    |
| tl1         | password123  | admin    |
| helpdesk1   | password123  | helpdesk |

---

## 📡 Endpoint API

### Auth
| Method | Endpoint                  | Akses  | Keterangan          |
|--------|---------------------------|--------|---------------------|
| POST   | /api/auth/login           | Public | Login               |
| GET    | /api/auth/profile         | All    | Profil sendiri      |
| PUT    | /api/auth/change-password | All    | Ganti password      |

### Reports
| Method | Endpoint                     | Akses    | Keterangan              |
|--------|------------------------------|----------|-------------------------|
| GET    | /api/reports/stats           | All      | Statistik dashboard     |
| GET    | /api/reports                 | All      | Daftar laporan          |
| GET    | /api/reports/:id             | All      | Detail laporan          |
| POST   | /api/reports                 | Teknisi  | Buat laporan baru       |
| PATCH  | /api/reports/:id/validate    | Admin    | Validasi laporan        |

### Users
| Method | Endpoint   | Akses | Keterangan        |
|--------|------------|-------|-------------------|
| GET    | /api/users | Admin | Daftar semua user |

---

## 🤖 Koneksi dari Android

- **Emulator AVD**: Gunakan `http://10.0.2.2:3000`
- **Perangkat fisik**: Gunakan IP LAN mesin Anda (mis. `http://192.168.1.x:3000`)