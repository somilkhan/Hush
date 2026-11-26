package app.hushstream.design

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 4-point spacing scale from DESIGN_SYSTEM section 4.1, plus the gutter and touch-target minimums. */
object HushSpacing {
    val s2: Dp = 2.dp
    val s4: Dp = 4.dp
    val s8: Dp = 8.dp
    val s12: Dp = 12.dp
    val s16: Dp = 16.dp
    val s20: Dp = 20.dp
    val s24: Dp = 24.dp
    val s32: Dp = 32.dp
    val s40: Dp = 40.dp
    val s48: Dp = 48.dp
    val s64: Dp = 64.dp

    /** Horizontal screen gutter, compact width. */
    val gutter: Dp = 16.dp

    /** Minimum interactive size for every control (ACCESSIBILITY section 1). */
    val minTouch: Dp = 48.dp
}
