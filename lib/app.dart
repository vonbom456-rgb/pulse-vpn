import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pulse_vpn/core/providers/app_providers.dart';
import 'package:pulse_vpn/core/router/app_router.dart';
import 'package:pulse_vpn/core/theme/pulse_theme.dart';

class PulseApp extends ConsumerWidget {
  const PulseApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final themeMode = ref.watch(themeModeProvider);
    return MaterialApp.router(
      title: 'Pulse VPN',
      debugShowCheckedModeBanner: false,
      theme: PulseTheme.light,
      darkTheme: PulseTheme.dark,
      themeMode: themeMode,
      routerConfig: appRouter,
    );
  }
}

