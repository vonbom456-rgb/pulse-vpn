import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_sing_box/flutter_sing_box.dart';

/// Один runtime нужен и VPN-движку, и менеджеру профилей.
class SingBoxRuntime {
  SingBoxRuntime({FlutterSingBox? client}) : client = client ?? FlutterSingBox();

  final FlutterSingBox client;
  Future<void>? _initialization;

  Future<void> ensureInitialized() {
    return _initialization ??= _initializeGuarded();
  }

  Future<void> _initializeGuarded() async {
    try {
      // Нативный Activity появляется после первого кадра. Короткий retry также
      // закрывает возврат приложения из системного окна разрешения VPN.
      await WidgetsBinding.instance.endOfFrame;
      for (var attempt = 0; attempt < 5; attempt++) {
        try {
          await client.init();
          return;
        } on PlatformException catch (error) {
          if (error.code != 'NO_ACTIVITY' || attempt == 4) rethrow;
          await Future<void>.delayed(const Duration(milliseconds: 180));
        }
      }
    } catch (_) {
      // Ошибку нельзя кешировать навсегда: следующий тап должен повторить init.
      _initialization = null;
      rethrow;
    }
  }
}
