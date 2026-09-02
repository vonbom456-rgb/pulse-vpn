import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:pulse_vpn/core/auth/pulse_session.dart';

class SessionStore {
  SessionStore({FlutterSecureStorage? storage})
      : _storage = storage ?? const FlutterSecureStorage();

  static const _accessKey = 'pulse_access_token';
  static const _refreshKey = 'pulse_refresh_token';
  final FlutterSecureStorage _storage;

  Future<PulseSession?> read() async {
    final values = await _storage.readAll();
    final access = values[_accessKey];
    final refresh = values[_refreshKey];
    return access == null || refresh == null
        ? null
        : PulseSession(accessToken: access, refreshToken: refresh);
  }

  Future<void> write(PulseSession session) async {
    await _storage.write(key: _accessKey, value: session.accessToken);
    await _storage.write(key: _refreshKey, value: session.refreshToken);
  }

  Future<void> clear() async {
    await _storage.delete(key: _accessKey);
    await _storage.delete(key: _refreshKey);
  }
}
