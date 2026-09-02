import { buildApp } from './app.js';
import { loadConfig } from './config.js';
import { createDatabase } from './db.js';

const config = loadConfig();
const db = createDatabase(config.DATABASE_URL);
const app = await buildApp(config, db);

const shutdown = async () => {
  await app.close();
  await db.end();
};
process.on('SIGINT', () => void shutdown());
process.on('SIGTERM', () => void shutdown());

await app.listen({ host: '0.0.0.0', port: config.PORT });
