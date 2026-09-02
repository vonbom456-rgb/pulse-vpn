import 'package:flutter/material.dart';

abstract final class PulseColors {
  static const background = Color(0xFF0B0C10);
  static const backgroundLight = Color(0xFFF4F5F8);
  static const surface = Color(0xB315171E);
  static const surfaceLight = Color(0xCCFFFFFF);
  static const surfaceRaised = Color(0xFF1B1E27);
  static const indigo = Color(0xFF6C5CE7);
  static const teal = Color(0xFF00D9C0);
  static const success = Color(0xFF00E6A0);
  static const alert = Color(0xFFFF5C7A);
  static const textPrimary = Color(0xFFF5F6FA);
  static const textSecondary = Color(0xFF9198A8);
  static const divider = Color(0x1FFFFFFF);

  static const pulseGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [indigo, teal],
  );
  static const successGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [Color(0xFF19C8A1), success],
  );
}

abstract final class PulseSpace {
  static const xxs = 4.0;
  static const xs = 8.0;
  static const sm = 12.0;
  static const md = 16.0;
  static const lg = 24.0;
  static const xl = 32.0;
  static const xxl = 48.0;
  static const page = 20.0;
}

abstract final class PulseRadius {
  static const sm = 12.0;
  static const md = 20.0;
  static const lg = 28.0;
  static const pill = 999.0;
}

abstract final class PulseMotion {
  static const quick = Duration(milliseconds: 180);
  static const standard = Duration(milliseconds: 300);
  static const wave = Duration(milliseconds: 520);
  static const slow = Duration(milliseconds: 900);
  static const routeCurve = Curves.easeOutCubic;
}

abstract final class PulseSize {
  static const connectButton = 168.0;
  static const connectStage = 300.0;
  static const bottomNavHeight = 72.0;
  static const iconButton = 44.0;
}

abstract final class PulseShadows {
  static List<BoxShadow> glow(Color color, {double strength = .2}) => [
        BoxShadow(
          color: color.withValues(alpha: strength),
          blurRadius: 40,
          spreadRadius: 2,
        ),
      ];
}

