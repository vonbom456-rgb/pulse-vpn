# Pulse VPN Native

Нативный Android-клиент на Kotlin и Jetpack Compose с ядром sing-box/libbox.

## Возможности

- импорт подписок по HTTPS, QR и из буфера;
- sing-box JSON, Base64-списки VLESS/VMess/Trojan/Shadowsocks/Hysteria2/TUIC и Clash YAML;
- локальные профили и безопасное обновление удалённой подписки;
- реальный Android `VpnService` в отдельном процессе;
- выбор outbound, проверка задержки и статистика трафика;
- маршрутизация всего устройства или отдельных приложений;
- автозапуск после перезагрузки и переход к системному Kill switch;
- тёмная и светлая темы Compose.

Ссылки подписок и конфиги не логируются и хранятся только в приватном каталоге приложения. HEX API и платёжные операции в этой версии отсутствуют.

## Сборка

```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

APK создаются отдельно для `arm64-v8a`, `armeabi-v7a` и `x86_64`, поэтому приложение не тащит библиотеки для чужих процессоров.

## Лицензии

В VPN-слое адаптированы GPLv3-компоненты `flutter_sing_box`/SFA. Полный текст лицензии находится в `COPYING`.
