# 📱 Sistem Aplikasi Pelaporan Operasional

Aplikasi Android full-stack untuk manajemen laporan pekerjaan teknisi lapangan, dilengkapi validasi admin dan monitoring helpdesk.

---

## 🏗️ Arsitektur Sistem

```
┌─────────────────────────────────────────────────────┐
│                  ANDROID APP (Kotlin)               │
│                                                     │
│  Login ──► Dashboard (role-based)                   │
│             ├── Teknisi  → Input Laporan            │
│             ├── Admin    → Validasi Laporan         │
│             └── Helpdesk → Monitoring               │
└────────────────────────┬────────────────────────────┘
                         │ HTTP/REST (Retrofit)
                         ▼
┌─────────────────────────────────────────────────────┐
│              BACKEND API (Node.js + Express)        │
│                                                     │
│  /api/auth    → Login, Profile, Change Password     │
│  /api/reports → CRUD Laporan + Validasi + Stats     │
│  /api/users   → Manajemen User                      │
│  /uploads     → Static foto laporan                 │
└────────────────────────┬────────────────────────────┘
                         │ mysql2 (Connection Pool)
                         ▼
┌─────────────────────────────────────────────────────┐
│                DATABASE (MySQL)                     │
│                                                     │
│  users          → Data pengguna + role              │
│  reports        → Data laporan pekerjaan            │
│  report_photos  → Foto pendukung laporan            │
└─────────────────────────────────────────────────────┘
```

---

## 📁 Struktur Proyek

```
laporan-operasional/
│
├── backend/                          # Node.js REST API
│   ├── package.json
│   ├── .env.example
│   ├── README.md
│   └── src/
│       ├── app.js                    # Entry point Express
│       ├── seed.js                   # Seeder akun default
│       ├── config/
│       │   └── database.js           # MySQL connection pool
│       ├── middleware/
│       │   ├── auth.js               # JWT verify + role guard
│       │   └── upload.js             # Multer image handler
│       ├── controllers/
│       │   ├── authController.js     # Login, profile, ganti password
│       │   └── reportController.js   # CRUD laporan + validasi
│       └── routes/
│           ├── auth.js
│           ├── reports.js
│           └── users.js
│
├── database/
│   └── schema.sql                    # DDL lengkap + view
│
└── android/                          # Android App (Kotlin)
    ├── build.gradle
    ├── settings.gradle
    ├── gradle.properties
    └── app/
        ├── build.gradle
        └── src/main/
            ├── AndroidManifest.xml
            ├── java/com/laporan/ops/
            │   ├── api/
            │   │   ├── ApiService.kt         # Retrofit interface
            │   │   └── RetrofitClient.kt     # OkHttp singleton
            │   ├── model/
            │   │   └── Models.kt             # Data classes
            │   ├── utils/
            │   │   └── SessionManager.kt     # SharedPreferences session
            │   ├── adapter/
            │   │   └── LaporanAdapter.kt     # RecyclerView adapter
            │   └── ui/
            │       ├── LoginActivity.kt
            │       ├── teknisi/
            │       │   ├── TeknisiDashboardActivity.kt
            │       │   └── TambahLaporanActivity.kt
            │       ├── admin/
            │       │   ├── AdminDashboardActivity.kt
            │       │   └── DetailLaporanActivity.kt
            │       └── helpdesk/
            │           └── HelpdeskDashboardActivity.kt
            └── res/
                ├── layout/           # 6 activity layouts + item
                ├── values/           # colors, strings, themes
                ├── drawable/         # vector icons + backgrounds
                └── xml/              # file_paths (FileProvider)
```

---

## ⚙️ Setup Backend

### Prasyarat
- Node.js v18+
- MySQL 8.0+
- npm

### Langkah Instalasi

**1. Masuk ke direktori backend**
```bash
cd laporan-operasional/backend
```

**2. Install dependensi**
```bash
npm install
```

**3. Buat file environment**
```bash
cp .env.example .env
```

**4. Edit file `.env` sesuai konfigurasi MySQL Anda**
```env
NODE_ENV=development
PORT=3000
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=your_password_here
DB_NAME=db_laporan_ops
JWT_SECRET=ganti_dengan_string_acak_yang_panjang
JWT_EXPIRES_IN=7d
```

**5. Buat database dan tabel**
```bash
mysql -u root -p < ../database/schema.sql
```

**6. Isi data awal (seed)**
```bash
npm run seed
```

**7. Jalankan server**
```bash
# Development (auto-reload)
npm run dev

# Production
npm start
```

Server akan berjalan di: **http://localhost:3000**

---

## 📱 Setup Android

