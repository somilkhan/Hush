package app.hush.placeholder.design

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Role-based colour tokens from docs/design/DESIGN_SYSTEM.md section 2.
 * Light and dark are authored separately. No raw hex belongs in components; read tokens via HushTokens.colors.
 */
data class HushColors(
    val canvas: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val label: Color,
    val labelSecondary: Color,
    val labelTertiary: Color,
    val separator: Color,
    val accent: Color,
    val accentOn: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val scrim: Color,
    val glassTint: Color,
    val rim: Color,
)

val hushLightColors = HushColors(
    canvas = Color(0xFFF4F4F2),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFFFFFFF),
    label = Color(0xFF141518),
    labelSecondary = Color(0xFF5C6068),
    labelTertiary = Color(0xFF7E838B),
    separator = Color(0x1A141518),
    accent = Color(0xFF0E6B66),
    accentOn = Color(0xFFFFFFFF),
    success = Color(0xFF2F6B3A),
    warning = Color(0xFF8A5A00),
    danger = Color(0xFFA3262B),
    scrim = Color(0x73000000),
    glassTint = Color(0xB8FFFFFF),
    rim = Color(0x2EFFFFFF),
)

val hushDarkColors = HushColors(
    canvas = Color(0xFF0B0C0E),
    surface = Color(0xFF15171A),
    surfaceRaised = Color(0xFF1D2025),
    label = Color(0xFFF2F3F5),
    labelSecondary = Color(0xFF9BA1AA),
    labelTertiary = Color(0xFF6B717A),
    separator = Color(0x1AFFFFFF),
    accent = Color(0xFF5FD0C6),
    accentOn = Color(0xFF0B0C0E),
    success = Color(0xFF7FD19A),
    warning = Color(0xFFE8B65C),
    danger = Color(0xFFF08A8D),
    scrim = Color(0x99000000),
    glassTint = Color(0x991A1D22),
    rim = Color(0x1FFFFFFF),
)

val LocalHushColors = staticCompositionLocalOf { hushLightColors }
