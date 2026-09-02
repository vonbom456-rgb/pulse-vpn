import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pulse_vpn/core/providers/app_providers.dart';
import 'package:pulse_vpn/core/theme/pulse_tokens.dart';
import 'package:pulse_vpn/shared/widgets/glass_card.dart';
import 'package:pulse_vpn/shared/widgets/pulse_scaffold.dart';

class SettingsScreen extends ConsumerStatefulWidget {
  const SettingsScreen({super.key});

  @override
  ConsumerState<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends ConsumerState<SettingsScreen> {
  @override
  Widget build(BuildContext context) {
    final dark = ref.watch(themeModeProvider) == ThemeMode.dark;
    return PulseScaffold(
      title: 'Настройки',
      child: ListView(children: [
        _Section(title: 'Внешний вид', children: [
          _SwitchRow(
            icon: Icons.dark_mode_outlined,
            label: 'Тёмная тема',
            value: dark,
            onChanged: (value) => ref.read(themeModeProvider.notifier).state = value ? ThemeMode.dark : ThemeMode.light,
          ),
        ]),
        const SizedBox(height: PulseSpace.lg),
        GlassCard(
          child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Icon(Icons.memory_rounded, color: PulseColors.teal),
            const SizedBox(width: PulseSpace.sm),
            Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text('Локальный режим', style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: PulseSpace.xs),
              Text('Профили хранятся на устройстве. Протокол, DNS и маршрутизация берутся из импортированной конфигурации sing-box.', style: Theme.of(context).textTheme.bodyMedium),
            ])),
          ]),
        ),
      ]),
    );
  }
}

class _Section extends StatelessWidget {
  const _Section({required this.title, required this.children});
  final String title;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    Padding(padding: const EdgeInsets.only(left: PulseSpace.xs, bottom: PulseSpace.xs), child: Text(title.toUpperCase(), style: Theme.of(context).textTheme.labelLarge?.copyWith(color: PulseColors.textSecondary, letterSpacing: 1.2))),
    GlassCard(padding: EdgeInsets.zero, child: Column(children: [for (var i = 0; i < children.length; i++) ...[children[i], if (i != children.length - 1) const Divider(height: 1, indent: 56)]])),
  ]);
}

class _SwitchRow extends StatelessWidget {
  const _SwitchRow({required this.icon, required this.label, required this.value, required this.onChanged});
  final IconData icon;
  final String label;
  final bool value;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) => SwitchListTile(
    secondary: Icon(icon, color: PulseColors.teal),
    title: Text(label),
    value: value,
    activeThumbColor: PulseColors.success,
    onChanged: onChanged,
  );
}
