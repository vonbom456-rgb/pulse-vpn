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
  var autoConnect = true;
  var killSwitch = true;
  var protocol = 'VLESS · Reality';

  @override
  Widget build(BuildContext context) {
    final dark = ref.watch(themeModeProvider) == ThemeMode.dark;
    return PulseScaffold(
      title: 'Настройки',
      child: ListView(children: [
        _Section(title: 'Соединение', children: [
          _SelectRow(icon: Icons.route_rounded, label: 'Протокол', value: protocol, onTap: _pickProtocol),
          _SwitchRow(icon: Icons.wifi_tethering_rounded, label: 'Автоподключение', value: autoConnect, onChanged: (value) => setState(() => autoConnect = value)),
          _SwitchRow(icon: Icons.pause_circle_outline_rounded, label: 'Kill switch', value: killSwitch, onChanged: (value) => setState(() => killSwitch = value)),
          _SelectRow(icon: Icons.call_split_rounded, label: 'Split tunneling', value: '8 приложений', onTap: () {}),
        ]),
        const SizedBox(height: PulseSpace.lg),
        _Section(title: 'Внешний вид', children: [
          _SwitchRow(
            icon: Icons.dark_mode_outlined,
            label: 'Тёмная тема',
            value: dark,
            onChanged: (value) => ref.read(themeModeProvider.notifier).state = value ? ThemeMode.dark : ThemeMode.light,
          ),
          _SelectRow(icon: Icons.vibration_rounded, label: 'Хаптик', value: 'Системный', onTap: () {}),
        ]),
        const SizedBox(height: PulseSpace.lg),
        _Section(title: 'Система', children: [
          _SelectRow(icon: Icons.dns_outlined, label: 'DNS', value: 'Auto · DoH', onTap: () {}),
          _SelectRow(icon: Icons.description_outlined, label: 'Журнал', value: 'Ошибки', onTap: () {}),
        ]),
      ]),
    );
  }

  Future<void> _pickProtocol() async {
    final result = await showModalBottomSheet<String>(
      context: context,
      builder: (context) => SafeArea(child: Padding(
        padding: const EdgeInsets.all(PulseSpace.page),
        child: Column(mainAxisSize: MainAxisSize.min, children: ['VLESS · Reality', 'VLESS · WS · TLS', 'Trojan · TLS'].map((item) => ListTile(title: Text(item), trailing: item == protocol ? const Icon(Icons.check_rounded, color: PulseColors.success) : null, onTap: () => Navigator.of(context).pop(item))).toList()),
      )),
    );
    if (result != null) setState(() => protocol = result);
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

class _SelectRow extends StatelessWidget {
  const _SelectRow({required this.icon, required this.label, required this.value, required this.onTap});
  final IconData icon;
  final String label;
  final String value;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => ListTile(
    onTap: onTap,
    leading: Icon(icon, color: PulseColors.teal),
    title: Text(label),
    trailing: Row(mainAxisSize: MainAxisSize.min, children: [Text(value, style: Theme.of(context).textTheme.bodyMedium), const SizedBox(width: PulseSpace.xs), const Icon(Icons.chevron_right_rounded, color: PulseColors.textSecondary)]),
  );
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
