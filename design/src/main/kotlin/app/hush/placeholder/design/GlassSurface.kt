package app.hush.placeholder.design

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Chrome-only glass container (top bar, bottom bar, player control cluster).
 *
 * What this renders today: a translucent tint with a 1dp top rim and a hairline edge.
 * It does NOT blur what is behind it. A real backdrop blur needs the content behind the bar to be captured
 * (a backdrop library or a RenderNode path). That is not adopted yet (DESIGN_SYSTEM 5.2), so no blur is claimed.
 *
 * Translucent tint is used only on API 31+ and when glass is not reduced. Otherwise the opaque fallback
 * (surfaceRaised at 96%) is used. The rim and hairline are the same in both, so the structure does not change.
 * Never place this on content cards, body text or over video.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = HushShapes.xl,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = HushTokens.colors
    val reduced = LocalReduceTransparency.current
    val translucent = !reduced && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val fill = if (translucent) colors.glassTint else colors.surfaceRaised.copy(alpha = 0.96f)
    val rim = colors.rim
    val hairline = colors.separator

    Box(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .drawBehind {
                drawLine(
                    color = rim,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .border(width = 1.dp, color = hairline, shape = shape),
        content = content,
    )
}
