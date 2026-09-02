import { randomUUID } from 'node:crypto';
import { z } from 'zod';
import type { Database } from '../db.js';
import { inTransaction } from '../db.js';
import { HexClient, HexApiError } from './client.js';
import {
  devicesResponseSchema,
  mutationResponseSchema,
  plansResponseSchema,
  subscriptionResponseSchema,
  subscriptionsResponseSchema,
  upgradeQuoteSchema,
  type HexSubscription,
} from './schemas.js';

const deleteDeviceResponseSchema = z.object({
  success: z.literal(true),
  subscription_id: z.string(),
  device_id: z.number().int(),
  blocked: z.boolean(),
});

interface UserLink {
  userId: string;
  externalUserId: string;
  telegramId: string | null;
}

export class SubscriptionService {
  constructor(
    private readonly db: Database,
    private readonly hex: HexClient,
    private readonly hexBotId?: string,
  ) {}

  async plans() {
    return this.hex.get('/plans', plansResponseSchema);
  }

  async current(userId: string) {
    const link = await this.userLink(userId);
    const query = new URLSearchParams({ external_user_id: link.externalUserId, limit: '100' });
    const list = await this.hex.get(`/subscriptions?${query.toString()}`, subscriptionsResponseSchema);
    const selected = [...list.items].sort((a, b) => this.statusRank(a.status) - this.statusRank(b.status))[0];
    if (!selected) return null;
    const detail = await this.hex.get(`/subscriptions/${encodeURIComponent(selected.subscription_id)}`, subscriptionResponseSchema);
    await this.cache(userId, detail.subscription);
    const devices = await this.hex.get(
      `/subscriptions/${encodeURIComponent(selected.subscription_id)}/devices`,
      devicesResponseSchema,
    );
    return this.safeSubscription(detail.subscription, devices.devices.length);
  }

  async devices(userId: string) {
    const subscription = await this.ownedSubscription(userId);
    const response = await this.hex.get(
      `/subscriptions/${encodeURIComponent(subscription.subscription_id)}/devices`,
      devicesResponseSchema,
    );
    return response.devices.map((device) => ({
      id: device.id,
      name: device.name,
      os: device.os,
      model: device.model,
      osVersion: device.os_version,
      firstSeen: device.first_seen,
      lastSeen: device.last_seen,
    }));
  }

  async removeDevice(userId: string, deviceId: number) {
    const subscription = await this.ownedSubscription(userId);
    return this.hex.delete(
      `/subscriptions/${encodeURIComponent(subscription.subscription_id)}/devices/${deviceId}`,
      deleteDeviceResponseSchema,
    );
  }

  async configPayload(userId: string): Promise<string> {
    const subscription = await this.ownedSubscription(userId);
    if (subscription.status !== 'active') throw new HexApiError(409, 'subscription_not_active');
    return this.hex.fetchSubscriptionPayload(subscription.subscription_url);
  }

  async createRenewQuote(userId: string) {
    const subscription = await this.ownedSubscription(userId);
    if (subscription.status === 'frozen') throw new HexApiError(409, 'subscription_frozen');
    if (subscription.is_trial) throw new HexApiError(409, 'trial_not_renewable');
    const plans = await this.plans();
    const plan = plans.plans.find((item) => item.plan_uuid === subscription.plan_uuid);
    if (!plan) throw new HexApiError(404, 'plan_not_found');
    return this.saveQuote(userId, subscription.subscription_id, 'renew', {}, plan.price_minor, plan.price_usd);
  }

  async createPurchaseQuote(userId: string, planId: string) {
    const current = await this.current(userId);
    if (current && current.status !== 'expired') throw new HexApiError(409, 'subscription_already_exists');
    const plans = await this.plans();
    const plan = plans.plans.find((item) => item.plan_uuid === planId);
    if (!plan) throw new HexApiError(404, 'plan_not_found');
    return this.saveQuote(userId, 'new', 'purchase', { planId }, plan.price_minor, plan.price_usd);
  }

