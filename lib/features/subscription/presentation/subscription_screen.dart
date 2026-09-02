import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pulse_vpn/core/providers/app_providers.dart';
import 'package:pulse_vpn/core/theme/pulse_tokens.dart';
import 'package:pulse_vpn/features/auth/application/auth_controller.dart';
import 'package:pulse_vpn/features/subscription/application/subscription_controller.dart';
import 'package:pulse_vpn/features/subscription/domain/pulse_subscription.dart';
import 'package:pulse_vpn/shared/widgets/glass_card.dart';
import 'package:pulse_vpn/shared/widgets/pulse_banner.dart';
import 'package:pulse_vpn/shared/widgets/pulse_scaffold.dart';

class SubscriptionScreen extends ConsumerWidget {
  const SubscriptionScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authControllerProvider);
    if (auth.isLoading) {
      return const PulseScaffold(title: 'Подписка', child: Center(child: CircularProgressIndicator()));
    }
    if (auth.value == null) return _SignedOut(onLogin: () => context.push('/auth'));
    final state = ref.watch(pulseSubscriptionProvider);
    return PulseScaffold(
      title: 'Подписка',
      actions: [IconButton(onPressed: () => ref.read(pulseSubscriptionProvider.notifier).refresh(), icon: const Icon(Icons.refresh_rounded))],
      child: state.when(
        loading: () => const _LoadingState(),
        error: (error, _) => _ErrorState(
          noSubscription: error is DioException && error.response?.statusCode == 404,
          onRetry: () => ref.read(pulseSubscriptionProvider.notifier).refresh(),
          onPurchase: () => _purchaseFirst(context, ref),
        ),
        data: (subscription) => subscription == null
            ? _ErrorState(noSubscription: true, onRetry: () => ref.read(pulseSubscriptionProvider.notifier).refresh(), onPurchase: () => _purchaseFirst(context, ref))
            : _SubscriptionBody(subscription: subscription),
      ),
    );
  }
}

class _SignedOut extends StatelessWidget {
  const _SignedOut({required this.onLogin});
  final VoidCallback onLogin;
  @override
  Widget build(BuildContext context) => PulseScaffold(
    title: 'Подписка',
    child: Center(child: GlassCard(child: Padding(
      padding: const EdgeInsets.all(PulseSpace.md),
      child: Column(mainAxisSize: MainAxisSize.min, children: [
        const Icon(Icons.person_outline_rounded, size: 40, color: PulseColors.teal),
        const SizedBox(height: PulseSpace.sm),
        Text('Войдите в Pulse', style: Theme.of(context).textTheme.titleLarge),
        const SizedBox(height: PulseSpace.xs),
        Text('Чтобы управлять тарифом, трафиком и устройствами.', textAlign: TextAlign.center, style: Theme.of(context).textTheme.bodyMedium),
        const SizedBox(height: PulseSpace.md),
        FilledButton(onPressed: onLogin, child: const Text('Войти')),
      ]),
    ))),
  );
}

