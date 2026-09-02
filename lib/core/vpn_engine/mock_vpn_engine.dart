import 'dart:async';
import 'dart:math';

import 'package:pulse_vpn/core/vpn_engine/vpn_engine.dart';

class MockVpnEngine implements VpnEngine {
  final _controller = StreamController<VpnSnapshot>.broadcast();
  final _random = Random();
  Timer? _trafficTimer;
  int _bytes = 0;
  DateTime? _connectedAt;

  @override
  Stream<VpnSnapshot> get snapshots => _controller.stream;

  @override
  Future<void> initialize() async {
    _controller.add(const VpnSnapshot(status: VpnStatus.disconnected));
  }

  @override
  Future<void> connect() async {
    _trafficTimer?.cancel();
    _controller.add(const VpnSnapshot(status: VpnStatus.connecting));
    await Future<void>.delayed(const Duration(milliseconds: 1450));
    _connectedAt = DateTime.now();
    _controller.add(const VpnSnapshot(status: VpnStatus.connected));
    _trafficTimer = Timer.periodic(const Duration(milliseconds: 900), (_) {
      final down = 32 + _random.nextDouble() * 48;
      final up = 4 + _random.nextDouble() * 14;
      _bytes += ((down + up) * 112500).round();
      _controller.add(VpnSnapshot(
        status: VpnStatus.connected,
        downloadMbps: down,
        uploadMbps: up,
        sessionBytes: _bytes,
        sessionDuration: DateTime.now().difference(_connectedAt!),
      ));
    });
  }

  @override
  Future<void> disconnect() async {
    _controller.add(const VpnSnapshot(status: VpnStatus.disconnecting));
    await Future<void>.delayed(const Duration(milliseconds: 500));
    _trafficTimer?.cancel();
    _connectedAt = null;
    _controller.add(const VpnSnapshot(status: VpnStatus.disconnected));
  }

  @override
  Future<void> dispose() async {
    _trafficTimer?.cancel();
    await _controller.close();
  }
}
