import { randomUUID } from 'node:crypto';
import type { FastifyInstance } from 'fastify';
import type { Database } from '../db.js';
import { randomToken, sha256 } from './crypto.js';

export interface SessionTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export async function createSession(app: FastifyInstance, db: Database, userId: string): Promise<SessionTokens> {
  const refreshToken = randomToken(48);
  await db.query(
    `INSERT INTO refresh_sessions (id, user_id, token_hash, expires_at)
     VALUES ($1, $2, $3, now() + interval '30 days')`,
    [randomUUID(), userId, sha256(refreshToken)],
  );
  return {
    accessToken: app.jwt.sign({ sub: userId }, { expiresIn: '15m' }),
    refreshToken,
    expiresIn: 900,
  };
}

export async function rotateSession(
  app: FastifyInstance,
  db: Database,
  refreshToken: string,
): Promise<SessionTokens | null> {
  const client = await db.connect();
  try {
    await client.query('BEGIN');
    const result = await client.query<{ id: string; user_id: string }>(
      `SELECT id, user_id FROM refresh_sessions
       WHERE token_hash = $1 AND revoked_at IS NULL AND expires_at > now()
       FOR UPDATE`,
      [sha256(refreshToken)],
    );
    const session = result.rows[0];
    if (!session) {
      await client.query('ROLLBACK');
      return null;
    }
    await client.query('UPDATE refresh_sessions SET revoked_at = now() WHERE id = $1', [session.id]);
    await client.query('COMMIT');
    return createSession(app, db, session.user_id);
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }
}
