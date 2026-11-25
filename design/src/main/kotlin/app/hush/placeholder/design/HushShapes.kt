package app.hush.placeholder.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Corner radii from DESIGN_SYSTEM section 4.2. No other radii. */
object HushShapes {
    val xs: RoundedCornerShape = RoundedCornerShape(6.dp)
    val s: RoundedCornerShape = RoundedCornerShape(8.dp)
    val m: RoundedCornerShape = RoundedCornerShape(12.dp)
    val l: RoundedCornerShape = RoundedCornerShape(20.dp)
    val xl: RoundedCornerShape = RoundedCornerShape(28.dp)

    /** Material shapes mapped to HUSH radii, so Material components pick up the same corners. */
    val material: Shapes = Shapes(
        extraSmall = xs,
        small = s,
        medium = m,
        large = l,
        extraLarge = xl,
    )
}
