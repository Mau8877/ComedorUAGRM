import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// Paleta e identidad visual de ComedorU.
abstract final class AppColors {
  // Modo claro
  static const lightPrimary = Color(0xFF1E3A56);
  static const lightAccent = Color(0xFFE8B657);
  static const lightBackground = Color(0xFFEAEEF3);
  static const lightSurface = Color(0xFFFFFFFF);
  static const lightTextPrimary = Color(0xFF141C26);
  static const lightTextSecondary = Color(0xFF5B6A78);
  static const lightSuccess = Color(0xFF5E7A5C);
  static const lightError = Color(0xFFB24A3C);

  // Modo oscuro
  static const darkPrimary = Color(0xFF5F84A6);
  static const darkAccent = Color(0xFFE8B657);
  static const darkBackground = Color(0xFF141C26);
  static const darkSurface = Color(0xFF1C2530);
  static const darkTextPrimary = Color(0xFFE7ECF1);
  static const darkTextSecondary = Color(0xFF8794A2);
  static const darkSuccess = Color(0xFF7FA97D);
  static const darkError = Color(0xFFD97A66);
}

abstract final class AppTheme {
  static TextTheme _textTheme(Color textPrimary, Color textSecondary) {
    final heading = GoogleFonts.familjenGroteskTextTheme();
    final body = GoogleFonts.jostTextTheme();

    return body
        .copyWith(
          displayLarge: heading.displayLarge,
          displayMedium: heading.displayMedium,
          displaySmall: heading.displaySmall,
          headlineLarge: heading.headlineLarge,
          headlineMedium: heading.headlineMedium,
          headlineSmall: heading.headlineSmall,
          titleLarge: heading.titleLarge,
          titleMedium: heading.titleMedium,
          titleSmall: heading.titleSmall,
        )
        .apply(
          bodyColor: textPrimary,
          displayColor: textPrimary,
        )
        .copyWith(
          bodySmall: body.bodySmall?.copyWith(color: textSecondary),
          labelSmall: body.labelSmall?.copyWith(color: textSecondary),
        );
  }

  static ThemeData get light {
    const colorScheme = ColorScheme.light(
      primary: AppColors.lightPrimary,
      onPrimary: Colors.white,
      secondary: AppColors.lightAccent,
      onSecondary: AppColors.lightTextPrimary,
      surface: AppColors.lightSurface,
      onSurface: AppColors.lightTextPrimary,
      error: AppColors.lightError,
      onError: Colors.white,
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: AppColors.lightBackground,
      textTheme: _textTheme(
        AppColors.lightTextPrimary,
        AppColors.lightTextSecondary,
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.lightPrimary,
        foregroundColor: Colors.white,
      ),
    );
  }

  static ThemeData get dark {
    const colorScheme = ColorScheme.dark(
      primary: AppColors.darkPrimary,
      onPrimary: AppColors.darkBackground,
      secondary: AppColors.darkAccent,
      onSecondary: AppColors.darkBackground,
      surface: AppColors.darkSurface,
      onSurface: AppColors.darkTextPrimary,
      error: AppColors.darkError,
      onError: AppColors.darkBackground,
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: AppColors.darkBackground,
      textTheme: _textTheme(
        AppColors.darkTextPrimary,
        AppColors.darkTextSecondary,
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.darkSurface,
        foregroundColor: AppColors.darkTextPrimary,
      ),
    );
  }
}
