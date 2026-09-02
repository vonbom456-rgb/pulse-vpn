import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_sing_box/flutter_sing_box.dart';
import 'package:pulse_vpn/core/vpn_engine/sing_box_runtime.dart';
import 'package:pulse_vpn/core/vpn_engine/vpn_engine.dart';

/// Production-адаптер Android: flutter_sing_box внутри поднимает VpnService.
class SingBoxVpnEngine implements VpnEngine {
  SingBoxVpnEngine(this._runtime);

  final SingBoxRuntime _runtime;
  final _snapshots = StreamController<VpnSnapshot>.broadcast();
  StreamSubscription<ProxyState>? _stateSubscription;
  StreamSubscription<ClientStatus>? _trafficSubscription;
  VpnStatus _status = VpnStatus.disconnected;
  ClientStatus? _traffic;
  int? _sessionBaseline;
  DateTime? _connectedAt;
  String? _coreVersion;
  bool _initialized = false;

  @override
  Stream<VpnSnapshot> get snapshots => _snapshots.stream;

  @override
  Future<void> initialize() async {
    if (_initialized) return;
    try {
      await _runtime.ensureInitialized();
      _coreVersion = await _runtime.client.getSingBoxVersion();
      _stateSubscription = _runtime.client.proxyStateStream.listen(
        _onProxyState,
        onError: _emitError,
      );
      _trafficSubscription = _runtime.client.connectedStatusStream.listen(
        _onTraffic,
        onError: _emitError,
      );
      _initialized = true;
      _emit();
    } catch (error) {
      _emitError(error);
    }
  }

  @override
  Future<void> connect() async {
    await initialize();
    if (!_initialized) return;
    if (ProfileStorage().getSelectedProfile() == null) {
      _emitError(const VpnConfigurationMissingException());
      return;
    }
    _sessionBaseline = null;
    _status = VpnStatus.connecting;
    _emit();
    try {
      await _runtime.client.startVpn();
    } catch (error) {
      _emitError(error);
    }
  }

  @override
  Future<void> disconnect() async {
    _status = VpnStatus.disconnecting;
    _emit();
    try {
      await _runtime.client.stopVpn();
    } catch (error) {
      _emitError(error);
    }
  }

  void _onProxyState(ProxyState state) {
    final previous = _status;
    _status = switch (state) {
      ProxyState.started => VpnStatus.connected,
      ProxyState.starting => VpnStatus.connecting,
      ProxyState.stopping => VpnStatus.disconnecting,
      ProxyState.stopped || ProxyState.unknown => VpnStatus.disconnected,
    };
    if (_status == VpnStatus.connected && previous != VpnStatus.connected) {
      _connectedAt = DateTime.now();
    } else if (_status == VpnStatus.disconnected) {
      _connectedAt = null;
    }
    if (_status == VpnStatus.connected && _traffic != null) {
      _sessionBaseline ??= _traffic!.uplinkTotal + _traffic!.downlinkTotal;
    }
    _emit();
  }

  void _onTraffic(ClientStatus traffic) {
    _traffic = traffic;
    if (_status == VpnStatus.connected) {
      _sessionBaseline ??= traffic.uplinkTotal + traffic.downlinkTotal;
    }
    _emit();
  }

  void _emitError(Object error) {
    _status = VpnStatus.error;
    _snapshots.add(VpnSnapshot(
      status: _status,
      errorMessage: _friendlyMessage(error),
      coreVersion: _coreVersion,
    ));
  }

  String _friendlyMessage(Object error) {
    if (error is VpnConfigurationMissingException) return error.toString();
    if (error is PlatformException) {
      return switch (error.code) {
        'VPN_PERMISSION_DENIED' => 'Разрешение на VPN отклонено',
        'NO_ACTIVITY' => 'Не удалось открыть системный запрос VPN',
        'VPN_ERROR' => error.message ?? 'sing-box не смог запустить туннель',
        _ => error.message ?? 'Ошибка VPN: ${error.code}',
      };
    }
    return 'Не удалось запустить VPN: $error';
  }

  void _emit() {
    final traffic = _traffic;
    final total = traffic == null ? 0 : traffic.uplinkTotal + traffic.downlinkTotal;
    _snapshots.add(VpnSnapshot(
      status: _status,
      downloadMbps: traffic == null ? 0 : traffic.downlink * 8 / 1000000,
      uploadMbps: traffic == null ? 0 : traffic.uplink * 8 / 1000000,
      sessionBytes: _sessionBaseline == null || total < _sessionBaseline!
          ? 0
          : total - _sessionBaseline!,
      sessionDuration: _connectedAt == null
          ? Duration.zero
          : DateTime.now().difference(_connectedAt!),
      coreVersion: _coreVersion,
    ));
  }

  @override
  Future<void> dispose() async {
    await _stateSubscription?.cancel();
    await _trafficSubscription?.cancel();
    await _snapshots.close();
  }
}