class _SubscriptionBody extends ConsumerWidget {
  const _SubscriptionBody({required this.subscription});
  final PulseSubscription subscription;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final color = switch (subscription.status) {
      PulseSubscriptionStatus.active => PulseColors.success,
      PulseSubscriptionStatus.frozen => PulseColors.teal,
      PulseSubscriptionStatus.expired => PulseColors.alert,
    };
    return ListView(children: [
      Container(
        padding: const EdgeInsets.all(PulseSpace.lg),
        decoration: BoxDecoration(
          gradient: LinearGradient(colors: [PulseColors.indigo.withValues(alpha: .48), color.withValues(alpha: .20)]),
          borderRadius: BorderRadius.circular(PulseRadius.lg),
          boxShadow: PulseShadows.glow(color, strength: .18),
        ),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [Expanded(child: Text(subscription.planName, style: Theme.of(context).textTheme.headlineMedium)), _Status(status: subscription.status, color: color)]),
          const SizedBox(height: PulseSpace.lg),
          Text('${subscription.daysLeft}', style: Theme.of(context).textTheme.displaySmall?.copyWith(fontWeight: FontWeight.w700)),
          Text('дней осталось · до ${_date(subscription.endDate)}', style: Theme.of(context).textTheme.bodyMedium),
          if (subscription.daysLeft <= 3 && subscription.status == PulseSubscriptionStatus.active) ...[
            const SizedBox(height: PulseSpace.sm),
            const Text('Срок скоро закончится', style: TextStyle(color: PulseColors.alert, fontWeight: FontWeight.w700)),
          ],
        ]),
      ),
      const SizedBox(height: PulseSpace.md),
      Row(children: [
        Expanded(child: _MetricCard(label: 'Трафик', value: _traffic(subscription), icon: Icons.data_usage_rounded)),
        const SizedBox(width: PulseSpace.sm),
        Expanded(child: _MetricCard(label: 'Устройства', value: '${subscription.connectedDevices ?? '—'} / ${subscription.deviceLimit}', icon: Icons.devices_rounded, onTap: () => context.push('/devices'))),
      ]),
      const SizedBox(height: PulseSpace.lg),
      Text('УПРАВЛЕНИЕ', style: Theme.of(context).textTheme.labelLarge?.copyWith(color: PulseColors.textSecondary, letterSpacing: 1.2)),
      const SizedBox(height: PulseSpace.xs),
      GlassCard(padding: EdgeInsets.zero, child: Column(children: [
        _ActionTile(icon: Icons.calendar_month_rounded, title: 'Продлить', subtitle: 'Стоимость покажем до списания', onTap: () => _renew(context, ref)),
        const Divider(height: 1, indent: 56),
        _ActionTile(icon: Icons.workspace_premium_outlined, title: 'Сменить тариф', subtitle: 'HEX рассчитает точную доплату', onTap: () => _upgrade(context, ref)),
        const Divider(height: 1, indent: 56),
        _ActionTile(icon: Icons.add_chart_rounded, title: 'Докупить трафик', subtitle: 'Доступные пакеты от HEX', onTap: () => _trafficPack(context, ref)),
        const Divider(height: 1, indent: 56),
        _ActionTile(icon: Icons.devices_outlined, title: 'Устройства', subtitle: '${subscription.connectedDevices ?? '—'} подключено', onTap: () => context.push('/devices')),
      ])),
      const SizedBox(height: PulseSpace.md),
      OutlinedButton.icon(
        onPressed: subscription.status == PulseSubscriptionStatus.expired ? null : () => _freeze(context, ref),
        icon: Icon(subscription.status == PulseSubscriptionStatus.frozen ? Icons.play_arrow_rounded : Icons.ac_unit_rounded),
        label: Text(subscription.status == PulseSubscriptionStatus.frozen ? 'Разморозить подписку' : 'Заморозить подписку'),
      ),
      const SizedBox(height: PulseSpace.sm),
      TextButton.icon(onPressed: () => _install(context, ref), icon: const Icon(Icons.sync_rounded), label: const Text('Синхронизировать VPN-профиль')),
    ]);
  }

  Future<void> _renew(BuildContext context, WidgetRef ref) async {
    await _paid(context, ref, () => ref.read(subscriptionRepositoryProvider).renewQuote(), 'Продлить подписку?');
  }

  Future<void> _upgrade(BuildContext context, WidgetRef ref) async {
    final catalog = await ref.read(pulsePlansProvider.future);
    if (!context.mounted) return;
    final plan = await showModalBottomSheet<PulsePlan>(context: context, builder: (context) => SafeArea(child: ListView(shrinkWrap: true, padding: const EdgeInsets.all(PulseSpace.md), children: [
      Text('Выберите тариф', style: Theme.of(context).textTheme.headlineSmall),
      for (final plan in catalog.plans) ListTile(title: Text(plan.name), subtitle: Text('${plan.days} дней · ${plan.deviceLimit} устройств'), trailing: Text('\$${plan.priceUsd}'), onTap: () => Navigator.pop(context, plan)),
    ])));
    if (plan == null || !context.mounted) return;
    await _paid(context, ref, () => ref.read(subscriptionRepositoryProvider).upgradeQuote(plan.id), 'Сменить тариф на ${plan.name}?');
  }

  Future<void> _trafficPack(BuildContext context, WidgetRef ref) async {
    final catalog = await ref.read(pulsePlansProvider.future);
    if (!context.mounted) return;
    final pack = await showModalBottomSheet<TrafficPack>(context: context, builder: (context) => SafeArea(child: ListView(shrinkWrap: true, padding: const EdgeInsets.all(PulseSpace.md), children: [
      Text('Дополнительный трафик', style: Theme.of(context).textTheme.headlineSmall),
      for (final pack in catalog.trafficPacks) ListTile(title: Text('${pack.gb} GB'), trailing: Text('\$${pack.priceUsd}'), onTap: () => Navigator.pop(context, pack)),
    ])));
    if (pack == null || !context.mounted) return;
    await _paid(context, ref, () => ref.read(subscriptionRepositoryProvider).trafficQuote(pack.gb), 'Докупить ${pack.gb} GB?');
  }

  Future<void> _paid(BuildContext context, WidgetRef ref, Future<PurchaseQuote> Function() quoteLoader, String title) async {
    try {
      final quote = await quoteLoader();
      if (!context.mounted) return;
      final confirmed = await showDialog<bool>(context: context, builder: (context) => AlertDialog(
        title: Text(title),
        content: Text('С депозита HEX будет списано \$${quote.chargeUsd}. Действие нельзя отменить.'),
        actions: [TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Отмена')), FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Подтвердить'))],
      ));
      if (confirmed == true) {
        await ref.read(pulseSubscriptionProvider.notifier).confirm(quote);
        await HapticFeedback.mediumImpact();
      }
    } catch (_) {
      if (context.mounted) PulseBanner.show(context, 'Операция недоступна. Обновите данные и повторите.');
    }
  }

  Future<void> _freeze(BuildContext context, WidgetRef ref) async {
    final frozen = subscription.status != PulseSubscriptionStatus.frozen;
    final confirmed = await showDialog<bool>(context: context, builder: (context) => AlertDialog(
      title: Text(frozen ? 'Заморозить подписку?' : 'Разморозить подписку?'),
      content: Text(frozen ? 'Срок остановится, а VPN-конфигурация временно перестанет работать.' : 'Отсчёт срока продолжится с текущего момента.'),
      actions: [TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Отмена')), FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Подтвердить'))],
    ));
    if (confirmed == true) await ref.read(pulseSubscriptionProvider.notifier).setFrozen(frozen);
  }

  Future<void> _install(BuildContext context, WidgetRef ref) async {
    try {
      final payload = await ref.read(subscriptionRepositoryProvider).configPayload();
      await ref.read(vpnProfilesProvider.notifier).importSource(payload, name: 'Pulse HEX');
      if (context.mounted) PulseBanner.show(context, 'VPN-профиль синхронизирован');
    } catch (_) {
      if (context.mounted) PulseBanner.show(context, 'Не удалось синхронизировать VPN-профиль');
    }
  }

  static String _date(DateTime value) => '${value.day.toString().padLeft(2, '0')}.${value.month.toString().padLeft(2, '0')}.${value.year}';
  static String _traffic(PulseSubscription value) {
    if (value.trafficLimitBytes == null) return 'Без лимита';
    final used = (value.trafficUsedBytes ?? 0) / 1073741824;
    final limit = value.trafficLimitBytes! / 1073741824;
    return '${used.toStringAsFixed(1)} / ${limit.toStringAsFixed(0)} GB';
  }
}

