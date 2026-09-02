import 'dart:async';

import 'package:pulse_vpn/core/vpn_engine/vpn_engine.dart';

class UnsupportedVpnEngine implements VpnEngine {
  final _snapshots = StreamController<VpnSnapshot>.broadcast();

  @override
  Stream<VpnSnapshot> get snapshots => _snapshots.stream;

  @override
  Future<void> initialize() async {
    _snapshots.add(const VpnSnapshot(status: VpnStatus.disconnected));
  }

  @override
  Future<void> connect() async {
    _snapshots.add(const VpnSnapshot(
      status: VpnStatus.error,
      errorMessage: 'Реальный VPN-адаптер сейчас доступен только на Android',
    ));
  }

  @override
  Future<void> disconnect() async {}

  @override
  Future<void> dispose() => _snapshots.close();
}