### Prasyarat
- Android Studio Hedgehog (2023.1.1) atau lebih baru
- JDK 17
- Android SDK 34
- Emulator AVD atau perangkat fisik Android 7.0+ (API 24)

### Langkah Instalasi

**1. Buka proyek di Android Studio**
```
File → Open → pilih folder: laporan-operasional/android
```

**2. Sesuaikan BASE_URL di `RetrofitClient.kt`**

```kotlin
// Untuk Emulator AVD (default)
private const val BASE_URL = "http://10.0.2.2:3000/api/"

// Untuk perangkat fisik (ganti dengan IP LAN mesin Anda)
private const val BASE_URL = "http://192.168.1.xxx:3000/api/"
```

Cari IP LAN Anda:
```bash
# Windows
ipconfig

# macOS / Linux
ifconfig | grep inet
```

**3. Sync Gradle**
```
File → Sync Project with Gradle Files
```

**4. Build & Run**
```
Run → Run 'app'  (Shift+F10)
```

---

## 👥 Akun Default (setelah seed)

| Username    | Password     | Role             | Dashboard                    |
|-------------|--------------|------------------|------------------------------|
| `teknisi1`  | `password123`| Teknisi          | Input & lihat laporan sendiri|
| `teknisi2`  | `password123`| Teknisi          | Input & lihat laporan sendiri|
| `admin1`    | `password123`| Admin            | Validasi semua laporan       |
| `tl1`       | `password123`| Admin (TL)       | Validasi semua laporan       |
| `helpdesk1` | `password123`| Helpdesk         | Monitor laporan disetujui    |

---

## 🔄 Alur Sistem

```
1. TEKNISI
   ├── Login → Dashboard Teknisi
   ├── Lihat statistik laporan milik sendiri
   ├── Klik FAB (+) → Isi form laporan
   │   ├── Jenis pekerjaan
   │   ├── Lokasi
   │   ├── Tanggal & waktu
   │   ├── Deskripsi
   │   └── Upload foto (maks. 5)
   └── Kirim → Status: "Menunggu Validasi"

2. ADMIN / TEAM LEADER
   ├── Login → Dashboard Admin
   ├── Lihat semua laporan (filter by status)
   ├── Klik laporan → Detail
   └── Validasi:
       ├── ✅ Setujui → Status: "Disetujui"
       └── ❌ Tolak  → Status: "Ditolak" + catatan wajib

3. HELPDESK
   ├── Login → Dashboard Helpdesk
   └── Monitor laporan yang sudah "Disetujui"
       └── Gunakan untuk tindak lanjut / analisis
```

---

## 📡 API Endpoints

### Auth
| Method | Endpoint                    | Auth | Role  | Deskripsi             |
|--------|-----------------------------|------|-------|-----------------------|
| POST   | `/api/auth/login`           | ✗    | –     | Login, dapat token    |
| GET    | `/api/auth/profile`         | ✓    | All   | Profil sendiri        |
| PUT    | `/api/auth/change-password` | ✓    | All   | Ganti password        |

### Reports
| Method | Endpoint                       | Auth | Role       | Deskripsi                |
|--------|--------------------------------|------|------------|--------------------------|
| GET    | `/api/reports/stats`           | ✓    | All        | Statistik dashboard      |
| GET    | `/api/reports`                 | ✓    | All        | Daftar laporan (filter)  |
| GET    | `/api/reports/:id`             | ✓    | All        | Detail laporan           |
| POST   | `/api/reports`                 | ✓    | Teknisi    | Buat laporan baru        |
| PATCH  | `/api/reports/:id/validate`    | ✓    | Admin      | Approve / Reject         |

### Users
| Method | Endpoint     | Auth | Role  | Deskripsi           |
|--------|--------------|------|-------|---------------------|
| GET    | `/api/users` | ✓    | Admin | Daftar semua user   |

### Contoh Request

**Login**
```bash
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "teknisi1", "password": "password123"}'
```

**Buat Laporan**
```bash
curl -X POST http://localhost:3000/api/reports \
  -H "Authorization: Bearer <TOKEN>" \
  -F "jenis_pekerjaan=Perbaikan Jaringan" \
  -F "lokasi=Gedung A Lt.3" \
  -F "waktu_kerja=2024-01-15 09:00:00" \
  -F "deskripsi=Mengganti kabel UTP yang putus" \
  -F "photos=@/path/to/photo.jpg"
```

**Validasi Laporan**
```bash
curl -X PATCH http://localhost:3000/api/reports/1/validate \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"action": "approve", "catatan": null}'
```

---

## 🗃️ Skema Database

