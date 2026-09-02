import 'package:flutter_sing_box/flutter_sing_box.dart';

/// Один runtime нужен и VPN-движку, и менеджеру профилей.
class SingBoxRuntime {
  SingBoxRuntime({FlutterSingBox? client}) : client = client ?? FlutterSingBox();

  final FlutterSingBox client;
  Future<void>? _initialization;

  Future<void> ensureInitialized() {
    return _initialization ??= client.init();
  }
}
