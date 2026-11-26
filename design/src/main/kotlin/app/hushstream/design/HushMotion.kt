package app.hushstream.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Duration tokens from DESIGN_SYSTEM section 7.1. There is no looping or ambient motion. */
enum class HushMotionToken(val millis: Int) {
    INSTANT(0),
    MICRO(120),
    STANDARD(200),
    EMPHASIS(320),
    PAGE(380),
}

/** Curves from DESIGN_SYSTEM section 7.2 (design intent). Springs are not used in this slice. */
object HushMotion {
    val enter: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val exit: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
}

/** True when the system asks to remove animations (animator duration scale is 0). Set in HushTheme. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** True when glass must use the opaque fallback. Set in HushTheme from GlassMode and the system. */
val LocalReduceTransparency = staticCompositionLocalOf { false }

/**
 * Returns a tween for a motion token. Under reduced motion the duration is capped at the STANDARD token,
 * so the change reads as a plain fade. Callers pick fades for reduced motion; no spatial travel is added here.
 */
@Composable
fun <T> motionSpec(token: HushMotionToken, exit: Boolean = false): FiniteAnimationSpec<T> {
    val reduce = LocalReduceMotion.current
    val millis = if (reduce) minOf(token.millis, HushMotionToken.STANDARD.millis) else token.millis
    return tween(
        durationMillis = millis,
        easing = if (exit) HushMotion.exit else HushMotion.enter,
    )
}
