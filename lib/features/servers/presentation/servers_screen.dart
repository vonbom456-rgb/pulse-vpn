import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pulse_vpn/core/providers/app_providers.dart';
import 'package:pulse_vpn/core/theme/pulse_tokens.dart';
import 'package:pulse_vpn/core/vpn_engine/vpn_engine.dart';
import 'package:pulse_vpn/core/vpn_engine/vpn_route_manager.dart';
import 'package:pulse_vpn/features/servers/presentation/server_tile.dart';
import 'package:pulse_vpn/shared/widgets/glass_card.dart';
import 'package:pulse_vpn/shared/widgets/pulse_scaffold.dart';

class ServersScreen extends ConsumerStatefulWidget {
  const ServersScreen({super.key});

  @override
  ConsumerState<ServersScreen> createState() => _ServersScreenState();
}

class _ServersScreenState extends ConsumerState<ServersScreen> {
  var query = '';

  @override
  Widget build(BuildContext context) {
    final groups = ref.watch(vpnRouteGroupsProvider);
    final connected =
        ref.watch(vpnControllerProvider).status == VpnStatus.connected;
    return PulseScaffold(
      title: 'Маршруты',
      actions: [
        IconButton(
          onPressed: () => context.push('/import'),
          icon: const Icon(Icons.add_rounded),
        ),
      ],
      child: Column(children: [
        TextField(
          onChanged: (value) => setState(() => query = value),
          decoration: InputDecoration(
            hintText: 'Название сервера',
            prefixIcon: const Icon(Icons.search_rounded),
            filled: true,
            fillColor: PulseColors.surface,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(PulseRadius.pill),
              borderSide: BorderSide.none,
            ),
          ),
        ),
        const SizedBox(height: PulseSpace.md),
        Expanded(
          child: groups.when(
            loading: () => ListView.separated(
              itemCount: 5,
              separatorBuilder: (_, __) =>
                  const SizedBox(height: PulseSpace.sm),
              itemBuilder: (_, __) => const ServerSkeleton(),
            ),
            error: (error, _) => _RouteMessage(
              icon: Icons.error_outline_rounded,
              title: 'Маршруты недоступны',
              message: error.toString(),
            ),
            data: (items) => _buildGroups(items, connected),
          ),
        ),
      ]),
    );
  }

  Widget _buildGroups(List<VpnRouteGroup> groups, bool connected) {
    final filtered = groups
        .map(
          (group) => VpnRouteGroup(
            tag: group.tag,
            routes: group.routes
                .where(
                  (route) => route.tag.toLowerCase().contains(query.toLowerCase()),
                )
                .toList(growable: false),
          ),
        )
        .where((group) => group.routes.isNotEmpty)
        .toList(growable: false);
    if (filtered.isEmpty) {
      return _RouteMessage(
        icon: connected ? Icons.search_off_rounded : Icons.power_settings_new_rounded,
        title: connected ? 'Ничего не найдено' : 'Сначала подключитесь',
        message: connected
            ? 'Измените запрос поиска'
            : 'sing-box отдаст реальные маршруты после запуска туннеля',
      );
    }
    var index = 0;
    return ListView(
      children: [
        for (final group in filtered) ...[
          Row(children: [
            Expanded(
              child: Text(
                group.tag.toUpperCase(),
                style: Theme.of(context).textTheme.labelLarge?.copyWith(
                      color: PulseColors.textSecondary,
                      letterSpacing: 1.1,
                    ),
              ),
            ),
            TextButton.icon(
              onPressed: () =>
                  ref.read(vpnRouteManagerProvider).test(group.tag),
              icon: const Icon(Icons.speed_rounded, size: 18),
              label: const Text('Проверить'),
            ),
          ]),
          const SizedBox(height: PulseSpace.xs),
          for (final route in group.routes) ...[
            ServerTile(
              route: route,
              index: index++,
              onSelected: () =>
                  ref.read(vpnRouteManagerProvider).select(route),
            ),
            const SizedBox(height: PulseSpace.sm),
          ],
          const SizedBox(height: PulseSpace.sm),
        ],
      ],
    );
  }
}

class _RouteMessage extends StatelessWidget {
  const _RouteMessage({
    required this.icon,
    required this.title,
    required this.message,
  });

  final IconData icon;
  final String title;
  final String message;

  @override
  Widget build(BuildContext context) => Center(
        child: GlassCard(
          child: Padding(
            padding: const EdgeInsets.all(PulseSpace.md),
            child: Column(mainAxisSize: MainAxisSize.min, children: [
              Icon(icon, color: PulseColors.teal, size: 36),
              const SizedBox(height: PulseSpace.sm),
              Text(title, style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: PulseSpace.xs),
              Text(
                message,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            ]),
          ),
        ),
      );
}
