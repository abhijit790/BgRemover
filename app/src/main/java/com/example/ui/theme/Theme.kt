package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext


private val DarkColorScheme = darkColorScheme(
  primary = Purple80,
  onPrimary = Color(0xFF381E72),
  primaryContainer = SleekPrimaryContainer,
  onPrimaryContainer = Color(0xFFEADDFF),
  secondary = PurpleGrey80,
  secondaryContainer = Color(0xFF332D41),
  background = SleekBgDark,
  surface = Color(0xFF1C1A22),
  surfaceVariant = SleekSecondaryText,
  onBackground = Color(0xFFE6E1E5),
  onSurface = Color(0xFFE6E1E5)
)

private val LightColorScheme = lightColorScheme(
  primary = SleekPrimary,
  onPrimary = Color.White,
  primaryContainer = SleekPrimaryContainer,
  onPrimaryContainer = Color(0xFF21005D),
  secondary = SleekSecondaryText,
  secondaryContainer = SleekSecondaryContainer,
  onSecondaryContainer = SleekPrimary,
  background = SleekBgLight,
  surface = Color(0xFFF7F2FA),
  surfaceVariant = SleekSurfaceVariant,
  onBackground = SleekOnBgLight,
  onSurface = SleekOnBgLight
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling dynamicColor by default so the gorgeous Sleek Interface palette is always displayed
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

