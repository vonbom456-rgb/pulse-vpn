import 'package:dio/dio.dart';
import 'package:pulse_vpn/core/auth/pulse_session.dart';
import 'package:pulse_vpn/core/auth/session_store.dart';

class PulseApiClient {
  PulseApiClient(this._store)
      : _dio = Dio(BaseOptions(
          baseUrl: const String.fromEnvironment(
            'PULSE_API_URL',
            defaultValue: 'https://api.example.com',
          ),
          connectTimeout: const Duration(seconds: 8),
          receiveTimeout: const Duration(seconds: 12),
          headers: const {'Accept': 'application/json'},
        ));

  final SessionStore _store;
  final Dio _dio;

  Future<Response<T>> get<T>(String path, {ResponseType? responseType}) {
    return _authorized<T>(
      (token) => _dio.get<T>(
        path,
        options: Options(
          headers: {'Authorization': 'Bearer $token'},
          responseType: responseType,
        ),
      ),
    );
  }

  Future<Response<T>> post<T>(String path, {Object? data}) {
    return _authorized<T>(
      (token) => _dio.post<T>(
        path,
        data: data,
        options: Options(headers: {'Authorization': 'Bearer $token'}),
      ),
    );
  }

  Future<Response<T>> delete<T>(String path) {
    return _authorized<T>(
      (token) => _dio.delete<T>(
        path,
        options: Options(headers: {'Authorization': 'Bearer $token'}),
      ),
    );
  }

  Future<PulseSession> exchangeTelegram(String code) =>
      _sessionRequest('/api/auth/exchange', {'code': code});

  Future<void> requestEmailCode(String email) async {
    await _dio.post<void>('/api/auth/email/request', data: {'email': email});
  }

  Future<PulseSession> verifyEmail(String email, String code) =>
      _sessionRequest('/api/auth/email/verify', {'email': email, 'code': code});

  Future<PulseSession> _sessionRequest(String path, Object data) async {
    final response = await _dio.post<Map<String, dynamic>>(path, data: data);
    final session = PulseSession(
      accessToken: response.data!['accessToken'] as String,
      refreshToken: response.data!['refreshToken'] as String,
    );
    await _store.write(session);
    return session;
  }

  Future<Response<T>> _authorized<T>(
    Future<Response<T>> Function(String token) request,
  ) async {
    var session = await _store.read();
    if (session == null) throw const PulseAuthException();
    try {
      return await request(session.accessToken);
    } on DioException catch (error) {
      if (error.response?.statusCode != 401) rethrow;
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/auth/refresh',
        data: {'refreshToken': session.refreshToken},
      );
      session = PulseSession(
        accessToken: response.data!['accessToken'] as String,
        refreshToken: response.data!['refreshToken'] as String,
      );
      await _store.write(session);
      return request(session.accessToken);
    }
  }
}

class PulseAuthException implements Exception {
  const PulseAuthException();
}