class _Status extends StatelessWidget {
  const _Status({required this.status, required this.color});
  final PulseSubscriptionStatus status;
  final Color color;
  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: PulseSpace.sm, vertical: PulseSpace.xs),
    decoration: BoxDecoration(color: color.withValues(alpha: .14), borderRadius: BorderRadius.circular(PulseRadius.pill)),
    child: Text(switch (status) { PulseSubscriptionStatus.active => 'ACTIVE', PulseSubscriptionStatus.frozen => 'FROZEN', PulseSubscriptionStatus.expired => 'EXPIRED' }, style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.w700)),
  );
}

class _MetricCard extends StatelessWidget {
  const _MetricCard({required this.label, required this.value, required this.icon, this.onTap});
  final String label;
  final String value;
  final IconData icon;
  final VoidCallback? onTap;
  @override
  Widget build(BuildContext context) => GlassCard(onTap: onTap, child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Icon(icon, color: PulseColors.teal), const SizedBox(height: PulseSpace.sm), Text(value, style: Theme.of(context).textTheme.titleMedium), Text(label, style: Theme.of(context).textTheme.bodyMedium)]));
}

class _ActionTile extends StatelessWidget {
  const _ActionTile({required this.icon, required this.title, required this.subtitle, required this.onTap});
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;
  @override
  Widget build(BuildContext context) => ListTile(onTap: onTap, leading: Icon(icon, color: PulseColors.teal), title: Text(title), subtitle: Text(subtitle), trailing: const Icon(Icons.chevron_right_rounded));
}

