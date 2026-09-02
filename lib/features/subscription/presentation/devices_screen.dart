import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pulse_vpn/core/theme/pulse_tokens.dart';
import 'package:pulse_vpn/features/subscription/application/subscription_controller.dart';
import 'package:pulse_vpn/features/subscription/domain/pulse_subscription.dart';
import 'package:pulse_vpn/shared/widgets/glass_card.dart';
import 'package:pulse_vpn/shared/widgets/pulse_banner.dart';
import 'package:pulse_vpn/shared/widgets/pulse_scaffold.dart';

class DevicesScreen extends ConsumerWidget {
  const DevicesScreen({super.key});
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final devices = ref.watch(pulseDevicesProvider);
    return PulseScaffold(
      title: 'Устройства',
      child: devices.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (_, __) => Center(child: OutlinedButton(onPressed: () => ref.invalidate(pulseDevicesProvider), child: const Text('Повторить'))),
        data: (items) => items.isEmpty
            ? Center(child: Text('Подключённых устройств нет', style: Theme.of(context).textTheme.bodyLarge))
            : ListView(children: [
                Text('HEX не помечает текущее устройство, поэтому удаление всегда требует двойного подтверждения.', style: Theme.of(context).textTheme.bodyMedium),
                const SizedBox(height: PulseSpace.md),
                for (final device in items) ...[_DeviceCard(device: device, onDelete: () => _delete(context, ref, device)), const SizedBox(height: PulseSpace.sm)],
              ]),
      ),
    );
  }

  Future<void> _delete(BuildContext context, WidgetRef ref, PulseDevice device) async {
    final first = await showDialog<bool>(context: context, builder: (context) => AlertDialog(
      title: const Text('Отвязать устройство?'),
      content: Text('${device.name ?? device.model ?? 'Устройство'} потеряет доступ к этой подписке.'),
      actions: [TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Отмена')), TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('Продолжить'))],
    ));
    if (first != true || !context.mounted) return;
    final typed = await showDialog<bool>(context: context, builder: (context) => AlertDialog(
      title: const Text('Подтвердите удаление'),
      content: const Text('Если это текущий телефон, VPN перестанет работать до повторной установки профиля.'),
      actions: [TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Не удалять')), FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Отвязать'))],
    ));
    if (typed == true) {
      await ref.read(subscriptionRepositoryProvider).removeDevice(device.id);
      ref.invalidate(pulseDevicesProvider);
      if (context.mounted) PulseBanner.show(context, 'Устройство отвязано');
    }
  }
}

class _DeviceCard extends StatelessWidget {
  const _DeviceCard({required this.device, required this.onDelete});
  final PulseDevice device;
  final VoidCallback onDelete;
  @override
  Widget build(BuildContext context) => GlassCard(child: Row(children: [
    Container(width: 46, height: 46, decoration: BoxDecoration(color: PulseColors.indigo.withValues(alpha: .14), borderRadius: BorderRadius.circular(PulseRadius.sm)), child: Icon(device.os?.toLowerCase().contains('ios') == true ? Icons.phone_iphone_rounded : Icons.phone_android_rounded, color: PulseColors.teal)),
    const SizedBox(width: PulseSpace.sm),
    Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(device.name ?? device.model ?? 'Устройство', style: Theme.of(context).textTheme.titleMedium), Text('${device.os ?? 'OS неизвестна'}${device.osVersion == null ? '' : ' · ${device.osVersion}'}', style: Theme.of(context).textTheme.bodyMedium), Text('Активность: ${_date(device.lastSeen)}', style: Theme.of(context).textTheme.bodySmall)])),
    IconButton(onPressed: onDelete, icon: const Icon(Icons.link_off_rounded, color: PulseColors.alert)),
  ]));
  static String _date(DateTime value) => '${value.toLocal().day.toString().padLeft(2, '0')}.${value.toLocal().month.toString().padLeft(2, '0')}.${value.toLocal().year}';
}
