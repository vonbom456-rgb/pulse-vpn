CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  external_user_id text UNIQUE NOT NULL,
  email text UNIQUE,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE user_identities (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  provider text NOT NULL CHECK (provider IN ('telegram', 'email')),
  provider_subject text NOT NULL,
  telegram_id bigint,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(provider, provider_subject),
  UNIQUE(telegram_id)
);

CREATE TABLE refresh_sessions (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash text UNIQUE NOT NULL,
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE login_challenges (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  kind text NOT NULL CHECK (kind IN ('email_otp', 'telegram_exchange')),
  subject text NOT NULL,
  secret_hash text NOT NULL,
  payload jsonb NOT NULL DEFAULT '{}'::jsonb,
  expires_at timestamptz NOT NULL,
  consumed_at timestamptz,
  attempts integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX login_challenges_lookup_idx ON login_challenges(kind, subject, created_at DESC);

CREATE TABLE hex_subscriptions (
  subscription_id text PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status text NOT NULL CHECK (status IN ('active', 'frozen', 'expired')),
  plan_uuid uuid NOT NULL,
  plan_name text NOT NULL,
  is_trial boolean NOT NULL,
  days integer NOT NULL,
  days_left integer NOT NULL,
  device_limit integer NOT NULL,
  traffic_limit_bytes bigint,
  traffic_used_bytes bigint,
  end_date timestamptz NOT NULL,
  frozen_at timestamptz,
  source_updated_at timestamptz NOT NULL DEFAULT now(),
  raw_safe jsonb NOT NULL DEFAULT '{}'::jsonb,
  UNIQUE(user_id, subscription_id)
);

CREATE INDEX hex_subscriptions_user_idx ON hex_subscriptions(user_id, source_updated_at DESC);

CREATE TABLE hex_operation_quotes (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  subscription_id text NOT NULL,
  action text NOT NULL,
  request_payload jsonb NOT NULL,
  charge_minor integer NOT NULL,
  charge_usd text NOT NULL,
  custom_id uuid UNIQUE NOT NULL,
  expires_at timestamptz NOT NULL,
  consumed_at timestamptz,
  result jsonb,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE hex_webhook_events (
  event_id text PRIMARY KEY,
  event_name text NOT NULL,
  payload jsonb NOT NULL,
  received_at timestamptz NOT NULL DEFAULT now(),
  processed_at timestamptz
);

CREATE TABLE notification_outbox (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  event_name text NOT NULL,
  payload jsonb NOT NULL,
  delivered_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(user_id, event_name, (payload->>'event_id'))
);
