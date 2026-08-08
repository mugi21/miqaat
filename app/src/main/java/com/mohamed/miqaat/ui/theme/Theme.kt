package com.mohamed.miqaat.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = GreenPrimaryContainer,
    onPrimaryContainer = GreenOnPrimaryContainer,
    inversePrimary = GreenInversePrimary,
    secondary = GreenSecondary,
    onSecondary = GreenOnSecondary,
    secondaryContainer = GreenSecondaryContainer,
    onSecondaryContainer = GreenOnSecondaryContainer,
    tertiary = GreenTertiary,
    onTertiary = GreenOnTertiary,
    tertiaryContainer = GreenTertiaryContainer,
    onTertiaryContainer = GreenOnTertiaryContainer,
    background = GreenBackground,
    onBackground = GreenOnBackground,
    surface = GreenBackground,
    onSurface = GreenOnBackground,
    surfaceVariant = GreenSurfaceVariant,
    onSurfaceVariant = GreenOnSurfaceVariant,
    surfaceContainerLow = GreenSurfaceContainerLow,
    surfaceContainer = GreenSurfaceContainer,
    surfaceContainerHigh = GreenSurfaceContainerHigh,
    outline = GreenOutline,
    outlineVariant = GreenOutlineVariant,
    error = GreenError,
    onError = GreenOnError,
    errorContainer = GreenErrorContainer,
    onErrorContainer = GreenOnErrorContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkGreenPrimary,
    onPrimary = DarkGreenOnPrimary,
    primaryContainer = DarkGreenPrimaryContainer,
    onPrimaryContainer = DarkGreenOnPrimaryContainer,
    inversePrimary = DarkGreenInversePrimary,
    secondary = DarkGreenSecondary,
    onSecondary = DarkGreenOnSecondary,
    secondaryContainer = DarkGreenSecondaryContainer,
    onSecondaryContainer = DarkGreenOnSecondaryContainer,
    tertiary = DarkGreenTertiary,
    onTertiary = DarkGreenOnTertiary,
    tertiaryContainer = DarkGreenTertiaryContainer,
    onTertiaryContainer = DarkGreenOnTertiaryContainer,
    background = DarkGreenBackground,
    onBackground = DarkGreenOnBackground,
    surface = DarkGreenBackground,
    onSurface = DarkGreenOnBackground,
    surfaceVariant = DarkGreenSurfaceVariant,
    onSurfaceVariant = DarkGreenOnSurfaceVariant,
    surfaceContainerLow = DarkGreenSurfaceContainerLow,
    surfaceContainer = DarkGreenSurfaceContainer,
    surfaceContainerHigh = DarkGreenSurfaceContainerHigh,
    outline = DarkGreenOutline,
    outlineVariant = DarkGreenOutlineVariant,
    error = DarkGreenError,
    onError = DarkGreenOnError,
    errorContainer = DarkGreenErrorContainer,
    onErrorContainer = DarkGreenOnErrorContainer,
)

@Composable
fun MiqaatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Désactivé par défaut : sur Android 12+, la couleur dynamique remplace toute la
    // palette par celle du fond d'écran de l'utilisateur — l'identité verte de Miqaat
    // disparaîtrait. Le paramètre reste disponible pour en faire une option plus tard.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
