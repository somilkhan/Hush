package app.hush.placeholder.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Corner radii from DESIGN_SYSTEM section 4.2. No other radii. */
object HushShapes {
    val xs: Shape = RoundedCornerShape(6.dp)
    val s: Shape = RoundedCornerShape(8.dp)
    val m: Shape = RoundedCornerShape(12.dp)
    val l: Shape = RoundedCornerShape(20.dp)
    val xl: Shape = RoundedCornerShape(28.dp)

    /** Material shapes mapped to HUSH radii, so Material components pick up the same corners. */
    val material: Shapes = Shapes(
        extraSmall = xs,
        small = s,
        medium = m,
        large = l,
        extraLarge = xl,
    )
}
