import type { FastifyInstance } from 'fastify';
import type { AppConfig } from '../config.js';
import type { Database } from '../db.js';
import { verifyWebhookSignature } from '../auth/crypto.js';
import { HexClient } from './client.js';
import { subscriptionResponseSchema, webhookPayloadSchema } from './schemas.js';
import { SubscriptionService } from './subscription-service.js';

const knownEvents = new Set([
  'subscription.created',
  'subscription.renewed',
  'subscription.upgraded',
  'subscription.traffic_purchased',
  'subscription.frozen',
  'subscription.unfrozen',
  'subscription.expires_in_72h',
  'subscription.expires_in_48h',
  'subscription.expires_in_24h',
  'subscription.expired',
  'subscription.traffic_threshold',
  'deposit.credited',
]);

export function registerHexWebhook(
  app: FastifyInstance,
  db: Database,
  config: AppConfig,
  hex: HexClient,
  subscriptions: SubscriptionService,
): void {
  app.post('/api/webhooks/hex', async (request, reply) => {
    const rawBody = request.rawBody;
    const signature = request.headers['x-signature'];
    if (!rawBody || !verifyWebhookSignature(rawBody, Array.isArray(signature) ? signature[0] : signature, config.HEX_WEBHOOK_SECRET)) {
      return reply.code(401).send({ error: 'invalid_signature' });
    }
    const parsed = webhookPayloadSchema.safeParse(request.body);
    if (!parsed.success) return reply.code(422).send({ error: 'invalid_webhook' });
    const event = parsed.data;
    if (!knownEvents.has(event.event)) return reply.code(204).send();

    const inserted = await db.query<{ event_id: string }>(
      `INSERT INTO hex_webhook_events (event_id, event_name, payload)
       VALUES ($1, $2, $3) ON CONFLICT (event_id) DO NOTHING RETURNING event_id`,
      [event.event_id, event.event, event],
    );
    if (!inserted.rows[0]) return reply.code(204).send();

    if (event.subscription) {
      const user = await db.query<{ id: string }>(
        'SELECT id FROM users WHERE external_user_id = $1',
        [event.subscription.external_user_id],
      );
      const userId = user.rows[0]?.id;
      if (userId) {
        // Подпись подтверждает отправителя, но актуальное состояние перечитываем из HEX.
        const current = await hex.get(
          `/subscriptions/${encodeURIComponent(event.subscription.subscription_id)}`,
          subscriptionResponseSchema,
        );
        if (current.subscription.external_user_id === event.subscription.external_user_id) {
          await subscriptions.cache(userId, current.subscription);
        }
        await db.query(
          `INSERT INTO notification_outbox (user_id,event_name,payload)
           VALUES ($1,$2,$3) ON CONFLICT DO NOTHING`,
          [userId, event.event, { event_id: event.event_id, occurred_at: event.occurred_at, subscription_id: event.subscription.subscription_id }],
        );
      }
    }
    await db.query('UPDATE hex_webhook_events SET processed_at = now() WHERE event_id = $1', [event.event_id]);
    return reply.code(204).send();
  });
}