class _LoadingState extends StatelessWidget {
  const _LoadingState();
  @override
  Widget build(BuildContext context) => ListView(children: [for (var i = 0; i < 4; i++) ...[const GlassCard(child: SizedBox(height: 72)), const SizedBox(height: PulseSpace.sm)]]);
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({required this.noSubscription, required this.onRetry, required this.onPurchase});
  final bool noSubscription;
  final VoidCallback onRetry;
  final VoidCallback onPurchase;
  @override
  Widget build(BuildContext context) => Center(child: GlassCard(child: Padding(padding: const EdgeInsets.all(PulseSpace.md), child: Column(mainAxisSize: MainAxisSize.min, children: [
    Icon(noSubscription ? Icons.add_card_rounded : Icons.cloud_off_rounded, size: 40, color: PulseColors.teal),
    const SizedBox(height: PulseSpace.sm),
    Text(noSubscription ? 'Подписки пока нет' : 'HEX временно недоступен', style: Theme.of(context).textTheme.titleLarge),
    const SizedBox(height: PulseSpace.xs),
    Text(noSubscription ? 'Подписка появится здесь после оформления.' : 'Проверьте интернет. Сохранённый VPN-профиль продолжит работать.', textAlign: TextAlign.center, style: Theme.of(context).textTheme.bodyMedium),
    const SizedBox(height: PulseSpace.md),
    if (noSubscription) FilledButton(onPressed: onPurchase, child: const Text('Выбрать тариф')),
    TextButton(onPressed: onRetry, child: const Text('Обновить')),
  ]))));
}

Future<void> _purchaseFirst(BuildContext context, WidgetRef ref) async {
  try {
    final catalog = await ref.read(pulsePlansProvider.future);
    if (!context.mounted) return;
    final plan = await showModalBottomSheet<PulsePlan>(
      context: context,
      builder: (context) => SafeArea(child: ListView(
        shrinkWrap: true,
        padding: const EdgeInsets.all(PulseSpace.md),
        children: [
          Text('Выберите тариф', style: Theme.of(context).textTheme.headlineSmall),
          for (final plan in catalog.plans)
            ListTile(
              title: Text(plan.name),
              subtitle: Text('${plan.days} дней · ${plan.deviceLimit} устройств'),
              trailing: Text('\$${plan.priceUsd}'),
              onTap: () => Navigator.pop(context, plan),
            ),
        ],
      )),
    );
    if (plan == null || !context.mounted) return;
    final quote = await ref.read(subscriptionRepositoryProvider).purchaseQuote(plan.id);
    if (!context.mounted) return;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Подключить ${plan.name}?'),
        content: Text('С депозита HEX будет списано \$${quote.chargeUsd} только после подтверждения.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Отмена')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Подтвердить')),
        ],
      ),
    );
    if (confirmed == true) await ref.read(pulseSubscriptionProvider.notifier).confirm(quote);
  } catch (_) {
    if (context.mounted) PulseBanner.show(context, 'Не удалось оформить подписку');
  }
}
