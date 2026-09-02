import { createHash, createHmac, randomBytes, timingSafeEqual } from 'node:crypto';

export function sha256(value: string): string {
  return createHash('sha256').update(value).digest('hex');
}

export function randomToken(bytes = 32): string {
  return randomBytes(bytes).toString('base64url');
}

export function verifyTelegramPayload(
  payload: Record<string, string>,
  botToken: string,
  nowSeconds = Math.floor(Date.now() / 1000),
): boolean {
  const receivedHash = payload.hash;
  const authDate = Number(payload.auth_date);
  if (!receivedHash || !Number.isInteger(authDate) || Math.abs(nowSeconds - authDate) > 600) {
    return false;
  }
  const checkString = Object.entries(payload)
    .filter(([key]) => key !== 'hash')
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([key, value]) => `${key}=${value}`)
    .join('\n');
  const secret = createHash('sha256').update(botToken).digest();
  const expected = createHmac('sha256', secret).update(checkString).digest();
  const received = Buffer.from(receivedHash, 'hex');
  return received.length === expected.length && timingSafeEqual(received, expected);
}

export function verifyWebhookSignature(rawBody: Buffer, signature: string | undefined, secret: string): boolean {
  if (!signature?.startsWith('sha256=')) return false;
  const received = Buffer.from(signature.slice(7), 'hex');
  const expected = createHmac('sha256', secret).update(rawBody).digest();
  return received.length === expected.length && timingSafeEqual(received, expected);
}
