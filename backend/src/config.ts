import { z } from 'zod';

const schema = z.object({
  NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
  PORT: z.coerce.number().int().positive().default(8080),
  DATABASE_URL: z.string().min(1),
  JWT_ACCESS_SECRET: z.string().min(32),
  JWT_REFRESH_SECRET: z.string().min(32),
  HEX_API_KEY: z.string().min(1),
  HEX_WEBHOOK_SECRET: z.string().min(16),
  HEX_API_BASE_URL: z.string().url().refine((value) => value.startsWith('https://'), 'HTTPS required'),
  HEX_SUBSCRIPTION_HOSTS: z.string().min(1).transform((value) => value.split(',').map((host) => host.trim().toLowerCase()).filter(Boolean)),
  PUBLIC_BASE_URL: z.string().url(),
  APP_DEEP_LINK: z.string().default('pulsevpn://auth'),
  TELEGRAM_BOT_TOKEN: z.string().min(1),
  TELEGRAM_BOT_USERNAME: z.string().min(1),
  HEX_BOT_ID: z.string().uuid().optional(),
  SMTP_HOST: z.string().min(1),
  SMTP_PORT: z.coerce.number().int().positive().default(587),
  SMTP_SECURE: z.string().default('false').transform((value) => value === 'true'),
  SMTP_USER: z.string().min(1),
  SMTP_PASSWORD: z.string().min(1),
  SMTP_FROM: z.string().min(1),
});

export type AppConfig = z.infer<typeof schema>;

export function loadConfig(environment: NodeJS.ProcessEnv = process.env): AppConfig {
  return schema.parse(environment);
}
