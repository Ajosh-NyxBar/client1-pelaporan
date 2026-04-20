# Fix Login Issue - Local Backend Setup

## ✅ Current Status
- [x] Backend code siap (Express + MySQL + JWT)
- [x] Android API config ditemukan
- [x] PC IP: `192.168.1.13`

## 📋 Steps to Complete

### 1. Database Setup
- [x] Schema.sql verified ✓
- [ ] Start MySQL
- [ ] `CREATE DATABASE db_laporan_ops;`
- [ ] `mysql -u root -p db_laporan_ops < database/schema.sql`

### 2. Backend Setup
- [x] `cd backend`
- [ ] `npm run seed` (buat test users: teknisi1/password123) 
- [x] `npm run dev` (start :3000) ✅ RUNNING!

### 3. Android Update
- [ ] Update BASE_URL → `http://192.168.1.13:3000/api/`
- [ ] `./gradlew assembleDebug` or Android Studio Build APK
- [ ] Install APK ke HP (same WiFi network)

### 4. Test
- [ ] Backend health: http://192.168.1.13:3000 (HP browser)
- [ ] `npm run seed` jika belum (buat users)
- [ ] Login HP app: `teknisi1` / `password123`

**Test Credentials:**
```
teknisi1 / password123 (teknisi)
admin1   / password123 (admin)
helpdesk1 / password123 (helpdesk)
