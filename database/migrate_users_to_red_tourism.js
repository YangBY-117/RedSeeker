#!/usr/bin/env node
const fs = require('fs');
const sqlite3 = require('sqlite3').verbose();
const path = require('path');

function usage() {
  console.log('Usage: node migrate_users_to_red_tourism.js --from <path/to/users.db> --to <path/to/red_tourism.db>');
}

function parseArgs() {
  const args = process.argv.slice(2);
  const opts = { from: 'users.db', to: 'red_tourism.db' };
  for (let i = 0; i < args.length; i++) {
    if ((args[i] === '--from' || args[i] === '-f') && args[i+1]) { opts.from = args[++i]; }
    else if ((args[i] === '--to' || args[i] === '-t') && args[i+1]) { opts.to = args[++i]; }
    else if (args[i] === '--help' || args[i] === '-h') { usage(); process.exit(0); }
  }
  return opts;
}

async function run() {
  const opts = parseArgs();
  const fromPath = path.resolve(opts.from);
  const toPath = path.resolve(opts.to);

  if (!fs.existsSync(fromPath)) {
    console.error(`[migrate] source DB not found: ${fromPath}`);
    process.exit(1);
  }

  if (!fs.existsSync(toPath)) {
    console.log(`[migrate] target DB not found, creating: ${toPath}`);
  }

  const db = new sqlite3.Database(toPath);

  db.serialize(() => {
    db.run('PRAGMA foreign_keys = ON;');

    // Ensure target has users/ratings tables by running migration SQL if needed
    const migrationsSql = fs.readFileSync(path.join(__dirname, 'migrations', '002_users_and_ratings.sql'), 'utf8');
    try {
      db.exec(migrationsSql);
      console.log('[migrate] Applied migrations to target DB');
    } catch (e) {
      console.warn('[migrate] Warning applying migrations (may already exist):', e.message);
    }

    // Attach source DB
    db.run(`ATTACH DATABASE ? AS fromdb`, [fromPath], function(err) {
      if (err) {
        console.error('[migrate] Failed to attach source DB:', err.message);
        process.exit(1);
      }

      db.run('BEGIN TRANSACTION');

      // Copy users
      db.run(`INSERT OR IGNORE INTO users (id, username, password, created_at, last_login)
              SELECT id, username, password, created_at, last_login FROM fromdb.users`, function(err) {
        if (err) console.error('[migrate] Copy users error:', err.message);
        else console.log('[migrate] users copied');
      });

      // Copy user_browse_history
      db.run(`INSERT OR IGNORE INTO user_browse_history (id, user_id, attraction_id, browse_time)
              SELECT id, user_id, attraction_id, browse_time FROM fromdb.user_browse_history`, function(err) {
        if (err) console.error('[migrate] Copy user_browse_history error:', err.message);
        else console.log('[migrate] user_browse_history copied');
      });

      // Copy attraction_ratings
      db.run(`INSERT OR IGNORE INTO attraction_ratings (id, attraction_id, user_id, rating, comment, created_at)
              SELECT id, attraction_id, user_id, rating, comment, created_at FROM fromdb.attraction_ratings`, function(err) {
        if (err) console.error('[migrate] Copy attraction_ratings error:', err.message);
        else console.log('[migrate] attraction_ratings copied');
      });

      db.run('COMMIT', function(err) {
        if (err) console.error('[migrate] Commit error:', err.message);
        else console.log('[migrate] Migration transaction committed');

        // Detach and close
        db.run(`DETACH DATABASE fromdb`, function(err) {
          if (err) console.error('[migrate] Detach error:', err.message);
          db.close();
          console.log('[migrate] Done. You may now remove the source DB file if desired.');
        });
      });
    });
  });
}

run().catch(err => { console.error(err); process.exit(1); });
