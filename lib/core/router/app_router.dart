import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:pulse_vpn/core/theme/pulse_tokens.dart';
import 'package:pulse_vpn/features/connect/presentation/connect_screen.dart';
import 'package:pulse_vpn/features/import_subscription/presentation/import_screen.dart';
import 'package:pulse_vpn/features/onboarding/presentation/onboarding_screen.dart';
import 'package:pulse_vpn/features/profile/presentation/profile_screen.dart';
import 'package:pulse_vpn/features/servers/presentation/servers_screen.dart';
import 'package:pulse_vpn/features/settings/presentation/settings_screen.dart';
import 'package:pulse_vpn/features/statistics/presentation/statistics_screen.dart';

final appRouter = GoRouter(
  initialLocation: '/onboarding',
  routes: [
    GoRoute(path: '/onboarding', builder: (_, __) => const OnboardingScreen()),
    GoRoute(path: '/', builder: (_, __) => const ConnectScreen()),
    GoRoute(path: '/servers', pageBuilder: (_, state) => _page(state, const ServersScreen())),
    GoRoute(path: '/import', pageBuilder: (_, state) => _page(state, const ImportScreen())),
    GoRoute(path: '/statistics', pageBuilder: (_, state) => _page(state, const StatisticsScreen())),
    GoRoute(path: '/settings', pageBuilder: (_, state) => _page(state, const SettingsScreen())),
    GoRoute(path: '/profile', pageBuilder: (_, state) => _page(state, const ProfileScreen())),
  ],
);

CustomTransitionPage<void> _page(GoRouterState state, Widget child) {
  return CustomTransitionPage<void>(
    key: state.pageKey,
    transitionDuration: PulseMotion.standard,
    reverseTransitionDuration: PulseMotion.standard,
    child: child,
    transitionsBuilder: (_, animation, __, child) => FadeTransition(
      opacity: CurvedAnimation(parent: animation, curve: PulseMotion.routeCurve),
      child: SlideTransition(
        position: Tween(begin: const Offset(0, .025), end: Offset.zero).animate(
          CurvedAnimation(parent: animation, curve: PulseMotion.routeCurve),
        ),
        child: child,
      ),
    ),
  );
}

