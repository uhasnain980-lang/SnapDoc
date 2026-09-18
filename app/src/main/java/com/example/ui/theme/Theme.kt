package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode {
  SYSTEM, LIGHT, DARK
}

private val DarkColorScheme = darkColorScheme(
  primary = SnapBlueLight,
  onPrimary = Color(0xFF0F172A),
  primaryContainer = SnapBlueContainerDark,
  onPrimaryContainer = Color(0xFFDBEAFE),
  secondary = SnapCyanLight,
  onSecondary = Color(0xFF00363D),
  secondaryContainer = SnapCyanDark,
  onSecondaryContainer = Color(0xFFCFFAFE),
  tertiary = SnapEmeraldLight,
  onTertiary = Color(0xFF003822),
  background = BackgroundDark,
  onBackground = TextPrimaryDark,
  surface = SurfaceDark,
  onSurface = TextPrimaryDark,
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = TextSecondaryDark,
  outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
  primary = SnapBluePrimary,
  onPrimary = Color.White,
  primaryContainer = SnapBlueContainerLight,
  onPrimaryContainer = Color(0xFF1E3A8A),
  secondary = SnapCyanSecondary,
  onSecondary = Color.White,
  secondaryContainer = SnapCyanContainer,
  onSecondaryContainer = Color(0xFF155E75),
  tertiary = SnapEmeraldTertiary,
  onTertiary = Color.White,
  background = BackgroundLight,
  onBackground = TextPrimaryLight,
  surface = SurfaceLight,
  onSurface = TextPrimaryLight,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = TextSecondaryLight,
  outline = OutlineLight
)

@Composable
fun SnapDocTheme(
  themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  content: @Composable () -> Unit,
) {
  val isDark = when (themeMode) {
    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
  }

  val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
