import 'dart:async';

import 'package:app_links/app_links.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pulse_vpn/core/auth/pulse_session.dart';
import 'package:pulse_vpn/core/providers/app_providers.dart';
import 'package:url_launcher/url_launcher.dart';

final authControllerProvider =
    AsyncNotifierProvider<AuthController, PulseSession?>(AuthController.new);

class AuthController extends AsyncNotifier<PulseSession?> {
  StreamSubscription<Uri>? _links;

  @override
  Future<PulseSession?> build() async {
    _links = AppLinks().uriLinkStream.listen(_handleLink);
    ref.onDispose(() => _links?.cancel());
    return ref.read(sessionStoreProvider).read();
  }

  Future<void> openTelegram() async {
    final baseUrl = ref.read(pulseApiBaseUrlProvider);
    final uri = Uri.parse('$baseUrl/api/auth/telegram/start');
    if (!await launchUrl(uri, mode: LaunchMode.externalApplication)) {
      throw StateError('Не удалось открыть Telegram-вход');
    }
  }

  Future<void> requestEmailCode(String email) {
    return ref.read(pulseApiClientProvider).requestEmailCode(email.trim());
  }

  Future<void> verifyEmail(String email, String code) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      () => ref.read(pulseApiClientProvider).verifyEmail(email.trim(), code.trim()),
    );
  }

  Future<void> signOut() async {
    await ref.read(sessionStoreProvider).clear();
    state = const AsyncData(null);
  }

  Future<void> _handleLink(Uri uri) async {
    if (uri.scheme != 'pulsevpn' || uri.host != 'auth') return;
    final code = uri.queryParameters['code'];
    if (code == null) return;
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      () => ref.read(pulseApiClientProvider).exchangeTelegram(code),
    );
  }
}
