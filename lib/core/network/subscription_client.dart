import 'package:dio/dio.dart';

class SubscriptionClient {
  SubscriptionClient(this._dio);

  final Dio _dio;

  Future<String> download(String url) async {
    final response = await _dio.get<String>(
      url,
      options: Options(
        responseType: ResponseType.plain,
        receiveTimeout: const Duration(seconds: 12),
      ),
    );
    final payload = response.data;
    if (payload == null || payload.trim().isEmpty) {
      throw const FormatException('Сервер вернул пустую подписку');
    }
    return payload;
  }
}

