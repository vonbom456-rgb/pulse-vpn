# Pulse VPN

[![Build Android APK](https://github.com/vonbom456-rgb/pulse-vpn/actions/workflows/build-android.yml/badge.svg)](https://github.com/vonbom456-rgb/pulse-vpn/actions/workflows/build-android.yml)

Flutter-клиент премиального VPN-сервиса. Android-сборка использует реальный `VpnService` и sing-box; UI и доменная логика не зависят от Android API, ядро скрыто за `VpnEngine`.

## Что готово

- onboarding с parallax-переходами;
- Connect-экран с процедурным heartbeat, концентрическими кольцами, wave transition, haptics и live-метриками;
- реальные группы/outbound-маршруты из sing-box, выбор узла и URL test;
- импорт QR, subscription URL, VLESS URI и sing-box JSON;
- статистика с live-графиком;
- настройки, светлая/тёмная тема, профиль подписки;
- production Android-адаптер `SingBoxVpnEngine` и mock через `--dart-define=PULSE_USE_MOCK=true`;
- реальные потоки статуса, download/upload и трафика за сессию;
- Riverpod для DI и асинхронного состояния, GoRouter для переходов.

## Запуск

В корне проекта:

```bash
flutter create . --platforms=android,ios
flutter pub get
dart run flutter_launcher_icons
flutter run
```

Минимальная версия Android — API 26. При первом запуске Android покажет системный диалог разрешения VPN. Без импортированного профиля кнопка подключения открывает экран добавления подписки.

## APK через GitHub Actions

После загрузки проекта в GitHub откройте **Actions → Build Android APK → Run workflow**. Готовый файл появится внизу страницы запуска в разделе **Artifacts** под именем `pulse-vpn-release`.

Workflow сам создаёт Android runner, выставляет minSdk 26, генерирует launcher icon, запускает analyze/tests и собирает release APK. Pull request также проходит полную проверку до слияния.

## VPN-ядро и лицензия

Интеграция сделана через `flutter_sing_box 1.1.5`, который включает Android `VpnService` и `libbox 1.13.15`. Обёртка распространяется под GPL-3.0. Перед публикацией закрытого коммерческого приложения нужно либо выполнить требования GPL для производного продукта, либо заменить адаптер на собственную сборку sing-box с подходящей юридической моделью. Благодаря контрактам `VpnEngine`, `VpnProfileManager` и `VpnRouteManager` UI переписывать не потребуется.

iOS UI уже платформонезависим, но production Network Extension пока не подключён: текущий production-адаптер включается только на Android и честно показывает ошибку на других платформах.

## Почему Riverpod

Riverpod одновременно решает DI для сменного `VpnEngine`, управление жизненным циклом stream-подписок и тестирование без `BuildContext`. Для этого проекта он оставляет меньше событийного boilerplate, чем Bloc, при сохранении явной модели состояния.

## Структура

```text
lib/
├── core/
│   ├── models/
│   ├── providers/
│   ├── router/
│   ├── theme/
│   └── vpn_engine/
├── features/
│   ├── connect/
│   ├── import_subscription/
│   ├── onboarding/
│   ├── profile/
│   ├── servers/
│   ├── settings/
│   └── statistics/
└── shared/widgets/
```

## Production checklist

- прогнать Android-сборку и реальное подключение на физическом устройстве;
- добавить iOS Network Extension, App Group и entitlement-профили;
- принять лицензионное решение по GPL-3.0 или заменить sing-box adapter;
- заменить процедурный pulse на финальный Rive asset при наличии арт-дирекшна;
- добавить фирменный variable font и единый кастомный line-icon font;
- добавить шифрование чувствительных локальных метаданных и миграции;
- проверить нестандартные subscription-форматы конкретного провайдера;
- подключить API тарифа, оплату и secure storage токенов;
- добавить golden/widget/integration тесты и CI signing.
