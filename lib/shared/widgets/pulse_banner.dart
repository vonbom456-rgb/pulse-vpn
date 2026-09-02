import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:pulse_vpn/core/theme/pulse_tokens.dart';
import 'package:pulse_vpn/shared/widgets/glass_card.dart';

abstract final class PulseBanner {
  static void show(
    BuildContext context,
    String message, {
    bool success = false,
  }) {
    final overlay = Overlay.of(context);
    late final OverlayEntry entry;
    entry = OverlayEntry(
      builder: (context) => Positioned(
        top: MediaQuery.paddingOf(context).top + PulseSpace.sm,
        left: PulseSpace.page,
        right: PulseSpace.page,
        child: Material(
          color: Colors.transparent,
          child: GlassCard(
            child: Row(children: [
              Icon(
                success ? Icons.check_circle_outline_rounded : Icons.error_outline_rounded,
                color: success ? PulseColors.success : PulseColors.alert,
              ),
              const SizedBox(width: PulseSpace.sm),
              Expanded(child: Text(message)),
              IconButton(onPressed: entry.remove, icon: const Icon(Icons.close_rounded)),
            ]),
          ),
        )
            .animate()
            .fadeIn(duration: PulseMotion.quick)
            .slideY(begin: -.9, end: 0, duration: const Duration(milliseconds: 520), curve: Curves.elasticOut),
      ),
    );
    overlay.insert(entry);
    Future<void>.delayed(const Duration(seconds: 4), () {
      if (entry.mounted) entry.remove();
    });
  }
}
