import { randomInt, randomUUID } from 'node:crypto';
import type { FastifyInstance } from 'fastify';
import nodemailer from 'nodemailer';
import { z } from 'zod';
import type { AppConfig } from '../config.js';
import type { Database } from '../db.js';
import { randomToken, sha256, verifyTelegramPayload } from './crypto.js';
import { createSession, rotateSession } from './session.js';

function escapeHtml(value: string): string {
  return value.replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]!);
}

async function getOrCreateIdentity(
  db: Database,
  provider: 'telegram' | 'email',
  subject: string,
  data: { email?: string; telegramId?: string; metadata?: Record<string, unknown> },
): Promise<string> {
  const existing = await db.query<{ user_id: string }>(
    'SELECT user_id FROM user_identities WHERE provider = $1 AND provider_subject = $2',
    [provider, subject],
  );
  if (existing.rows[0]) return existing.rows[0].user_id;
  const client = await db.connect();
  try {
    await client.query('BEGIN');
    const userId = randomUUID();
    await client.query(
      'INSERT INTO users (id, external_user_id, email) VALUES ($1, $2, $3)',
      [userId, `pulse:${userId}`, data.email ?? null],
    );
    await client.query(
      `INSERT INTO user_identities
       (user_id, provider, provider_subject, telegram_id, metadata)
       VALUES ($1, $2, $3, $4, $5)`,
      [userId, provider, subject, data.telegramId ?? null, data.metadata ?? {}],
    );
    await client.query('COMMIT');
    return userId;
  } catch (error: unknown) {
    await client.query('ROLLBACK');
    const raced = await db.query<{ user_id: string }>(
      'SELECT user_id FROM user_identities WHERE provider = $1 AND provider_subject = $2',
      [provider, subject],
    );
    if (raced.rows[0]) return raced.rows[0].user_id;
    throw error;
  } finally {
    client.release();
  }
}

export function registerAuthRoutes(app: FastifyInstance, db: Database, config: AppConfig): void {
  const mailer = nodemailer.createTransport({
    host: config.SMTP_HOST,
    port: config.SMTP_PORT,
    secure: config.SMTP_SECURE,
    auth: { user: config.SMTP_USER, pass: config.SMTP_PASSWORD },
  });

  app.get('/api/auth/telegram/start', async (_request, reply) => {
    const callback = `${config.PUBLIC_BASE_URL}/api/auth/telegram/callback`;
    const username = escapeHtml(config.TELEGRAM_BOT_USERNAME.replace(/^@/, ''));
    reply.type('text/html; charset=utf-8').send(`<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><title>PulseVPN</title></head><body style="background:#0B0C10;color:#F5F6FA;font-family:sans-serif;display:grid;place-items:center;min-height:100vh"><main style="text-align:center"><h1>PulseVPN</h1><p>Подтвердите вход через Telegram</p><script async src="https://telegram.org/js/telegram-widget.js?22" data-telegram-login="${username}" data-size="large" data-auth-url="${escapeHtml(callback)}" data-request-access="write"></script></main></body></html>`);
  });

  app.get('/api/auth/telegram/callback', async (request, reply) => {
    const query = z.record(z.string(), z.string()).parse(request.query);
    if (!verifyTelegramPayload(query, config.TELEGRAM_BOT_TOKEN)) {
      return reply.code(401).send({ error: 'invalid_telegram_signature' });
    }
    const telegramId = query.id;
    if (!telegramId) return reply.code(422).send({ error: 'telegram_id_missing' });
    const userId = await getOrCreateIdentity(db, 'telegram', telegramId, {
      telegramId,
      metadata: { username: query.username, firstName: query.first_name, lastName: query.last_name },
    });
    const code = randomToken();
    await db.query(
      `INSERT INTO login_challenges (kind, subject, secret_hash, payload, expires_at)
       VALUES ('telegram_exchange', $1, $2, $3, now() + interval '2 minutes')`,
      [telegramId, sha256(code), { userId }],
    );
    return reply.redirect(`${config.APP_DEEP_LINK}?code=${encodeURIComponent(code)}`);
  });

  app.post('/api/auth/exchange', async (request, reply) => {
    const { code } = z.object({ code: z.string().min(20) }).parse(request.body);
    const result = await db.query<{ id: string; payload: { userId: string } }>(
      `UPDATE login_challenges SET consumed_at = now()
       WHERE id = (SELECT id FROM login_challenges
         WHERE kind = 'telegram_exchange' AND secret_hash = $1
           AND consumed_at IS NULL AND expires_at > now()
         ORDER BY created_at DESC LIMIT 1 FOR UPDATE SKIP LOCKED)
       RETURNING id, payload`,
      [sha256(code)],
    );
    const challenge = result.rows[0];
    if (!challenge) return reply.code(401).send({ error: 'invalid_or_expired_code' });
    return createSession(app, db, challenge.payload.userId);
  });

  app.post('/api/auth/email/request', async (request, reply) => {
    const { email } = z.object({ email: z.string().email().transform((value) => value.toLowerCase()) }).parse(request.body);
    const recent = await db.query<{ count: string }>(
      `SELECT count(*) FROM login_challenges
       WHERE kind = 'email_otp' AND subject = $1 AND created_at > now() - interval '1 hour'`,
      [email],
    );
    if (Number(recent.rows[0]?.count ?? 0) >= 5) return reply.code(429).send({ error: 'too_many_codes' });
    const code = randomInt(100_000, 1_000_000).toString();
    await db.query(
      `INSERT INTO login_challenges (kind, subject, secret_hash, expires_at)
       VALUES ('email_otp', $1, $2, now() + interval '10 minutes')`,
      [email, sha256(`${code}:${config.JWT_ACCESS_SECRET}`)],
    );
    await mailer.sendMail({ from: config.SMTP_FROM, to: email, subject: 'Код входа PulseVPN', text: `Код входа: ${code}. Он действует 10 минут.` });
    return reply.code(202).send({ accepted: true });
  });

  app.post('/api/auth/email/verify', async (request, reply) => {
    const body = z.object({ email: z.string().email().transform((value) => value.toLowerCase()), code: z.string().regex(/^\d{6}$/) }).parse(request.body);
    const result = await db.query<{ id: string }>(
      `UPDATE login_challenges SET consumed_at = now(), attempts = attempts + 1
       WHERE id = (SELECT id FROM login_challenges WHERE kind = 'email_otp'
         AND subject = $1 AND secret_hash = $2 AND consumed_at IS NULL
         AND expires_at > now() AND attempts < 5 ORDER BY created_at DESC LIMIT 1)
       RETURNING id`,
      [body.email, sha256(`${body.code}:${config.JWT_ACCESS_SECRET}`)],
    );
    if (!result.rows[0]) return reply.code(401).send({ error: 'invalid_or_expired_code' });
    const userId = await getOrCreateIdentity(db, 'email', body.email, { email: body.email });
    return createSession(app, db, userId);
  });

  app.post('/api/auth/refresh', async (request, reply) => {
    const { refreshToken } = z.object({ refreshToken: z.string().min(32) }).parse(request.body);
    const tokens = await rotateSession(app, db, refreshToken);
    return tokens ?? reply.code(401).send({ error: 'invalid_refresh_token' });
  });
}
