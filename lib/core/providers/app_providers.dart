import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:dio/dio.dart';
import 'package:pulse_vpn/core/network/subscription_client.dart';
import 'package:pulse_vpn/core/network/pulse_api_client.dart';
import 'package:pulse_vpn/core/auth/session_store.dart';
import 'package:pulse_vpn/core/storage/profile_repository.dart';
import 'package:pulse_vpn/core/vpn_engine/mock_vpn_engine.dart';
import 'package:pulse_vpn/core/vpn_engine/sing_box_runtime.dart';
import 'package:pulse_vpn/core/vpn_engine/sing_box_vpn_engine.dart';
import 'package:pulse_vpn/core/vpn_engine/unsupported_vpn_engine.dart';
import 'package:pulse_vpn/core/vpn_engine/vpn_engine.dart';
import 'package:pulse_vpn/core/vpn_engine/vpn_profile_manager.dart';
import 'package:pulse_vpn/core/vpn_engine/vpn_route_manager.dart';

final themeModeProvider = StateProvider<ThemeMode>((ref) => ThemeMode.dark);

final dioProvider = Provider<Dio>((ref) => Dio());
final pulseApiBaseUrlProvider = Provider<String>(
  (ref) => const String.fromEnvironment(
    'PULSE_API_URL',
    defaultValue: 'https://api.example.com',
  ),
);
final sessionStoreProvider = Provider<SessionStore>((ref) => SessionStore());
final pulseApiClientProvider = Provider<PulseApiClient>(
  (ref) => PulseApiClient(ref.watch(sessionStoreProvider)),
);
final subscriptionClientProvider = Provider<SubscriptionClient>(
  (ref) => SubscriptionClient(ref.watch(dioProvider)),
);
final profileRepositoryProvider = Provider<ProfileRepository>(
  (ref) => HiveProfileRepository(),
);

final singBoxRuntimeProvider = Provider<SingBoxRuntime>((ref) {
  return SingBoxRuntime();
});

final vpnProfileManagerProvider = Provider<VpnProfileManager>((ref) {
  return SingBoxProfileManager(ref.watch(singBoxRuntimeProvider));
});

final vpnRouteManagerProvider = Provider<VpnRouteManager>((ref) {
  return VpnRouteManager(ref.watch(singBoxRuntimeProvider));
});

final vpnRouteGroupsProvider = StreamProvider<List<VpnRouteGroup>>((ref) {
  return ref.watch(vpnRouteManagerProvider).watchGroups();
});

final vpnProfilesProvider =
    AsyncNotifierProvider<VpnProfilesController, List<VpnProfile>>(
  VpnProfilesController.new,
);

class VpnProfilesController extends AsyncNotifier<List<VpnProfile>> {
  VpnProfileManager get _manager => ref.read(vpnProfileManagerProvider);

  @override
  Future<List<VpnProfile>> build() => _manager.load();

  Future<VpnProfile> importSource(String source, {String? name}) async {
    state = const AsyncLoading();
    try {
      final profile = await _manager.import(source, name: name);
      state = AsyncData(await _manager.load());
      return profile;
    } catch (error, stackTrace) {
      state = AsyncError(error, stackTrace);
      rethrow;
    }
  }

  Future<void> select(int id) async {
    await _manager.select(id);
    state = AsyncData(await _manager.load());
  }

  Future<void> delete(int id) async {
    await _manager.delete(id);
    state = AsyncData(await _manager.load());
  }
}

final vpnEngineProvider = Provider<VpnEngine>((ref) {
  const useMock = bool.fromEnvironment('PULSE_USE_MOCK');
  final VpnEngine engine = useMock
      ? MockVpnEngine()
      : Platform.isAndroid
          ? SingBoxVpnEngine(ref.watch(singBoxRuntimeProvider))
          : UnsupportedVpnEngine();
  ref.onDispose(() => unawaited(engine.dispose()));
  return engine;
});

final vpnControllerProvider =
    StateNotifierProvider<VpnController, VpnSnapshot>((ref) {
  return VpnController(ref.watch(vpnEngineProvider));
});

final vpnSpeedHistoryProvider =
    StateNotifierProvider<VpnSpeedHistoryController, List<double>>((ref) {
  final controller = VpnSpeedHistoryController();
  ref.listen<VpnSnapshot>(vpnControllerProvider, (previous, next) {
    if (next.status == VpnStatus.connecting) controller.clear();
    if (next.status == VpnStatus.connected) {
      controller.add(next.downloadMbps);
    }
  });
  return controller;
});

class VpnSpeedHistoryController extends StateNotifier<List<double>> {
  VpnSpeedHistoryController() : super(const []);

  void add(double value) {
    final updated = [...state, value];
    state = updated.length > 60
        ? updated.sublist(updated.length - 60)
        : updated;
  }

  void clear() => state = const [];
}

class VpnController extends StateNotifier<VpnSnapshot> {
  VpnController(this._engine)
      : super(const VpnSnapshot(status: VpnStatus.disconnected)) {
    _subscription = _engine.snapshots.listen((snapshot) => state = snapshot);
    unawaited(_engine.initialize());
  }

  final VpnEngine _engine;
  late final StreamSubscription<VpnSnapshot> _subscription;

  Future<void> toggle() async {
    if (state.status == VpnStatus.connected) {
      await _engine.disconnect();
      return;
    }
    if (state.status == VpnStatus.disconnected ||
        state.status == VpnStatus.error) {
      await _engine.connect();
    }
  }

  @override
  void dispose() {
    _subscription.cancel();
    super.dispose();
  }
}
