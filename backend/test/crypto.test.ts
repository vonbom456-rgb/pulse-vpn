import { createHash, createHmac } from 'node:crypto';
import { describe, expect, it } from 'vitest';
import { verifyTelegramPayload, verifyWebhookSignature } from '../src/auth/crypto.js';

describe('HEX webhook signature', () => {
  it('accepts only the exact raw body', () => {
    const secret = 'webhook-secret-long-enough';
    const body = Buffer.from('{"event":"subscription.renewed"}');
    const signature = `sha256=${createHmac('sha256', secret).update(body).digest('hex')}`;
    expect(verifyWebhookSignature(body, signature, secret)).toBe(true);
    expect(verifyWebhookSignature(Buffer.from('{}'), signature, secret)).toBe(false);
    expect(verifyWebhookSignature(body, 'sha256=00', secret)).toBe(false);
  });
});

describe('Telegram login signature', () => {
  it('validates hash and rejects stale auth_date', () => {
    const token = '123456:secret';
    const now = 1_800_000_000;
    const payload: Record<string, string> = { id: '42', first_name: 'Pulse', auth_date: String(now) };
    const check = Object.entries(payload).sort(([a], [b]) => a.localeCompare(b)).map(([key, value]) => `${key}=${value}`).join('\n');
    const key = createHash('sha256').update(token).digest();
    payload.hash = createHmac('sha256', key).update(check).digest('hex');
    expect(verifyTelegramPayload(payload, token, now)).toBe(true);
    expect(verifyTelegramPayload(payload, token, now + 601)).toBe(false);
  });
});
