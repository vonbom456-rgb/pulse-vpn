import 'package:flutter_test/flutter_test.dart';
import 'package:pulse_vpn/core/vpn_engine/mock_vpn_engine.dart';
import 'package:pulse_vpn/core/vpn_engine/vpn_engine.dart';

void main() {
  test('mock engine проходит connecting → connected', () async {
    final engine = MockVpnEngine();
    final states = <VpnStatus>[];
    final subscription = engine.snapshots.listen((event) => states.add(event.status));
    await engine.initialize();
    await engine.connect();
    await Future<void>.delayed(Duration.zero);
    expect(states, containsAllInOrder([VpnStatus.disconnected, VpnStatus.connecting, VpnStatus.connected]));
    await subscription.cancel();
    await engine.dispose();
  });
}
