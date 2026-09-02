import { z } from 'zod';

export const hexSubscriptionSchema = z.object({
  subscription_id: z.string().min(1),
  external_user_id: z.string().min(1),
  telegram_id: z.number().int().nullable().optional(),
  bot_id: z.string().uuid().nullable().optional(),
  plan_uuid: z.string().uuid(),
  plan_name: z.string(),
  is_trial: z.boolean(),
  status: z.enum(['active', 'frozen', 'expired']),
  subscription_url: z.string().url(),
  days: z.number().int(),
  days_left: z.number().int(),
  devices: z.number().int(),
  traffic_limit_bytes: z.number().int().nullable().optional(),
  traffic_used_bytes: z.number().int().nullable().optional(),
  auto_renew: z.boolean(),
  renewal_count: z.number().int(),
  created_at: z.string().datetime({ offset: true }),
  end_date: z.string().datetime({ offset: true }),
  frozen_at: z.string().datetime({ offset: true }).nullable().optional(),
  price_minor: z.number().int(),
  price_usd: z.string(),
}).passthrough();

export type HexSubscription = z.infer<typeof hexSubscriptionSchema>;

export const plansResponseSchema = z.object({
  success: z.literal(true),
  currency: z.literal('USD'),
  plans: z.array(z.object({
    plan_uuid: z.string().uuid(),
    name: z.string(),
    days: z.number().int().positive(),
    devices: z.number().int().positive(),
    price_minor: z.number().int().nonnegative(),
    price_usd: z.string(),
  })),
  trial: z.object({
    available: z.boolean(),
    plan_uuid: z.string().uuid().optional(),
    name: z.string().optional(),
    days: z.number().int().optional(),
    devices: z.number().int().optional(),
  }).passthrough(),
  traffic_packs: z.array(z.object({
    gb: z.number().int().positive(),
    price_minor: z.number().int().nonnegative(),
    price_usd: z.string(),
  })),
});

export const subscriptionsResponseSchema = z.object({
  success: z.literal(true),
  total: z.number().int(),
  offset: z.number().int(),
  limit: z.number().int(),
  items: z.array(hexSubscriptionSchema),
});

export const subscriptionResponseSchema = z.object({
  success: z.literal(true),
  subscription: hexSubscriptionSchema,
});

export const mutationResponseSchema = subscriptionResponseSchema.extend({
  custom_id: z.string().optional(),
  charged_minor: z.number().int().optional(),
  charged_usd: z.string().optional(),
  balance_after_minor: z.number().int().optional(),
  balance_after_usd: z.string().optional(),
}).passthrough();

export const upgradeQuoteSchema = z.object({
  success: z.literal(true),
  subscription_id: z.string(),
  new_plan_uuid: z.string().uuid(),
  new_plan_name: z.string(),
  remaining_days: z.number().int(),
  charge_minor: z.number().int().nonnegative(),
  charge_usd: z.string(),
});

export const devicesResponseSchema = z.object({
  success: z.literal(true),
  subscription_id: z.string(),
  devices: z.array(z.object({
    id: z.number().int(),
    hwid: z.string(),
    name: z.string().nullable().optional(),
    os: z.string().nullable().optional(),
    model: z.string().nullable().optional(),
    os_version: z.string().nullable().optional(),
    ip: z.string().nullable().optional(),
    first_seen: z.string().datetime({ offset: true }),
    last_seen: z.string().datetime({ offset: true }),
  }).passthrough()),
});

export const webhookPayloadSchema = z.object({
  event: z.string(),
  event_id: z.string().min(1),
  occurred_at: z.string().datetime({ offset: true }),
  source: z.string(),
  subscription: hexSubscriptionSchema.optional(),
}).passthrough();
