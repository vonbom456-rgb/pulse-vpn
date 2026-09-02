import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:pulse_vpn/core/theme/pulse_tokens.dart';
import 'package:pulse_vpn/core/vpn_engine/vpn_engine.dart';

class PulseConnectButton extends StatefulWidget {
  const PulseConnectButton({
    required this.status,
    required this.onPressed,
    this.isConfigured = true,
    super.key,
  });

  final VpnStatus status;
  final VoidCallback onPressed;
  final bool isConfigured;

  @override
  State<PulseConnectButton> createState() => _PulseConnectButtonState();
}

class _PulseConnectButtonState extends State<PulseConnectButton>
    with TickerProviderStateMixin {
  late final AnimationController _heartbeat;
  late final AnimationController _press;
  late final AnimationController _wave;

  bool get _active => widget.status == VpnStatus.connected || widget.status == VpnStatus.connecting;

  @override
  void initState() {
    super.initState();
    _heartbeat = AnimationController(vsync: this, duration: const Duration(milliseconds: 1320));
    _press = AnimationController(vsync: this, duration: PulseMotion.quick);
    _wave = AnimationController(vsync: this, duration: PulseMotion.wave);
    if (_active) _heartbeat.repeat();
  }

  @override
  void didUpdateWidget(covariant PulseConnectButton oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.status != widget.status) {
      _wave.forward(from: 0);
      _heartbeat.duration = widget.status == VpnStatus.connecting
          ? const Duration(milliseconds: 760)
          : const Duration(milliseconds: 1320);
      if (_active) {
        _heartbeat.repeat();
      } else {
        _heartbeat.stop();
        _heartbeat.value = 0;
      }
    }
  }

  double _beat(double t) {
    double bump(double center, double width, double amplitude) {
      final x = (t - center) / width;
      return amplitude * math.exp(-x * x);
    }
    return bump(.18, .055, 1) + bump(.31, .075, .58) + bump(.68, .14, .18);
  }

  Future<void> _tap() async {
    if (widget.status == VpnStatus.connecting ||
        widget.status == VpnStatus.disconnecting) {
      return;
    }
    await HapticFeedback.mediumImpact();
    _press.forward().then((_) => _press.reverse());
    widget.onPressed();
  }

  @override
  void dispose() {
    _heartbeat.dispose();
    _press.dispose();
    _wave.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final connected = widget.status == VpnStatus.connected;
    return SizedBox.square(
      dimension: PulseSize.connectStage,
      child: AnimatedBuilder(
        animation: Listenable.merge([_heartbeat, _press, _wave]),
        builder: (context, _) {
          final beat = _active ? _beat(_heartbeat.value) : 0.0;
          final pressedScale = 1 - Curves.easeOut.transform(_press.value) * .04;
          return Stack(
            alignment: Alignment.center,
            children: [
              if (_wave.isAnimating)
                Transform.scale(
                  scale: 1 + Curves.easeOutCubic.transform(_wave.value) * 1.25,
                  child: Opacity(
                    opacity: (1 - _wave.value) * .34,
                    child: Container(
                      width: PulseSize.connectButton,
                      height: PulseSize.connectButton,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        border: Border.all(color: connected ? PulseColors.success : PulseColors.indigo, width: 2),
                      ),
                    ),
                  ),
                ),
              for (var index = 0; index < 3; index++)
                Transform.scale(
                  scale: 1.18 + index * .22 + beat * (.035 + index * .014),
                  child: Opacity(
                    opacity: _active ? .20 - index * .045 : .06,
                    child: Container(
                      width: PulseSize.connectButton,
                      height: PulseSize.connectButton,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        border: Border.all(
                          color: connected ? PulseColors.success : PulseColors.indigo,
                          width: 1.2,
                        ),
                      ),
                    ),
                  ),
                ),
              Transform.scale(
                scale: pressedScale * (1 + beat * .015),
                child: GestureDetector(
                  onTap: _tap,
                  child: AnimatedContainer(
                    duration: PulseMotion.standard,
                    width: PulseSize.connectButton,
                    height: PulseSize.connectButton,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: connected ? PulseColors.successGradient : PulseColors.pulseGradient,
                      boxShadow: PulseShadows.glow(
                        connected ? PulseColors.success : PulseColors.indigo,
                        strength: _active ? .38 : .18,
                      ),
                    ),
                    child: Center(
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(_icon, size: 32, color: Colors.white),
                          const SizedBox(height: PulseSpace.xs),
                          Text(
                            _label,
                            style: Theme.of(context).textTheme.labelLarge?.copyWith(color: Colors.white),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ],
          );
        },
      ),
    );
  }

  IconData get _icon => switch (widget.status) {
        VpnStatus.connected => Icons.graphic_eq_rounded,
        VpnStatus.connecting || VpnStatus.disconnecting => Icons.more_horiz_rounded,
        _ => widget.isConfigured
            ? Icons.power_settings_new_rounded
            : Icons.add_link_rounded,
      };

  String get _label => switch (widget.status) {
        VpnStatus.connected => 'В ПУЛЬСЕ',
        VpnStatus.connecting => 'СОЕДИНЯЕМ',
        VpnStatus.disconnecting => 'ЗАВЕРШАЕМ',
        _ => widget.isConfigured ? 'ПОДКЛЮЧИТЬ' : 'ДОБАВИТЬ КЛЮЧ',
      };
}
