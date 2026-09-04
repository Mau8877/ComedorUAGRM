import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// Paleta e identidad visual de ComedorU.
///
/// * Espejo manual de las variables de `projects/frontend/src/index.css` --
/// * no hay forma de compartir el archivo de tokens entre Tailwind/CSS y
/// * Dart (ver WIDGETS_UI_MOBILE.md), así que todo cambio de paleta ahí se
/// * replica acá a mano en el mismo cambio.
abstract final class AppColors {
  // Modo claro
  static const lightPrimary = Color(0xFF2A3964);
  static const lightAccent = Color(0xFFCC100C);
  static const lightBackground = Color(0xFFEAEEF3);
  static const lightSurface = Color(0xFFFFFFFF);
  static const lightTextPrimary = Color(0xFF141C26);
  static const lightTextSecondary = Color(0xFF5B6A78);
  static const lightSuccess = Color(0xFF5E7A5C);
  static const lightError = Color(0xFFB24A3C);
  static const lightWarning = Color(0xFFB45309);
  static const lightInfo = Color(0xFF1F6FA0);

  // Modo oscuro
  static const darkPrimary = Color(0xFF5F84A6);
  static const darkAccent = Color(0xFFE2453F);
  static const darkBackground = Color(0xFF141C26);
  static const darkSurface = Color(0xFF1C2530);
  static const darkTextPrimary = Color(0xFFE7ECF1);
  static const darkTextSecondary = Color(0xFF8794A2);
  static const darkSuccess = Color(0xFF7FA97D);
  static const darkError = Color(0xFFD97A66);
  static const darkWarning = Color(0xFFE8A056);
  static const darkInfo = Color(0xFF4BA3D6);

  // Header -- superficie fija de marca, no varía entre claro/oscuro (mismo
  // criterio que `--header` en TAILWIND_STYLES_FRONTEND.md: si el AppBar
  // usara `primary`, perdería la identidad visual de marca al pasar a modo
  // oscuro, donde `primary` se aclara para mantener contraste).
  static const header = Color(0xFF2A3964);
  static const headerForeground = Colors.white;
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
      onSecondary: Colors.white,
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
        backgroundColor: AppColors.header,
        foregroundColor: AppColors.headerForeground,
      ),
    );
  }

  static ThemeData get dark {
    const colorScheme = ColorScheme.dark(
      primary: AppColors.darkPrimary,
      onPrimary: AppColors.darkBackground,
      secondary: AppColors.darkAccent,
      onSecondary: Colors.white,
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
        backgroundColor: AppColors.header,
        foregroundColor: AppColors.headerForeground,
      ),
    );
  }
}
