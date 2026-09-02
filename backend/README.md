# PulseVPN Backend

Серверный шлюз между мобильным приложением и официальным HEX VPN Franchise API. Мобильное приложение знает только URL этого backend и собственные JWT. Ключ HEX, webhook secret и `subscription_url` в мобильный клиент не выдаются.

## Запуск

1. Создайте PostgreSQL: `docker compose up -d postgres`.
2. Скопируйте `.env.example` в `.env` и заполните значения.
3. Выполните `npm install`, затем `npm run migrate`.
4. Для разработки: `npm run dev`. Для production: `npm run build && npm start`.
5. Backend должен быть доступен только по HTTPS. В Flutter передайте адрес: `--dart-define=PULSE_API_URL=https://ваш-api-домен`.

`HEX_API_BASE_URL` берите из актуальной официальной документации HEX. `HEX_SUBSCRIPTION_HOSTS` — allowlist доменов, которые HEX реально возвращает в `subscription_url`, через запятую. Сервер не запустится без обоих значений.

## Секреты

Установите только в secret/environment settings хостинга:

- `HEX_API_KEY` — ключ из кабинета HEX;
- `HEX_WEBHOOK_SECRET` — секрет подписи webhook из кабинета HEX;
- `JWT_ACCESS_SECRET`, `JWT_REFRESH_SECRET` — разные случайные значения от 32 символов;
- `TELEGRAM_BOT_TOKEN`, SMTP credentials.

Не добавляйте `.env` в Git. В HEX укажите webhook `https://ваш-api-домен/api/webhooks/hex`. Подпись `X-Signature` проверяется HMAC-SHA256 по сырым байтам тела до разбора JSON; события дедуплицируются по `event_id`.

## Проверки

`npm run check` запускает ESLint, unit-тесты HMAC/Telegram/retry/SSRF и TypeScript build. `npm audit --omit=dev --audit-level=high` выполняется в CI.

Для полного staging-теста с тестовым депозитом HEX вручную пройдите состояния: отсутствие подписки, active, expired, frozen, renew, upgrade, traffic, device delete, 429/503/timeout, неверная подпись, повтор webhook и повторный запуск приложения. Денежные действия в UI всегда требуют quote и отдельное подтверждение.
