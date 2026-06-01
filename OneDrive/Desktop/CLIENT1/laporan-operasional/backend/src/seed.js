const bcrypt = require('bcryptjs');
const db = require('./config/database');

const users = [
  { username: 'teknisi1',  password: 'password123', name: 'Budi Santoso',    role: 'teknisi'   },
  { username: 'teknisi2',  password: 'password123', name: 'Andi Wijaya',     role: 'teknisi'   },
  { username: 'admin1',    password: 'password123', name: 'Admin Pusat',     role: 'admin'     },
  { username: 'tl1',       password: 'password123', name: 'Team Leader',     role: 'admin'     },
  { username: 'helpdesk1', password: 'password123', name: 'Helpdesk Monitor',role: 'helpdesk'  },
];

const towers = [
  { nama: 'Tower BTS Reremi',    alamat: null },
  { nama: 'Tower BTS Sanggeng',  alamat: null },
  { nama: 'Tower BTS Padarni',   alamat: null },
  { nama: 'Tower BTS Amban',     alamat: null },
  { nama: 'Tower BTS Wosi',      alamat: null },
  { nama: 'Tower BTS Susweni',   alamat: null },
  { nama: 'Tower BTS Ayambori',  alamat: null },
  { nama: 'Tower BTS Sowi',      alamat: null },
];

async function seed() {
  console.log('🌱 Memulai seeding database...');

  for (const u of users) {
    const hash = await bcrypt.hash(u.password, 10);
    await db.execute(
      'INSERT IGNORE INTO users (username, password, name, role) VALUES (?, ?, ?, ?)',
      [u.username, hash, u.name, u.role]
    );
    console.log(`  ✅ User: ${u.username} [${u.role}]`);
  }

  for (const t of towers) {
    await db.execute(
      'INSERT IGNORE INTO towers (nama, alamat) VALUES (?, ?)',
      [t.nama, t.alamat]
    );
    console.log(`  🗼 Tower: ${t.nama}`);
  }

  console.log('🎉 Seeding selesai!');
  process.exit(0);
}

seed().catch(err => {
  console.error('❌ Seeding gagal:', err);
  process.exit(1);
});
