import { describe, expect, it, vi } from 'vitest';
import { z } from 'zod';
import { HexApiError, HexClient } from '../src/hex/client.js';

const config = {
  HEX_API_KEY: 'server-only-key',
  HEX_API_BASE_URL: 'https://hex-api.example/v1',
  HEX_SUBSCRIPTION_HOSTS: ['subscriptions.example'],
};

describe('HexClient reliability', () => {
  it('retries a rate limit and parses the next response', async () => {
    const fetcher = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(new Response(JSON.stringify({ error_code: 'rate_limited' }), { status: 429, headers: { 'Retry-After': '0.001' } }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ success: true }), { status: 200 }));
    const client = new HexClient(config, fetcher);
    await expect(client.get('/plans', z.object({ success: z.literal(true) }))).resolves.toEqual({ success: true });
    expect(fetcher).toHaveBeenCalledTimes(2);
  });

  it('does not retry a non-idempotent mutation by default', async () => {
    const fetcher = vi.fn<typeof fetch>().mockResolvedValue(
      new Response(JSON.stringify({ error_code: 'upstream_unavailable' }), { status: 503 }),
    );
    const client = new HexClient(config, fetcher);
    await expect(client.post('/freeze', undefined, z.unknown())).rejects.toBeInstanceOf(HexApiError);
    expect(fetcher).toHaveBeenCalledTimes(1);
  });

  it('blocks an unapproved subscription host before making a request', async () => {
    const fetcher = vi.fn<typeof fetch>();
    const client = new HexClient(config, fetcher);
    await expect(client.fetchSubscriptionPayload('https://internal.example/config')).rejects.toMatchObject({ code: 'subscription_host_not_allowed' });
    expect(fetcher).not.toHaveBeenCalled();
  });
});
