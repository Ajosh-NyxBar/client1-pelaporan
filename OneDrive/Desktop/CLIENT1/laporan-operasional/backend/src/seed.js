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
  { nama: 'Tower Telkom Makassar Pusat', alamat: 'Jl. A.P. Pettarani, Makassar' },
  { nama: 'Tower Telkom Panakkukang',    alamat: 'Jl. Boulevard, Panakkukang, Makassar' },
  { nama: 'Tower Telkom Tamalanrea',     alamat: 'Jl. Perintis Kemerdekaan, Tamalanrea' },
  { nama: 'Tower Telkom Antang',         alamat: 'Jl. Antang Raya, Manggala, Makassar' },
  { nama: 'Tower Telkom Daya',           alamat: 'Jl. Kapasa Raya, Biringkanaya, Makassar' },
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
