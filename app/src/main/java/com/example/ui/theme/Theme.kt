package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.RectangleShape

private val BrutalistColorScheme =
  darkColorScheme(
    primary = AcidYellow,
    onPrimary = PureBlack,
    primaryContainer = MidGrey,
    onPrimaryContainer = PureWhite,
    secondary = DarkGrey,
    onSecondary = PureWhite,
    background = PureBlack,
    onBackground = PureWhite,
    surface = PureBlack,
    onSurface = PureWhite,
    surfaceVariant = MidGrey,
    onSurfaceVariant = LightGrey,
    outline = DarkGrey,
    error = DangerRed,
    onError = PureWhite,
  )

@Composable
fun NudgeTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = BrutalistColorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  NudgeTheme(content = content)
}

