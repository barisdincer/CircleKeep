package com.barisdincer.circlekeep.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.barisdincer.circlekeep.data.ThemeMode

private val DarkColorScheme =
  darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer
  )

private val LightColorScheme =
  lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer
  )

/** Warm extras that Material 3 does not model (success / warning rhythm states). */
data class CircleExtraColors(
  val success: Color,
  val onSuccess: Color,
  val successContainer: Color,
  val onSuccessContainer: Color,
  val warning: Color,
  val onWarning: Color,
  val warningContainer: Color,
  val onWarningContainer: Color,
)

private val LightExtraColors = CircleExtraColors(
  success = Color(0xFF2F8F5B),
  onSuccess = Color(0xFFFFFFFF),
  successContainer = Color(0xFFDDF0E4),
  onSuccessContainer = Color(0xFF0E3A23),
  warning = Color(0xFFBA7517),
  onWarning = Color(0xFFFFFFFF),
  warningContainer = Color(0xFFFCEFD7),
  onWarningContainer = Color(0xFF4A2E06),
)

private val DarkExtraColors = CircleExtraColors(
  success = Color(0xFF7BCB9E),
  onSuccess = Color(0xFF0E3A23),
  successContainer = Color(0xFF2A4A37),
  onSuccessContainer = Color(0xFFDDF0E4),
  warning = Color(0xFFE7B05A),
  onWarning = Color(0xFF422C06),
  warningContainer = Color(0xFF5E4413),
  onWarningContainer = Color(0xFFFCEFD7),
)

val LocalCircleExtraColors = staticCompositionLocalOf { LightExtraColors }

object CircleTheme {
  val extras: CircleExtraColors
    @Composable
    @ReadOnlyComposable
    get() = LocalCircleExtraColors.current
}

@Composable
fun MyApplicationTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  content: @Composable () -> Unit,
) {
  val darkTheme = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
  }
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

  CompositionLocalProvider(LocalCircleExtraColors provides extraColors) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
