import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pulse_vpn/core/providers/app_providers.dart';
import 'package:pulse_vpn/core/theme/pulse_tokens.dart';
import 'package:pulse_vpn/core/vpn_engine/vpn_engine.dart';
import 'package:pulse_vpn/core/vpn_engine/vpn_profile_manager.dart';
import 'package:pulse_vpn/core/vpn_engine/vpn_route_manager.dart';
import 'package:pulse_vpn/features/connect/presentation/pulse_connect_button.dart';
import 'package:pulse_vpn/features/servers/presentation/server_picker_sheet.dart';
import 'package:pulse_vpn/shared/widgets/animated_number.dart';
import 'package:pulse_vpn/shared/widgets/glass_card.dart';
import 'package:pulse_vpn/shared/widgets/pulse_banner.dart';

class ConnectScreen extends ConsumerWidget {
  const ConnectScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final vpn = ref.watch(vpnControllerProvider);
    final profiles = ref.watch(vpnProfilesProvider);
    final routeGroups = ref.watch(vpnRouteGroupsProvider).asData?.value ??
        const <VpnRouteGroup>[];
    final profileList = profiles.asData?.value ?? const <VpnProfile>[];
    final selectedProfile = profileList.isEmpty
        ? null
        : profileList.firstWhere(
            (profile) => profile.isSelected,
            orElse: () => profileList.first,
          );
    final configured = selectedProfile != null;
    VpnRoute? selectedRoute;
    for (final group in routeGroups) {
      for (final route in group.routes) {
        if (route.isSelected) selectedRoute ??= route;
      }
    }
    final connected = vpn.status == VpnStatus.connected;
    ref.listen(vpnControllerProvider, (previous, next) {
      if (next.status == VpnStatus.error && next.errorMessage != null) {
        _showError(context, next.errorMessage!);
      }
    });

    return Scaffold(
      body: Stack(
        children: [
          AnimatedPositioned(
            duration: PulseMotion.slow,
            curve: PulseMotion.routeCurve,
            left: -80,
            right: -80,
            bottom: connected ? -90 : -260,
            height: 360,
            child: AnimatedOpacity(
              duration: PulseMotion.slow,
              opacity: connected ? .22 : .06,
              child: const DecoratedBox(
                decoration: BoxDecoration(
                  gradient: RadialGradient(colors: [PulseColors.success, Colors.transparent]),
                ),
              ),
            ),
          ),
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: PulseSpace.page),
              child: Column(
                children: [
                  _Header(onProfile: () => context.push('/profile')),
                  const Spacer(),
                  Text(
                    !configured
                        ? 'Нужен профиль'
                        : connected
                            ? 'Защищено'
                            : 'Не подключено',
                    style: Theme.of(context).textTheme.headlineMedium,
                  ).animate(key: ValueKey(vpn.status)).fadeIn().slideY(begin: .15),
                  const SizedBox(height: PulseSpace.xs),
                  Text(
                    !configured
                        ? 'Добавьте ссылку или отсканируйте QR'
                        : connected
                            ? 'Сигнал стабилен · IP скрыт'
                            : 'Один импульс до приватности',
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                  PulseConnectButton(
                    status: vpn.status,
                    isConfigured: configured,
                    onPressed: () => configured
                        ? ref.read(vpnControllerProvider.notifier).toggle()
                        : context.push('/import'),
                  ),
                  AnimatedSwitcher(
                    duration: PulseMotion.standard,
                    child: connected
                        ? _LiveMetrics(vpn: vpn)
                        : const SizedBox(height: 64),
                  ),
                  const Spacer(),
                  _ProfileSelector(
                    profile: selectedProfile,
                    route: selectedRoute,
                  ),
                  const SizedBox(height: PulseSpace.lg),
                  const _NavigationDock(),
                  const SizedBox(height: PulseSpace.sm),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _showError(BuildContext context, String message) {
    PulseBanner.show(context, message);
  }
}

class _Header extends StatelessWidget {
  const _Header({required this.onProfile});
  final VoidCallback onProfile;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: PulseSpace.sm),
      child: Row(
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(PulseRadius.sm),
            child: Image.asset(
              'assets/images/pulse_app_icon.png',
              width: 38,
              height: 38,
              fit: BoxFit.cover,
            ),
          ),
          const SizedBox(width: PulseSpace.sm),
          ShaderMask(
            shaderCallback: PulseColors.pulseGradient.createShader,
            child: Text('PULSE', style: Theme.of(context).textTheme.headlineMedium?.copyWith(color: Colors.white, letterSpacing: 2)),
          ),
          const Spacer(),
          IconButton.filledTonal(onPressed: onProfile, icon: const Icon(Icons.person_outline_rounded)),
        ],
      ),
    );
  }
}