  async createUpgradeQuote(userId: string, newPlanUuid: string) {
    const subscription = await this.ownedSubscription(userId);
    const params = new URLSearchParams({ new_plan_uuid: newPlanUuid });
    const quote = await this.hex.get(
      `/subscriptions/${encodeURIComponent(subscription.subscription_id)}/upgrade-quote?${params.toString()}`,
      upgradeQuoteSchema,
    );
    return this.saveQuote(userId, subscription.subscription_id, 'upgrade', { newPlanUuid }, quote.charge_minor, quote.charge_usd);
  }

  async createTrafficQuote(userId: string, gb: number) {
    const subscription = await this.ownedSubscription(userId);
    const plans = await this.plans();
    const pack = plans.traffic_packs.find((item) => item.gb === gb);
    if (!pack) throw new HexApiError(422, 'traffic_pack_not_found');
    return this.saveQuote(userId, subscription.subscription_id, 'traffic', { gb }, pack.price_minor, pack.price_usd);
  }

  async confirmQuote(userId: string, quoteId: string) {
    return inTransaction(this.db, async (client) => {
      const result = await client.query<{
        id: string;
        subscription_id: string;
        action: string;
        request_payload: { newPlanUuid?: string; planId?: string; gb?: number };
        custom_id: string;
        result: Record<string, unknown> | null;
      }>(
        `SELECT id, subscription_id, action, request_payload, custom_id, result
         FROM hex_operation_quotes
         WHERE id = $1 AND user_id = $2 AND expires_at > now()
         FOR UPDATE`,
        [quoteId, userId],
      );
      const quote = result.rows[0];
      if (!quote) throw new HexApiError(404, 'quote_not_found');
      if (quote.result) return quote.result;
      const base = `/subscriptions/${encodeURIComponent(quote.subscription_id)}`;
      const link = quote.action === 'purchase' ? await this.userLink(userId) : null;
      const body = quote.action === 'purchase'
        ? {
            external_user_id: link!.externalUserId,
            custom_id: quote.custom_id,
            plan_uuid: quote.request_payload.planId,
            ...(link!.telegramId && this.hexBotId
              ? { telegram_id: Number(link!.telegramId), bot_id: this.hexBotId }
              : {}),
          }
        : quote.action === 'renew'
        ? { custom_id: quote.custom_id }
        : quote.action === 'upgrade'
          ? { custom_id: quote.custom_id, new_plan_uuid: quote.request_payload.newPlanUuid }
          : { custom_id: quote.custom_id, gb: quote.request_payload.gb };
      const path = quote.action === 'purchase'
        ? '/subscriptions'
        : quote.action === 'renew'
          ? `${base}/renew`
          : `${base}/${quote.action}`;
      const response = await this.hex.post(path, body, mutationResponseSchema, true);
      await this.cache(userId, response.subscription, client);
      const safeResult = {
        subscription: this.safeSubscription(response.subscription),
        chargedMinor: response.charged_minor,
        chargedUsd: response.charged_usd,
      };
      await client.query(
        'UPDATE hex_operation_quotes SET consumed_at = now(), result = $2 WHERE id = $1',
        [quote.id, safeResult],
      );
      return safeResult;
    });
  }

  async setFrozen(userId: string, frozen: boolean) {
    const subscription = await this.ownedSubscription(userId);
    if ((subscription.status === 'frozen') === frozen) return this.safeSubscription(subscription);
    const action = frozen ? 'freeze' : 'unfreeze';
    try {
      const response = await this.hex.post(
        `/subscriptions/${encodeURIComponent(subscription.subscription_id)}/${action}`,
        undefined,
        mutationResponseSchema,
      );
      await this.cache(userId, response.subscription);
      return this.safeSubscription(response.subscription);
    } catch (error) {
      if (error instanceof HexApiError && error.code === 'subscription_state') {
        return this.current(userId);
      }
      throw error;
    }
  }

  private async ownedSubscription(userId: string): Promise<HexSubscription> {
    const link = await this.userLink(userId);
    const query = new URLSearchParams({ external_user_id: link.externalUserId, limit: '100' });
    const list = await this.hex.get(`/subscriptions?${query.toString()}`, subscriptionsResponseSchema);
    const selected = [...list.items].sort((a, b) => this.statusRank(a.status) - this.statusRank(b.status))[0];
    if (!selected) throw new HexApiError(404, 'no_subscription');
    const detail = await this.hex.get(`/subscriptions/${encodeURIComponent(selected.subscription_id)}`, subscriptionResponseSchema);
    if (detail.subscription.external_user_id !== link.externalUserId) throw new HexApiError(403, 'ownership_mismatch');
    return detail.subscription;
  }

