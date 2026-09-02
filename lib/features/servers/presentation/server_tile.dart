import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:pulse_vpn/core/theme/pulse_tokens.dart';
import 'package:pulse_vpn/core/vpn_engine/vpn_route_manager.dart';
import 'package:pulse_vpn/shared/widgets/glass_card.dart';

class ServerTile extends StatelessWidget {
  const ServerTile({
    required this.route,
    required this.index,
    required this.onSelected,
    super.key,
  });

  final VpnRoute route;
  final int index;
  final VoidCallback onSelected;

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      onTap: () async {
        await HapticFeedback.selectionClick();
        onSelected();
      },
      child: Row(children: [
        Container(
          width: 48,
          height: 48,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: PulseColors.indigo.withValues(alpha: .12),
            shape: BoxShape.circle,
          ),
          child: const Icon(Icons.language_rounded, color: PulseColors.teal),
        ),
        const SizedBox(width: PulseSpace.sm),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                route.tag,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.titleMedium,
              ),
              Text(
                route.type.toUpperCase(),
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            ],
          ),
        ),
        _PingBadge(ping: route.delayMs),
        const SizedBox(width: PulseSpace.sm),
        AnimatedContainer(
          duration: PulseMotion.quick,
          width: 22,
          height: 22,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: route.isSelected ? PulseColors.success : Colors.transparent,
            border: Border.all(
              color: route.isSelected
                  ? PulseColors.success
                  : PulseColors.textSecondary,
            ),
          ),
          child: route.isSelected
              ? const Icon(
                  Icons.check_rounded,
                  size: 15,
                  color: PulseColors.background,
                )
              : null,
        ),
      ]),
    )
        .animate()
        .fadeIn(
          delay: Duration(milliseconds: 50 * index),
          duration: PulseMotion.standard,
        )
        .slideY(begin: .08, end: 0, curve: PulseMotion.routeCurve);
  }
}

class _PingBadge extends StatelessWidget {
  const _PingBadge({required this.ping});
  final int ping;

  @override
  Widget build(BuildContext context) {
    if (ping <= 0) {
      return Text('— мс', style: Theme.of(context).textTheme.bodyMedium);
    }
    final color = ping < 80
        ? PulseColors.success
        : ping < 160
            ? PulseColors.teal
            : PulseColors.alert;
    return Row(children: [
      Icon(Icons.bolt_rounded, size: 16, color: color),
      Text(
        '$ping',
        style: Theme.of(context).textTheme.labelLarge?.copyWith(color: color),
      ),
      Text(' мс', style: Theme.of(context).textTheme.bodyMedium),
    ]);
  }
}

class ServerSkeleton extends StatelessWidget {
  const ServerSkeleton({super.key});

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      child: Row(children: [
        const _Bone(width: 48, height: 48, round: true),
        const SizedBox(width: PulseSpace.sm),
        const Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _Bone(width: 128, height: 14),
              SizedBox(height: PulseSpace.xs),
              _Bone(width: 82, height: 10),
            ],
          ),
        ),
        const _Bone(width: 54, height: 20),
      ]),
    )
        .animate(onPlay: (controller) => controller.repeat())
        .shimmer(
          duration: const Duration(milliseconds: 1200),
          color: Colors.white12,
        );
  }
}

class _Bone extends StatelessWidget {
  const _Bone({required this.width, required this.height, this.round = false});
  final double width;
  final double height;
  final bool round;

  @override
  Widget build(BuildContext context) => Container(
        width: width,
        height: height,
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: .08),
          borderRadius: BorderRadius.circular(
            round ? PulseRadius.pill : PulseRadius.sm,
          ),
        ),
      );
}
