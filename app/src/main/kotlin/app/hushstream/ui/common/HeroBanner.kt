package app.hushstream.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import app.hushstream.core.domain.model.MediaItem
import app.hushstream.design.HushShapes
import app.hushstream.design.HushSpacing
import app.hushstream.design.HushTokens

/**
 * Home hero. No artwork exists yet, so the backdrop is a tonal block that fades into the page
 * canvas, keeping the title readable. Play and Details both open Details, where streams resolve.
 */
@Composable
fun HeroBanner(
    item: MediaItem,
    onPlay: (String) -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HushTokens.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 11f)
            .clip(HushShapes.l)
            .background(colors.surfaceRaised),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, colors.canvas.copy(alpha = 0.7f), colors.canvas),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(HushSpacing.gutter),
            verticalArrangement = Arrangement.spacedBy(HushSpacing.s8),
        ) {
            if (item.isDemo) DemoBadge()
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${kindLabel(item.kind)} · ${item.year}",
                style = MaterialTheme.typography.labelLarge,
                color = colors.labelSecondary,
            )
            Text(
                text = item.synopsis,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.labelSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(HushSpacing.s8)) {
                Button(
                    onClick = { onPlay(item.id) },
                    modifier = Modifier.defaultMinSize(minWidth = HushSpacing.minTouch, minHeight = HushSpacing.minTouch),
                ) {
                    Text("Play")
                }
                OutlinedButton(
                    onClick = { onOpen(item.id) },
                    modifier = Modifier.defaultMinSize(minWidth = HushSpacing.minTouch, minHeight = HushSpacing.minTouch),
                ) {
                    Text("Details")
                }
            }
        }
    }
}