class _LiveMetrics extends StatelessWidget {
  const _LiveMetrics({required this.vpn});
  final VpnSnapshot vpn;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 64,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          _Metric(icon: Icons.south_rounded, label: 'Загрузка', value: vpn.downloadMbps),
          const SizedBox(width: PulseSpace.xl),
          _Metric(icon: Icons.north_rounded, label: 'Отдача', value: vpn.uploadMbps),
        ],
      ),
    );
  }
}

class _Metric extends StatelessWidget {
  const _Metric({required this.icon, required this.label, required this.value});
  final IconData icon;
  final String label;
  final double value;

  @override
  Widget build(BuildContext context) {
    return Row(children: [
      Icon(icon, size: 18, color: PulseColors.success),
      const SizedBox(width: PulseSpace.xs),
      Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisAlignment: MainAxisAlignment.center, children: [
        AnimatedNumber(value: value, builder: (_, number) => Text('${number.toStringAsFixed(1)} Мбит/с', style: Theme.of(context).textTheme.titleMedium)),
        Text(label, style: Theme.of(context).textTheme.bodyMedium),
      ]),
    ]);
  }
}

class _ProfileSelector extends StatelessWidget {
  const _ProfileSelector({required this.profile, required this.route});
  final VpnProfile? profile;
  final VpnRoute? route;

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      borderRadius: PulseRadius.lg,
      onTap: () async {
        await HapticFeedback.selectionClick();
        if (!context.mounted) return;
        if (route != null) {
          await showModalBottomSheet<void>(
            context: context,
            isScrollControlled: true,
            useSafeArea: true,
            builder: (_) => const ServerPickerSheet(),
          );
        } else {
          context.push(profile == null ? '/import' : '/profile');
        }
      },
      child: Row(children: [
        Container(
          width: 44,
          height: 44,
          decoration: BoxDecoration(
            color: PulseColors.indigo.withValues(alpha: .16),
            borderRadius: BorderRadius.circular(PulseRadius.sm),
          ),
          child: Icon(
            profile == null ? Icons.add_link_rounded : Icons.route_rounded,
            color: PulseColors.teal,
          ),
        ),
        const SizedBox(width: PulseSpace.sm),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                route?.tag ?? profile?.name ?? 'Добавить VPN-профиль',
                style: Theme.of(context).textTheme.titleMedium,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              Text(
                profile == null
                    ? 'QR, ссылка или конфигурация'
                    : route == null
                        ? '${profile!.outboundsCount} маршрутов · sing-box'
                        : '${profile!.name} · ${route!.delayMs > 0 ? '${route!.delayMs} мс' : 'автомаршрут'}',
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            ],
          ),
        ),
        if (profile != null)
          const Icon(Icons.check_circle_rounded, color: PulseColors.success, size: 18),
        const SizedBox(width: PulseSpace.xs),
        const Icon(Icons.chevron_right_rounded, color: PulseColors.textSecondary),
      ]),
    );
  }
}

class _NavigationDock extends StatelessWidget {
  const _NavigationDock();

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(PulseRadius.pill),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 18, sigmaY: 18),
        child: Container(
          height: PulseSize.bottomNavHeight,
          decoration: BoxDecoration(color: PulseColors.surface, borderRadius: BorderRadius.circular(PulseRadius.pill), border: Border.all(color: Colors.white.withValues(alpha: .07))),
          child: Row(mainAxisAlignment: MainAxisAlignment.spaceEvenly, children: [
            _NavIcon(icon: Icons.graphic_eq_rounded, selected: true, onTap: () {}),
            _NavIcon(icon: Icons.bar_chart_rounded, onTap: () => context.push('/statistics')),
            _NavIcon(icon: Icons.dns_outlined, onTap: () => context.push('/servers')),
            _NavIcon(icon: Icons.tune_rounded, onTap: () => context.push('/settings')),
          ]),
        ),
      ),
    );
  }
}

class _NavIcon extends StatelessWidget {
  const _NavIcon({required this.icon, required this.onTap, this.selected = false});
  final IconData icon;
  final VoidCallback onTap;
  final bool selected;

  @override
  Widget build(BuildContext context) {
    return IconButton(
      onPressed: onTap,
      style: IconButton.styleFrom(backgroundColor: selected ? PulseColors.indigo.withValues(alpha: .18) : Colors.transparent),
      color: selected ? PulseColors.teal : PulseColors.textSecondary,
      icon: Icon(icon),
    );
  }
}
