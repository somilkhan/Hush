package app.hush.placeholder.design

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Glass preference, as decided in COMPONENT_CATALOG M2 Q4.
 * The app layer maps the stored DataStore value (key ui.glass_mode) to this enum. Components never read storage.
 */
enum class GlassMode { FOLLOW_SYSTEM, REDUCED, FULL }

/** Token accessor. Read colours through this object inside composables. */
object HushTokens {
    val colors: HushColors
        @Composable
        @ReadOnlyComposable
        get() = LocalHushColors.current
}

/**
 * Entry point for the design system. Wrap the app tree once.
 * Reduced transparency applies when the stored mode is REDUCED, or when the system removes animations
 * (Q4: system signal overrides the stored value).
 */
@Composable
fun HushTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    glassMode: GlassMode = GlassMode.FOLLOW_SYSTEM,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemRemovesAnimations = remember(context) { systemRemovesAnimations(context) }
    val reduceTransparency = glassMode == GlassMode.REDUCED || systemRemovesAnimations
    val colors = if (darkTheme) hushDarkColors else hushLightColors

    CompositionLocalProvider(
        LocalHushColors provides colors,
        LocalReduceMotion provides systemRemovesAnimations,
        LocalReduceTransparency provides reduceTransparency,
    ) {
        MaterialTheme(
            colorScheme = materialScheme(colors, darkTheme),
            typography = hushTypography(),
            shapes = HushShapes.material,
            content = content,
        )
    }
}

private fun systemRemovesAnimations(context: Context): Boolean = try {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
} catch (e: Exception) {
    false
}

/**
 * Maps HUSH roles onto Material slots so Material components do not fall back to the baseline purple scheme.
 * Every slot used by Material components is set explicitly.
 */
private fun materialScheme(c: HushColors, dark: Boolean): ColorScheme {
    val primaryContainer = c.accent.copy(alpha = 0.12f)
    return if (dark) {
        darkColorScheme(
            primary = c.accent,
            onPrimary = c.accentOn,
            primaryContainer = primaryContainer,
            onPrimaryContainer = c.accent,
            secondary = c.labelSecondary,
            onSecondary = c.surface,
            secondaryContainer = c.surfaceRaised,
            onSecondaryContainer = c.label,
            tertiary = c.labelSecondary,
            onTertiary = c.surface,
            tertiaryContainer = c.surfaceRaised,
            onTertiaryContainer = c.label,
            background = c.canvas,
            onBackground = c.label,
            surface = c.surface,
            onSurface = c.label,
            surfaceVariant = c.surfaceRaised,
            onSurfaceVariant = c.labelSecondary,
            surfaceContainerLowest = c.canvas,
            surfaceContainerLow = c.surface,
            surfaceContainer = c.surface,
            surfaceContainerHigh = c.surfaceRaised,
            surfaceContainerHighest = c.surfaceRaised,
            outline = c.labelSecondary,
            outlineVariant = c.separator,
            error = c.danger,
            onError = c.surface,
        )
    } else {
        lightColorScheme(
            primary = c.accent,
            onPrimary = c.accentOn,
            primaryContainer = primaryContainer,
            onPrimaryContainer = c.accent,
            secondary = c.labelSecondary,
            onSecondary = c.surface,
            secondaryContainer = c.surfaceRaised,
            onSecondaryContainer = c.label,
            tertiary = c.labelSecondary,
            onTertiary = c.surface,
            tertiaryContainer = c.surfaceRaised,
            onTertiaryContainer = c.label,
            background = c.canvas,
            onBackground = c.label,
            surface = c.surface,
            onSurface = c.label,
            surfaceVariant = c.surfaceRaised,
            onSurfaceVariant = c.labelSecondary,
            surfaceContainerLowest = c.surface,
            surfaceContainerLow = c.surface,
            surfaceContainer = c.surface,
            surfaceContainerHigh = c.surfaceRaised,
            surfaceContainerHighest = c.surfaceRaised,
            outline = c.labelSecondary,
            outlineVariant = c.separator,
            error = c.danger,
            onError = c.surface,
        )
    }
}