  private async userLink(userId: string): Promise<UserLink> {
    const result = await this.db.query<{ id: string; external_user_id: string; telegram_id: string | null }>(
      `SELECT u.id, u.external_user_id, i.telegram_id::text
       FROM users u LEFT JOIN user_identities i ON i.user_id = u.id AND i.provider = 'telegram'
       WHERE u.id = $1 LIMIT 1`,
      [userId],
    );
    const row = result.rows[0];
    if (!row) throw new HexApiError(401, 'user_not_found');
    return { userId: row.id, externalUserId: row.external_user_id, telegramId: row.telegram_id };
  }

  private async saveQuote(userId: string, subscriptionId: string, action: string, payload: object, chargeMinor: number, chargeUsd: string) {
    const result = await this.db.query<{ id: string; expires_at: string }>(
      `INSERT INTO hex_operation_quotes
       (user_id, subscription_id, action, request_payload, charge_minor, charge_usd, custom_id, expires_at)
       VALUES ($1, $2, $3, $4, $5, $6, $7, now() + interval '10 minutes')
       RETURNING id, expires_at`,
      [userId, subscriptionId, action, payload, chargeMinor, chargeUsd, randomUUID()],
    );
    return { quoteId: result.rows[0]!.id, action, chargeMinor, chargeUsd, expiresAt: result.rows[0]!.expires_at };
  }

  async cache(userId: string, subscription: HexSubscription, queryable: Pick<Database, 'query'> = this.db): Promise<void> {
    const safeRaw = Object.fromEntries(
      Object.entries(subscription).filter(([key]) => key !== 'subscription_url'),
    );
    await queryable.query(
      `INSERT INTO hex_subscriptions
       (subscription_id, user_id, status, plan_uuid, plan_name, is_trial, days, days_left,
        device_limit, traffic_limit_bytes, traffic_used_bytes, end_date, frozen_at, source_updated_at, raw_safe)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,now(),$14)
       ON CONFLICT (subscription_id) DO UPDATE SET
        status=EXCLUDED.status, plan_uuid=EXCLUDED.plan_uuid, plan_name=EXCLUDED.plan_name,
        is_trial=EXCLUDED.is_trial, days=EXCLUDED.days, days_left=EXCLUDED.days_left,
        device_limit=EXCLUDED.device_limit, traffic_limit_bytes=EXCLUDED.traffic_limit_bytes,
        traffic_used_bytes=EXCLUDED.traffic_used_bytes, end_date=EXCLUDED.end_date,
        frozen_at=EXCLUDED.frozen_at, source_updated_at=now(), raw_safe=EXCLUDED.raw_safe`,
      [subscription.subscription_id, userId, subscription.status, subscription.plan_uuid,
       subscription.plan_name, subscription.is_trial, subscription.days, subscription.days_left,
       subscription.devices, subscription.traffic_limit_bytes ?? null, subscription.traffic_used_bytes ?? null,
       subscription.end_date, subscription.frozen_at ?? null, safeRaw],
    );
  }

  private safeSubscription(subscription: HexSubscription, connectedDevices?: number) {
    const limit = subscription.traffic_limit_bytes ?? null;
    const used = subscription.traffic_used_bytes ?? null;
    return {
      id: subscription.subscription_id,
      status: subscription.status,
      planId: subscription.plan_uuid,
      planName: subscription.plan_name,
      isTrial: subscription.is_trial,
      days: subscription.days,
      daysLeft: subscription.days_left,
      endDate: subscription.end_date,
      frozenAt: subscription.frozen_at ?? null,
      trafficLimitBytes: limit,
      trafficUsedBytes: used,
      trafficRemainingBytes: limit === null || used === null ? null : Math.max(0, limit - used),
      deviceLimit: subscription.devices,
      connectedDevices: connectedDevices ?? null,
      autoRenew: subscription.auto_renew,
    };
  }

  private statusRank(status: HexSubscription['status']): number {
    return status === 'active' ? 0 : status === 'frozen' ? 1 : 2;
  }
}
