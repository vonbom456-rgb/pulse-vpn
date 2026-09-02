import { readdir, readFile } from 'node:fs/promises';
import { join } from 'node:path';
import { loadConfig } from './config.js';
import { createDatabase } from './db.js';

const config = loadConfig();
const db = createDatabase(config.DATABASE_URL);
const directory = join(process.cwd(), 'migrations');

await db.query(`CREATE TABLE IF NOT EXISTS schema_migrations (
  filename text PRIMARY KEY,
  applied_at timestamptz NOT NULL DEFAULT now()
)`);

for (const filename of (await readdir(directory)).filter((name) => name.endsWith('.sql')).sort()) {
  const applied = await db.query('SELECT 1 FROM schema_migrations WHERE filename = $1', [filename]);
  if (applied.rows[0]) continue;
  const sql = await readFile(join(directory, filename), 'utf8');
  const client = await db.connect();
  try {
    await client.query('BEGIN');
    await client.query(sql);
    await client.query('INSERT INTO schema_migrations (filename) VALUES ($1)', [filename]);
    await client.query('COMMIT');
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }
}

await db.end();
