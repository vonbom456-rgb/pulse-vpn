import cors from '@fastify/cors';
import jwt from '@fastify/jwt';
import Fastify, { type FastifyInstance } from 'fastify';
import type { AppConfig } from './config.js';
import type { Database } from './db.js';
import { registerAuthRoutes } from './auth/routes.js';
import { HexClient } from './hex/client.js';
import { registerSubscriptionRoutes } from './hex/routes.js';
import { SubscriptionService } from './hex/subscription-service.js';
import { registerHexWebhook } from './hex/webhook.js';

export async function buildApp(config: AppConfig, db: Database): Promise<FastifyInstance> {
  const app = Fastify({
    logger: {
      level: config.NODE_ENV === 'production' ? 'info' : 'debug',
      redact: ['req.headers.authorization', 'req.body.refreshToken', 'req.body.code', '*.subscription_url'],
    },
    trustProxy: true,
  });
  await app.register(cors, { origin: false });
  await app.register(jwt, { secret: config.JWT_ACCESS_SECRET });
  app.decorate('authenticate', async function authenticate(request, reply) {
    try {
      await request.jwtVerify();
    } catch {
      await reply.code(401).send({ error: 'unauthorized' });
    }
  });
  app.removeContentTypeParser('application/json');
  app.addContentTypeParser('application/json', { parseAs: 'buffer' }, (request, body, done) => {
    request.rawBody = body;
    try {
      done(null, JSON.parse(body.toString('utf8')) as unknown);
    } catch (error) {
      done(error as Error, undefined);
    }
  });
  app.get('/health', () => ({ ok: true }));
  const hex = new HexClient(config);
  const subscriptions = new SubscriptionService(db, hex, config.HEX_BOT_ID);
  registerAuthRoutes(app, db, config);
  registerSubscriptionRoutes(app, subscriptions);
  registerHexWebhook(app, db, config, hex, subscriptions);
  return app;
}

declare module 'fastify' {
  interface FastifyInstance {
    authenticate: (request: import('fastify').FastifyRequest, reply: import('fastify').FastifyReply) => Promise<void>;
  }
}