```sql
users
  ├── id, username (UNIQUE), password (bcrypt)
  ├── name, role (teknisi|admin|helpdesk)
  └── is_active, created_at, updated_at

reports
  ├── id, report_code (UNIQUE, format: RPT-YYYYMMDD-XXXX)
  ├── teknisi_id (FK → users)
  ├── jenis_pekerjaan, lokasi, waktu_kerja, deskripsi
  ├── status (menunggu|disetujui|ditolak)
  ├── validated_by (FK → users), validated_at, catatan_validasi
  └── created_at, updated_at

report_photos
  ├── id, report_id (FK → reports)
  └── photo_path, created_at

VIEW: v_reports_detail
  └── JOIN reports + users (teknisi) + users (validator)
```

---

## 🔒 Keamanan

| Fitur                   | Implementasi                                       |
|-------------------------|----------------------------------------------------|
| Autentikasi             | JWT (JSON Web Token), expire 7 hari               |
| Password                | Bcrypt hash (salt rounds: 10)                      |
| Otorisasi role          | Middleware `roleMiddleware(...roles)`              |
| Upload file             | Validasi tipe MIME + limit ukuran 5MB             |
| Cleartext HTTP (dev)    | `android:usesCleartextTraffic="true"` di Manifest |

> ⚠️ **Produksi**: Gunakan HTTPS, ganti `usesCleartextTraffic` dengan SSL certificate, dan simpan `JWT_SECRET` di environment yang aman.

---

## 🛠️ Teknologi Stack

### Backend
| Teknologi    | Versi   | Kegunaan                    |
|--------------|---------|-----------------------------|
| Node.js      | 18+     | Runtime JavaScript          |
| Express      | 4.18    | Web framework               |
| mysql2       | 3.6     | Driver MySQL (promise-based)|
| jsonwebtoken | 9.0     | JWT generate & verify       |
| bcryptjs     | 2.4     | Hash password               |
| multer       | 1.4     | Upload file/foto            |
| dotenv       | 16.3    | Environment variables       |
| cors         | 2.8     | Cross-Origin Resource Sharing|
| nodemon      | 3.0     | Auto-reload (development)   |

### Android
| Teknologi          | Versi  | Kegunaan                        |
|--------------------|--------|---------------------------------|
| Kotlin             | 1.9    | Bahasa pemrograman              |
| Android SDK        | API 34 | Target platform                 |
| Retrofit           | 2.9    | HTTP client (type-safe)         |
| OkHttp             | 4.12   | HTTP engine + logging           |
| Gson Converter     | 2.9    | JSON serialization              |
| Glide              | 4.16   | Image loading                   |
| Coroutines         | 1.7    | Async/concurrent operations     |
| Material Components| 1.10   | UI components                   |
| ViewBinding        | –      | Type-safe view access           |
| SwipeRefreshLayout | 1.1    | Pull-to-refresh                 |

---

## 🐛 Troubleshooting

### Backend

**Database gagal terkoneksi**
```bash
# Periksa MySQL berjalan
sudo systemctl status mysql   # Linux
net start mysql               # Windows

# Periksa kredensial di .env
DB_HOST, DB_USER, DB_PASSWORD, DB_NAME
```

**Port 3000 sudah digunakan**
```bash
# Windows
netstat -ano | findstr :3000
taskkill /PID <PID> /F

# macOS/Linux
lsof -ti:3000 | xargs kill
```

### Android

**Tidak bisa konek ke server (emulator)**
```
Pastikan BASE_URL = "http://10.0.2.2:3000/api/"
10.0.2.2 adalah alias loopback ke mesin host dari AVD
```

**Tidak bisa konek ke server (perangkat fisik)**
```
1. Perangkat dan komputer harus di jaringan WiFi yang sama
2. Ganti BASE_URL ke IP LAN komputer Anda
   Contoh: "http://192.168.1.15:3000/api/"
3. Pastikan firewall tidak memblokir port 3000
```

**Build error: "Unresolved reference"**
```
1. File → Sync Project with Gradle Files
2. Build → Clean Project
3. Build → Rebuild Project
```

**ViewBinding tidak ditemukan**
```
Pastikan di app/build.gradle:
buildFeatures {
    viewBinding true
}
```

---

## 🚀 Roadmap Pengembangan

- [ ] Notifikasi push (Firebase FCM) saat laporan divalidasi
- [ ] Export laporan ke PDF
- [ ] Dashboard analytics dengan grafik
- [ ] Filter laporan by tanggal range
- [ ] Multi-bahasa (i18n)
- [ ] Dark mode
- [ ] Offline mode dengan Room database
- [ ] Tanda tangan digital pada laporan

---

## 📄 Lisensi

MIT License — bebas digunakan dan dimodifikasi untuk kebutuhan internal perusahaan.

---

> Dibuat dengan ❤️ untuk efisiensi pelaporan operasional lapangan.