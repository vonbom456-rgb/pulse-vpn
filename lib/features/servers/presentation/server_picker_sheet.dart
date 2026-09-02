import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pulse_vpn/core/providers/app_providers.dart';
import 'package:pulse_vpn/core/theme/pulse_tokens.dart';
import 'package:pulse_vpn/core/vpn_engine/vpn_route_manager.dart';
import 'package:pulse_vpn/features/servers/presentation/server_tile.dart';

class ServerPickerSheet extends ConsumerWidget {
  const ServerPickerSheet({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final groups = ref.watch(vpnRouteGroupsProvider);
    return FractionallySizedBox(
      heightFactor: .82,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(
          PulseSpace.page,
          PulseSpace.sm,
          PulseSpace.page,
          PulseSpace.page,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(
              child: Container(
                width: 42,
                height: 4,
                decoration: BoxDecoration(
                  color: PulseColors.textSecondary.withValues(alpha: .45),
                  borderRadius: BorderRadius.circular(PulseRadius.pill),
                ),
              ),
            ),
            const SizedBox(height: PulseSpace.lg),
            Text('Выберите сигнал', style: Theme.of(context).textTheme.headlineLarge),
            const SizedBox(height: PulseSpace.xs),
            Text(
              'Реальные маршруты активного профиля',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            const SizedBox(height: PulseSpace.lg),
            Expanded(
              child: groups.when(
                loading: () => ListView.separated(
                  itemCount: 5,
                  separatorBuilder: (_, __) =>
                      const SizedBox(height: PulseSpace.sm),
                  itemBuilder: (_, __) => const ServerSkeleton(),
                ),
                error: (error, _) => Center(child: Text(error.toString())),
                data: (items) => _routes(context, ref, items),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _routes(
    BuildContext context,
    WidgetRef ref,
    List<VpnRouteGroup> groups,
  ) {
    final routes = groups.expand((group) => group.routes).toList()
      ..sort((a, b) {
        final aDelay = a.delayMs <= 0 ? 999999 : a.delayMs;
        final bDelay = b.delayMs <= 0 ? 999999 : b.delayMs;
        return aDelay.compareTo(bDelay);
      });
    if (routes.isEmpty) {
      return const Center(child: Text('Подключитесь, чтобы получить маршруты'));
    }
    return ListView.separated(
      itemCount: routes.length,
      separatorBuilder: (_, __) => const SizedBox(height: PulseSpace.sm),
      itemBuilder: (_, index) => ServerTile(
        route: routes[index],
        index: index,
        onSelected: () async {
          await ref.read(vpnRouteManagerProvider).select(routes[index]);
          if (context.mounted) Navigator.of(context).pop();
        },
      ),
    );
  }
}
