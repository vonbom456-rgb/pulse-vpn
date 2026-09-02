import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:pulse_vpn/core/providers/app_providers.dart';
import 'package:pulse_vpn/core/theme/pulse_tokens.dart';
import 'package:pulse_vpn/shared/widgets/glass_card.dart';
import 'package:pulse_vpn/shared/widgets/pulse_banner.dart';
import 'package:pulse_vpn/shared/widgets/pulse_scaffold.dart';

class ImportScreen extends ConsumerWidget {
  const ImportScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profiles = ref.watch(vpnProfilesProvider);
    final importing = profiles.isLoading;

    return PulseScaffold(
      title: 'Добавить подписку',
      child: Stack(
        children: [
          ListView(children: [
            _ImportOption(
              icon: Icons.qr_code_scanner_rounded,
              title: 'Сканировать QR',
              subtitle: 'Ссылка подписки, VLESS или sing-box JSON',
              onTap: importing ? null : () => _scan(context, ref),
            ),
            const SizedBox(height: PulseSpace.sm),
            _ImportOption(
              icon: Icons.link_rounded,
              title: 'Вставить ссылку',
              subtitle: 'HTTPS-ссылка от вашего VPN-провайдера',
              onTap: importing ? null : () => _openEditor(context, ref),
            ),
            const SizedBox(height: PulseSpace.sm),
            _ImportOption(
              icon: Icons.tune_rounded,
              title: 'Вставить конфигурацию',
              subtitle: 'VLESS URI или полный sing-box JSON',
              onTap: importing
                  ? null
                  : () => _openEditor(context, ref, rawConfig: true),
            ),
            const SizedBox(height: PulseSpace.xl),
            GlassCard(
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(Icons.key_rounded, color: PulseColors.teal),
                  const SizedBox(width: PulseSpace.sm),
                  Expanded(
                    child: Text(
                      'Конфигурация обрабатывается локально ядром sing-box. Pulse не отправляет VPN-ключи на свои серверы.',
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                  ),
                ],
              ),
            ),
          ]),
          if (importing)
            const Positioned.fill(
              child: ColoredBox(
                color: Color(0x660B0C10),
                child: Center(child: CircularProgressIndicator()),
              ),
            ),
        ],
      ),
    );
  }

  Future<void> _scan(BuildContext context, WidgetRef ref) async {
    final source = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (_) => const _ScannerSheet(),
    );
    if (source != null && context.mounted) {
      await _import(context, ref, source);
    }
  }

  Future<void> _openEditor(
    BuildContext context,
    WidgetRef ref, {
    bool rawConfig = false,
  }) async {
    final clipboard = await Clipboard.getData(Clipboard.kTextPlain);
    if (!context.mounted) return;
    final controller = TextEditingController(text: clipboard?.text ?? '');
    final source = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(rawConfig ? 'Конфигурация' : 'Ссылка подписки'),
        content: TextField(
          controller: controller,
          autofocus: true,
          minLines: rawConfig ? 5 : 1,
          maxLines: rawConfig ? 10 : 3,
          decoration: InputDecoration(
            hintText: rawConfig ? 'vless://… или { "inbounds": … }' : 'https://…',
          ),
        ),
        actions: [
          TextButton(
            onPressed: Navigator.of(context).pop,
            child: const Text('Отмена'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(controller.text),
            child: const Text('Импортировать'),
          ),
        ],
      ),
    );
    controller.dispose();
    if (source != null && context.mounted) {
      await _import(context, ref, source);
    }
  }

  Future<void> _import(BuildContext context, WidgetRef ref, String source) async {
    try {
      final profile = await ref
          .read(vpnProfilesProvider.notifier)
          .importSource(source);
      await HapticFeedback.mediumImpact();
      if (!context.mounted) return;
      PulseBanner.show(
        context,
        '${profile.name}: ${profile.outboundsCount} маршрутов добавлено',
        success: true,
      );
      Navigator.of(context).maybePop();
    } catch (error) {
      if (!context.mounted) return;
      PulseBanner.show(context, _friendlyImportError(error));
    }
  }

  String _friendlyImportError(Object error) {
    final message = error.toString().replaceFirst('Exception: ', '');
    if (message.contains('FormatException')) {
      return message.replaceFirst('FormatException: ', '');
    }
    if (message.contains('Invalid state') || message.contains('MMKV')) {
      return 'Хранилище профилей не готово. Перезапустите приложение.';
    }
    if (message.contains('DioException') || message.contains('SocketException')) {
      return 'Не удалось загрузить подписку. Проверьте ссылку и интернет.';
    }
    return 'Ссылка или конфигурация не распознана sing-box.';
  }
}

class _ImportOption extends StatelessWidget {
  const _ImportOption({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) => Opacity(
        opacity: onTap == null ? .5 : 1,
        child: GlassCard(
          onTap: onTap,
          child: Row(children: [
            Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                gradient: PulseColors.pulseGradient,
                borderRadius: BorderRadius.circular(PulseRadius.sm),
              ),
              child: Icon(icon, color: Colors.white),
            ),
            const SizedBox(width: PulseSpace.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: Theme.of(context).textTheme.titleMedium),
                  Text(subtitle, style: Theme.of(context).textTheme.bodyMedium),
                ],
              ),
            ),
            const Icon(Icons.chevron_right_rounded, color: PulseColors.textSecondary),
          ]),
        ),
      );
}

class _ScannerSheet extends StatefulWidget {
  const _ScannerSheet();

  @override
  State<_ScannerSheet> createState() => _ScannerSheetState();
}

class _ScannerSheetState extends State<_ScannerSheet> {
  bool _handled = false;

  @override
  Widget build(BuildContext context) => FractionallySizedBox(
        heightFactor: .88,
        child: ClipRRect(
          borderRadius: const BorderRadius.vertical(
            top: Radius.circular(PulseRadius.lg),
          ),
          child: Stack(children: [
            MobileScanner(
              onDetect: (capture) {
                final value = capture.barcodes.isEmpty
                    ? null
                    : capture.barcodes.first.rawValue;
                if (!_handled && value != null && value.trim().isNotEmpty) {
                  _handled = true;
                  Navigator.of(context).pop(value);
                }
              },
            ),
            Positioned(
              top: PulseSpace.md,
              left: PulseSpace.md,
              child: IconButton.filled(
                onPressed: Navigator.of(context).pop,
                icon: const Icon(Icons.close_rounded),
              ),
            ),
            Center(
              child: Container(
                width: 240,
                height: 240,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(PulseRadius.lg),
                  border: Border.all(color: PulseColors.teal, width: 2),
                ),
              ),
            ),
            Positioned(
              left: 0,
              right: 0,
              bottom: PulseSpace.xl,
              child: Text(
                'Наведите камеру на QR-код',
                textAlign: TextAlign.center,
                style: Theme.of(context)
                    .textTheme
                    .titleMedium
                    ?.copyWith(color: Colors.white),
              ),
            ),
          ]),
        ),
      );
}
