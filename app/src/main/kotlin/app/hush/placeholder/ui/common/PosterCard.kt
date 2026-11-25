package app.hush.placeholder.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import app.hush.placeholder.core.domain.model.MediaItem
import app.hush.placeholder.design.HushShapes
import app.hush.placeholder.design.HushSpacing
import app.hush.placeholder.design.HushTokens

/**
 * Portrait poster tile (2:3). No artwork exists in the contract yet, so the poster is a tonal
 * block with the title initial and a fade at the bottom. It is never presented as real artwork.
 */
@Composable
fun PosterCard(
    item: MediaItem,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HushTokens.colors
    Column(
        modifier = modifier
            .clip(HushShapes.m)
            .clickable(onClickLabel = "Open ${item.title}", role = Role.Button) { onOpen(item.id) }
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(HushSpacing.s8),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(HushShapes.m)
                .background(colors.surfaceRaised),
            contentAlignment = Alignment.BottomStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, colors.canvas.copy(alpha = 0.6f)),
                        ),
                    ),
            )
            Text(
                text = item.title.firstOrNull()?.uppercase() ?: "",
                style = MaterialTheme.typography.displayMedium,
                color = colors.labelTertiary,
                modifier = Modifier.padding(HushSpacing.s12),
            )
            if (item.isDemo) {
                DemoBadge(modifier = Modifier.align(Alignment.TopStart).padding(HushSpacing.s8))
            }
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall,
            color = colors.label,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${kindLabel(item.kind)} · ${item.year}",
            style = MaterialTheme.typography.labelSmall,
            color = colors.labelSecondary,
            maxLines = 1,
        )
    }
}
