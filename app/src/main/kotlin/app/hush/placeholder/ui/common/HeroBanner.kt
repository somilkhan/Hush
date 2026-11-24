package app.hush.placeholder.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.text.style.TextOverflow
import app.hush.placeholder.core.domain.model.MediaItem
import app.hush.placeholder.design.HushShapes
import app.hush.placeholder.design.HushSpacing
import app.hush.placeholder.design.HushTokens

/**
 * Feature block for the Home screen: tonal backdrop, title block, and two actions.
 * Play uses the first stream of the first episode. Details opens the detail screen.
 */
@Composable
fun HeroBanner(
    item: MediaItem,
    onPlay: (String) -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HushTokens.colors
    val playUrl = item.episodes.firstOrNull()?.streams?.firstOrNull()?.url

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 11f)
            .clip(HushShapes.l)
            .background(colors.surfaceRaised),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column(
            modifier = Modifier.padding(HushSpacing.gutter),
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
                    onClick = { playUrl?.let(onPlay) },
                    enabled = playUrl != null,
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
