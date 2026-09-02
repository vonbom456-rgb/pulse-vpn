import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pulse_vpn/core/providers/app_providers.dart';
import 'package:pulse_vpn/core/theme/pulse_tokens.dart';
import 'package:pulse_vpn/shared/widgets/animated_number.dart';
import 'package:pulse_vpn/shared/widgets/glass_card.dart';
import 'package:pulse_vpn/shared/widgets/pulse_scaffold.dart';

class StatisticsScreen extends ConsumerWidget {
  const StatisticsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final vpn = ref.watch(vpnControllerProvider);
    final history = ref.watch(vpnSpeedHistoryProvider);
    return PulseScaffold(
      title: 'Статистика',
      child: ListView(children: [
        Row(children: [
          Expanded(child: _Stat(value: vpn.downloadMbps, suffix: ' Мбит/с', label: 'Загрузка')),
          const SizedBox(width: PulseSpace.sm),
          Expanded(child: _Stat(value: vpn.uploadMbps, suffix: ' Мбит/с', label: 'Отдача')),
        ]),
        const SizedBox(height: PulseSpace.sm),
        GlassCard(
          padding: const EdgeInsets.fromLTRB(PulseSpace.md, PulseSpace.lg, PulseSpace.md, PulseSpace.sm),
          child: SizedBox(height: 240, child: LineChart(_chart(history))),
        ),
        const SizedBox(height: PulseSpace.sm),
        GlassCard(child: Column(children: [
          _DataRow(label: 'За сессию', value: _bytes(vpn.sessionBytes)),
          const Divider(height: PulseSpace.xl),
          _DataRow(label: 'Время подключения', value: _duration(vpn.sessionDuration)),
          const Divider(height: PulseSpace.xl),
          _DataRow(label: 'Ядро', value: vpn.coreVersion ?? 'sing-box'),
        ])),
      ]),
    );
  }

  LineChartData _chart(List<double> history) {
    final values = history.isEmpty ? const [0.0] : history;
    final points = values
        .asMap()
        .entries
        .map((entry) => FlSpot(entry.key.toDouble(), entry.value))
        .toList(growable: false);
    final peak = values.reduce((a, b) => a > b ? a : b);
    return LineChartData(
      minY: 0,
      maxY: peak < 10 ? 10 : peak * 1.2,
      gridData: FlGridData(show: true, drawVerticalLine: false, getDrawingHorizontalLine: (_) => const FlLine(color: PulseColors.divider, strokeWidth: 1)),
      titlesData: const FlTitlesData(show: false),
      borderData: FlBorderData(show: false),
      lineTouchData: LineTouchData(enabled: true),
      lineBarsData: [LineChartBarData(spots: points, isCurved: true, barWidth: 3, color: PulseColors.teal, dotData: const FlDotData(show: false), belowBarData: BarAreaData(show: true, gradient: LinearGradient(begin: Alignment.topCenter, end: Alignment.bottomCenter, colors: [PulseColors.teal.withValues(alpha: .28), Colors.transparent])))],
    );
  }

  String _bytes(int value) => value < 1000000 ? '${(value / 1000).toStringAsFixed(0)} КБ' : '${(value / 1000000).toStringAsFixed(1)} МБ';

  String _duration(Duration value) {
    final hours = value.inHours.toString().padLeft(2, '0');
    final minutes = (value.inMinutes % 60).toString().padLeft(2, '0');
    final seconds = (value.inSeconds % 60).toString().padLeft(2, '0');
    return '$hours:$minutes:$seconds';
  }
}

class _Stat extends StatelessWidget {
  const _Stat({required this.value, required this.suffix, required this.label});
  final double value;
  final String suffix;
  final String label;

  @override
  Widget build(BuildContext context) => GlassCard(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    AnimatedNumber(value: value, builder: (_, number) => Text('${number.toStringAsFixed(1)}$suffix', style: Theme.of(context).textTheme.titleMedium)),
    const SizedBox(height: PulseSpace.xxs),
    Text(label, style: Theme.of(context).textTheme.bodyMedium),
  ]));
}

class _DataRow extends StatelessWidget {
  const _DataRow({required this.label, required this.value});
  final String label;
  final String value;
  @override
  Widget build(BuildContext context) => Row(children: [Text(label, style: Theme.of(context).textTheme.bodyMedium), const Spacer(), Text(value, style: Theme.of(context).textTheme.titleMedium)]);
}
