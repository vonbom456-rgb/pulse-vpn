import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pulse_vpn/core/theme/pulse_tokens.dart';
import 'package:pulse_vpn/features/auth/application/auth_controller.dart';
import 'package:pulse_vpn/shared/widgets/glass_card.dart';
import 'package:pulse_vpn/shared/widgets/pulse_scaffold.dart';

class AuthScreen extends ConsumerStatefulWidget {
  const AuthScreen({super.key});

  @override
  ConsumerState<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends ConsumerState<AuthScreen> {
  final email = TextEditingController();
  final code = TextEditingController();
  var codeSent = false;

  @override
  void dispose() {
    email.dispose();
    code.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final auth = ref.watch(authControllerProvider);
    ref.listen(authControllerProvider, (_, next) {
      if (next.hasValue && next.value != null && context.mounted) context.pop();
    });
    return PulseScaffold(
      title: 'Аккаунт Pulse',
      child: ListView(children: [
        Text('Ваша подписка — внутри приложения', style: Theme.of(context).textTheme.headlineMedium),
        const SizedBox(height: PulseSpace.xs),
        Text(
          'Telegram нужен только для безопасного входа. Pulse user ID и VPN-подписка не зависят от Telegram.',
          style: Theme.of(context).textTheme.bodyMedium,
        ),
        const SizedBox(height: PulseSpace.lg),
        FilledButton.icon(
          onPressed: auth.isLoading
              ? null
              : () => ref.read(authControllerProvider.notifier).openTelegram(),
          icon: const Icon(Icons.send_rounded),
          label: const Text('Продолжить через Telegram'),
        ),
        const SizedBox(height: PulseSpace.lg),
        Row(children: [const Expanded(child: Divider()), Padding(
          padding: const EdgeInsets.symmetric(horizontal: PulseSpace.sm),
          child: Text('ИЛИ', style: Theme.of(context).textTheme.labelLarge),
        ), const Expanded(child: Divider())]),
        const SizedBox(height: PulseSpace.lg),
        GlassCard(child: Column(children: [
          TextField(
            controller: email,
            keyboardType: TextInputType.emailAddress,
            autocorrect: false,
            decoration: const InputDecoration(labelText: 'Email'),
          ),
          if (codeSent) ...[
            const SizedBox(height: PulseSpace.sm),
            TextField(
              controller: code,
              keyboardType: TextInputType.number,
              maxLength: 6,
              decoration: const InputDecoration(labelText: 'Код из письма'),
            ),
          ],
          const SizedBox(height: PulseSpace.md),
          SizedBox(width: double.infinity, child: OutlinedButton(
            onPressed: auth.isLoading ? null : _emailAction,
            child: Text(codeSent ? 'Войти' : 'Получить код'),
          )),
        ])),
        if (auth.hasError) ...[
          const SizedBox(height: PulseSpace.sm),
          Text('Не удалось войти. Проверьте данные и повторите.', style: TextStyle(color: Theme.of(context).colorScheme.error)),
        ],
      ]),
    );
  }

  Future<void> _emailAction() async {
    if (!codeSent) {
      await ref.read(authControllerProvider.notifier).requestEmailCode(email.text);
      if (mounted) setState(() => codeSent = true);
      return;
    }
    await ref.read(authControllerProvider.notifier).verifyEmail(email.text, code.text);
  }
}
