import 'package:comedoru_palette/comedoru_palette.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

abstract final class AppTheme {
  static TextTheme _textTheme(Color textPrimary, Color textSecondary) {
    final heading = GoogleFonts.getTextTheme(ComedoruStyle.fontHeading);
    final body = GoogleFonts.getTextTheme(ComedoruStyle.fontBody);

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
      primary: ComedoruPalette.lightPrimary,
      onPrimary: Colors.white,
      secondary: ComedoruPalette.lightAccent,
      onSecondary: Colors.white,
      surface: ComedoruPalette.lightCard,
      onSurface: ComedoruPalette.lightForeground,
      error: ComedoruPalette.lightDestructive,
      onError: Colors.white,
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: ComedoruPalette.lightBackground,
      textTheme: _textTheme(
        ComedoruPalette.lightForeground,
        ComedoruPalette.lightMutedForeground,
      ),
      cardTheme: CardThemeData(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(ComedoruStyle.radius),
        ),
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: ComedoruPalette.lightHeader,
        foregroundColor: ComedoruPalette.lightHeaderForeground,
      ),
    );
  }

  static ThemeData get dark {
    const colorScheme = ColorScheme.dark(
      primary: ComedoruPalette.darkPrimary,
      onPrimary: ComedoruPalette.darkBackground,
      secondary: ComedoruPalette.darkAccent,
      onSecondary: Colors.white,
      surface: ComedoruPalette.darkCard,
      onSurface: ComedoruPalette.darkForeground,
      error: ComedoruPalette.darkDestructive,
      onError: ComedoruPalette.darkBackground,
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: ComedoruPalette.darkBackground,
      textTheme: _textTheme(
        ComedoruPalette.darkForeground,
        ComedoruPalette.darkMutedForeground,
      ),
      cardTheme: CardThemeData(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(ComedoruStyle.radius),
        ),
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: ComedoruPalette.lightHeader,
        foregroundColor: ComedoruPalette.lightHeaderForeground,
      ),
    );
  }
}
