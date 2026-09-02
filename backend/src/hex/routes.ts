import type { FastifyInstance, FastifyRequest } from 'fastify';
import { z } from 'zod';
import { HexApiError } from './client.js';
import { SubscriptionService } from './subscription-service.js';

function userId(request: FastifyRequest): string {
  return request.user.sub;
}

export function registerSubscriptionRoutes(app: FastifyInstance, service: SubscriptionService): void {
  app.get('/api/plans', { onRequest: [app.authenticate] }, async () => {
    const result = await service.plans();
    return {
      currency: result.currency,
      plans: result.plans.map((plan) => ({
        id: plan.plan_uuid,
        name: plan.name,
        days: plan.days,
        deviceLimit: plan.devices,
        priceMinor: plan.price_minor,
        priceUsd: plan.price_usd,
      })),
      trafficPacks: result.traffic_packs.map((pack) => ({ gb: pack.gb, priceMinor: pack.price_minor, priceUsd: pack.price_usd })),
    };
  });

  app.get('/api/subscription', { onRequest: [app.authenticate] }, async (request, reply) => {
    const subscription = await service.current(userId(request));
    if (!subscription) return reply.code(404).send({ error: 'no_subscription' });
    return { subscription };
  });

  app.get('/api/subscription/devices', { onRequest: [app.authenticate] }, async (request) => ({
    devices: await service.devices(userId(request)),
  }));

  app.delete('/api/subscription/devices/:id', { onRequest: [app.authenticate] }, async (request) => {
    const { id } = z.object({ id: z.coerce.number().int().positive() }).parse(request.params);
    return service.removeDevice(userId(request), id);
  });

  app.get('/api/subscription/config', { onRequest: [app.authenticate] }, async (request, reply) => {
    const payload = await service.configPayload(userId(request));
    return reply.header('Cache-Control', 'no-store').type('text/plain; charset=utf-8').send(payload);
  });

  app.post('/api/subscription/renew/quote', { onRequest: [app.authenticate] }, async (request) => (
    service.createRenewQuote(userId(request))
  ));

  app.post('/api/subscription/purchase/quote', { onRequest: [app.authenticate] }, async (request) => {
    const { planId } = z.object({ planId: z.string().uuid() }).parse(request.body);
    return service.createPurchaseQuote(userId(request), planId);
  });

  app.post('/api/subscription/upgrade/quote', { onRequest: [app.authenticate] }, async (request) => {
    const { planId } = z.object({ planId: z.string().uuid() }).parse(request.body);
    return service.createUpgradeQuote(userId(request), planId);
  });

  app.post('/api/subscription/traffic/quote', { onRequest: [app.authenticate] }, async (request) => {
    const { gb } = z.object({ gb: z.number().int().positive() }).parse(request.body);
    return service.createTrafficQuote(userId(request), gb);
  });

  app.post('/api/subscription/actions/confirm', { onRequest: [app.authenticate] }, async (request) => {
    const { quoteId } = z.object({ quoteId: z.string().uuid() }).parse(request.body);
    return service.confirmQuote(userId(request), quoteId);
  });

  app.post('/api/subscription/freeze', { onRequest: [app.authenticate] }, async (request) => (
    service.setFrozen(userId(request), true)
  ));

  app.post('/api/subscription/unfreeze', { onRequest: [app.authenticate] }, async (request) => (
    service.setFrozen(userId(request), false)
  ));

  app.setErrorHandler((error, _request, reply) => {
    if (error instanceof HexApiError) {
      const status = error.code === 'no_subscription' ? 404 : error.status >= 400 && error.status < 600 ? error.status : 502;
      return reply.code(status).send({ error: error.code, retryAfter: error.retryAfterSeconds });
    }
    if (error instanceof z.ZodError) return reply.code(422).send({ error: 'validation_error' });
    app.log.error({ err: error }, 'request_failed');
    return reply.code(500).send({ error: 'internal_error' });
  });
}
