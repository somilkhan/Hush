package app.hush.placeholder.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hush.placeholder.core.domain.model.MediaItem
import app.hush.placeholder.core.domain.model.MediaKind
import app.hush.placeholder.design.GlassSurface
import app.hush.placeholder.design.HushShapes
import app.hush.placeholder.design.HushSpacing
import app.hush.placeholder.design.HushTokens

/** The three bottom destinations. Settings and Extensions are not in this slice. */
enum class HushTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    SEARCH("Search", Icons.Filled.Search),
    LIBRARY("Library", Icons.AutoMirrored.Filled.List),
    SETTINGS("Settings", Icons.Filled.Settings),
}

/** Glass top bar in the chrome role. Title is a heading for TalkBack. */
@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
) {
    val colors = HushTokens.colors
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
        shape = RectangleShape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = HushSpacing.s8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (navigationIcon != null) {
                navigationIcon()
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = HushSpacing.s8)
                    .semantics { heading() },
            )
        }
    }
}

/**
 * Visible keyboard focus: a 2dp ring in [color] on [shape], drawn only while the element has
 * focus. Touch users never see it; it exists for D-pad, keyboard and switch access.
 */
fun Modifier.focusRing(focused: Boolean, shape: Shape, color: Color): Modifier =
    if (focused) border(width = 2.dp, color = color, shape = shape) else this

/** Back control with a 48dp hit area. */
@Composable
fun HushBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.size(HushSpacing.minTouch)) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
        )
    }
}

/** Bottom navigation in the chrome role. Selected item tints icon and label only. */
@Composable
fun HushNavBar(
    selected: HushTab,
    onSelect: (HushTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HushTokens.colors
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        shape = RectangleShape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HushTab.entries.forEach { tab ->
                val isSelected = tab == selected
                val tint = if (isSelected) colors.accent else colors.labelSecondary
                val source = remember { MutableInteractionSource() }
                val focused by source.collectIsFocusedAsState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = HushSpacing.minTouch)
                        .focusRing(focused, HushShapes.m, colors.accent)
                        .selectable(
                            selected = isSelected,
                            interactionSource = source,
                            indication = LocalIndication.current,
                            role = Role.Tab,
                            onClick = { onSelect(tab) },
                        )
                        .padding(vertical = HushSpacing.s8),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Decorative: the text label names the destination for TalkBack.
                    Icon(imageVector = tab.icon, contentDescription = null, tint = tint)
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = tint,
                    )
                }
            }
        }
    }
}

/** Demo marker shown on every item where isDemo is true. Sentence case per DESIGN_SYSTEM section 3.1. */
@Composable
fun DemoBadge(modifier: Modifier = Modifier) {
    val colors = HushTokens.colors
    Text(
        text = "Demo",
        style = MaterialTheme.typography.labelSmall,
        color = colors.labelSecondary,
        modifier = modifier
            .clip(HushShapes.xs)
            .border(width = 1.dp, color = colors.separator, shape = HushShapes.xs)
            .padding(horizontal = HushSpacing.s8 - HushSpacing.s2, vertical = HushSpacing.s2),
    )
}

/** Text name for a kind, shown beside the year. */
fun kindLabel(kind: MediaKind): String = when (kind) {
    MediaKind.MOVIE -> "Movie"
    MediaKind.SERIES -> "Series"
}

/** One title as a list row. There is no artwork in the contract, so the row is typographic (no invented images). */
@Composable
fun MediaListItem(
    item: MediaItem,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HushTokens.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(HushShapes.m)
            .focusRing(focused, HushShapes.m, colors.accent)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                onClickLabel = "Open ${item.title}",
                role = Role.Button,
            ) { onOpen(item.id) }
            .padding(vertical = HushSpacing.s12),
        verticalArrangement = Arrangement.spacedBy(HushSpacing.s4),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HushSpacing.s8),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (item.isDemo) {
                DemoBadge()
            }
        }
        Text(
            text = "${item.year} · ${kindLabel(item.kind)}",
            style = MaterialTheme.typography.labelMedium,
            color = colors.labelSecondary,
        )
        if (item.synopsis.isNotBlank()) {
            Text(
                text = item.synopsis,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.labelSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Static skeleton rows at a fixed height. No shimmer, no looping motion. Hidden from TalkBack except one label. */
@Composable
fun LoadingRows(description: String, modifier: Modifier = Modifier) {
    val colors = HushTokens.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description }
            .padding(horizontal = HushSpacing.gutter, vertical = HushSpacing.s8),
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = HushSpacing.s12)
                    .height(64.dp)
                    .clip(HushShapes.m)
                    .background(colors.surfaceRaised),
            )
            if (index < 2) {
                HorizontalDivider(color = colors.separator)
            }
        }
    }
}

/** Empty, error or information state. Title is a heading. The optional action is a labelled button. */
@Composable
fun StateMessage(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = HushTokens.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HushSpacing.gutter, vertical = HushSpacing.s32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HushSpacing.s12),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.label,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.labelSecondary,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.defaultMinSize(minHeight = HushSpacing.minTouch),
            ) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
