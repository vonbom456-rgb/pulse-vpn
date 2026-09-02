import 'package:flutter/material.dart';
import 'package:pulse_vpn/core/theme/pulse_tokens.dart';

abstract final class PulseTheme {
  static ThemeData get dark => _build(Brightness.dark);
  static ThemeData get light => _build(Brightness.light);

  static ThemeData _build(Brightness brightness) {
    final dark = brightness == Brightness.dark;
    final scheme = ColorScheme.fromSeed(
      seedColor: PulseColors.indigo,
      brightness: brightness,
      surface: dark ? PulseColors.surfaceRaised : PulseColors.surfaceLight,
    );
    final base = ThemeData(
      brightness: brightness,
      colorScheme: scheme,
      scaffoldBackgroundColor:
          dark ? PulseColors.background : PulseColors.backgroundLight,
      useMaterial3: true,
      splashFactory: NoSplash.splashFactory,
      fontFamily: 'Inter',
    );
    final textColor = dark ? PulseColors.textPrimary : const Color(0xFF15171E);
    return base.copyWith(
      textTheme: base.textTheme.copyWith(
        displayLarge: TextStyle(
          color: textColor,
          fontSize: 48,
          height: 1,
          fontWeight: FontWeight.w700,
          letterSpacing: -2,
        ),
        headlineLarge: TextStyle(
          color: textColor,
          fontSize: 30,
          height: 1.15,
          fontWeight: FontWeight.w700,
          letterSpacing: -1,
        ),
        headlineMedium: TextStyle(
          color: textColor,
          fontSize: 22,
          fontWeight: FontWeight.w600,
          letterSpacing: -.5,
        ),
        titleMedium: TextStyle(
          color: textColor,
          fontSize: 16,
          fontWeight: FontWeight.w600,
        ),
        bodyLarge: TextStyle(color: textColor, fontSize: 16, height: 1.45),
        bodyMedium: const TextStyle(
          color: PulseColors.textSecondary,
          fontSize: 14,
          height: 1.4,
        ),
        labelLarge: TextStyle(
          color: textColor,
          fontSize: 14,
          fontWeight: FontWeight.w600,
          letterSpacing: .2,
        ),
      ),
      dividerTheme: const DividerThemeData(color: PulseColors.divider),
      bottomSheetTheme: BottomSheetThemeData(
        backgroundColor: dark ? PulseColors.surfaceRaised : Colors.white,
        modalBackgroundColor: dark ? PulseColors.surfaceRaised : Colors.white,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(
            top: Radius.circular(PulseRadius.lg),
          ),
        ),
      ),
      pageTransitionsTheme: const PageTransitionsTheme(
        builders: {
          TargetPlatform.android: FadeForwardsPageTransitionsBuilder(),
          TargetPlatform.iOS: FadeForwardsPageTransitionsBuilder(),
        },
      ),
    );
  }
}
