import 'package:flutter/material.dart';
import 'package:pulse_vpn/core/theme/pulse_tokens.dart';

class AnimatedNumber extends StatelessWidget {
  const AnimatedNumber({
    required this.value,
    required this.builder,
    super.key,
    this.duration = PulseMotion.standard,
  });

  final double value;
  final Widget Function(BuildContext context, double value) builder;
  final Duration duration;

  @override
  Widget build(BuildContext context) {
    return TweenAnimationBuilder<double>(
      tween: Tween(end: value),
      duration: duration,
      curve: PulseMotion.routeCurve,
      builder: (context, value, child) => builder(context, value),
    );
  }
}
