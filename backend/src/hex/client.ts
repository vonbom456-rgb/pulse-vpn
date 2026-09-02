import { z } from 'zod';
import type { AppConfig } from '../config.js';

const errorSchema = z.object({
  error_code: z.string().optional(),
  error: z.string().optional(),
  details: z.unknown().optional(),
}).passthrough();

export class HexApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    readonly retryAfterSeconds?: number,
  ) {
    super(`HEX API request failed (${status}/${code})`);
  }
}

export class HexClient {
  constructor(
    private readonly config: Pick<AppConfig, 'HEX_API_KEY' | 'HEX_API_BASE_URL' | 'HEX_SUBSCRIPTION_HOSTS'>,
  ) {}

  async get<T>(path: string, schema: z.ZodType<T>): Promise<T> {
    return this.request('GET', path, schema, undefined, true);
  }

  async post<T>(path: string, body: unknown, schema: z.ZodType<T>, retryable = false): Promise<T> {
    return this.request('POST', path, schema, body, retryable);
  }

  async delete<T>(path: string, schema: z.ZodType<T>): Promise<T> {
    return this.request('DELETE', path, schema, undefined, true);
  }

  async fetchSubscriptionPayload(subscriptionUrl: string): Promise<string> {
    const target = new URL(subscriptionUrl);
    if (target.protocol !== 'https:' || !this.config.HEX_SUBSCRIPTION_HOSTS.includes(target.hostname.toLowerCase())) {
      throw new HexApiError(502, 'subscription_host_not_allowed');
    }
    const response = await fetch(subscriptionUrl, {
      signal: AbortSignal.timeout(12_000),
      headers: { 'User-Agent': 'PulseVPN/1.0' },
      redirect: 'follow',
    });
    if (!response.ok) throw new HexApiError(response.status, 'subscription_payload_unavailable');
    const text = await response.text();
    if (!text.trim()) throw new HexApiError(502, 'empty_subscription_payload');
    return text;
  }

  private async request<T>(
    method: string,
    path: string,
    schema: z.ZodType<T>,
    body: unknown,
    retryable: boolean,
  ): Promise<T> {
    let attempt = 0;
    while (true) {
      try {
        const response = await fetch(`${this.config.HEX_API_BASE_URL}${path}`, {
          method,
          signal: AbortSignal.timeout(8_000),
          headers: {
            Authorization: `Bearer ${this.config.HEX_API_KEY}`,
            Accept: 'application/json',
            ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
          },
          ...(body === undefined ? {} : { body: JSON.stringify(body) }),
        });
        const payload: unknown = await response.json().catch(() => ({}));
        if (response.ok) return schema.parse(payload);
        const parsed = errorSchema.safeParse(payload);
        const code = parsed.success ? (parsed.data.error_code ?? 'hex_error') : 'hex_error';
        const retryAfter = Number(response.headers.get('retry-after') ?? 0) || undefined;
        const error = new HexApiError(response.status, code, retryAfter);
        if (!retryable || attempt >= 2 || ![429, 500, 503].includes(response.status)) throw error;
        await this.delay(retryAfter ? Math.min(retryAfter * 1000, 10_000) : 500 * 2 ** attempt);
      } catch (error) {
        if (error instanceof HexApiError) throw error;
        if (!retryable || attempt >= 2) throw new HexApiError(503, 'hex_unavailable');
        await this.delay(500 * 2 ** attempt);
      }
      attempt += 1;
    }
  }

  private delay(milliseconds: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, milliseconds));
  }
}
