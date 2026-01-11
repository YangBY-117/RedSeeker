#!/usr/bin/env node
const sqlite3 = require('sqlite3').verbose();
const bcrypt = require('bcryptjs');

function parseArgs() {
  const args = process.argv.slice(2);
  const opts = { db: 'red_tourism.db', count: 10, password: 'password123' };
  for (let i = 0; i < args.length; i++) {
    const a = args[i];
    if (a === '--db' && args[i+1]) { opts.db = args[++i]; }
    else if (a === '--count' && args[i+1]) { opts.count = parseInt(args[++i], 10); }
    else if (a === '--password' && args[i+1]) { opts.password = args[++i]; }
  }
  return opts;
}

const opts = parseArgs();
const db = new sqlite3.Database(opts.db);

db.serialize(() => {
  db.run("PRAGMA foreign_keys = ON;");

  // Ensure users table exists (migration should create it)
  db.run(`CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT (datetime('now')),
    last_login DATETIME
  );`);

  const insertStmt = db.prepare('INSERT OR IGNORE INTO users (username, password) VALUES (?, ?)');

  for (let i = 1; i <= opts.count; i++) {
    const username = `user${i}`;
    const hash = bcrypt.hashSync(opts.password, 10);
    insertStmt.run(username, hash, function(err) {
      if (err) console.error('Insert error', username, err.message);
    });
  }

  insertStmt.finalize(err => {
    if (err) console.error('Finalize error', err.message);
    db.get('SELECT COUNT(*) as c FROM users', (e, row) => {
      if (e) console.error(e.message);
      else console.log(`Inserted/Existing users count: ${row.c}`);
      db.close();
    });
  });
});
