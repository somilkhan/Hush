package app.hush.placeholder.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.hush.placeholder.core.domain.repository.MediaRepository
import app.hush.placeholder.core.player.HushVideoPlayer
import app.hush.placeholder.design.HushTheme
import app.hush.placeholder.design.HushTokens
import app.hush.placeholder.ui.common.GlassTopBar
import app.hush.placeholder.ui.common.HushNavBar
import app.hush.placeholder.ui.common.HushTab
import app.hush.placeholder.ui.details.DetailsScreen
import app.hush.placeholder.ui.home.HomeScreen
import app.hush.placeholder.ui.library.LibraryScreen
import app.hush.placeholder.ui.search.SearchScreen

/**
 * Root of the UI. Navigation is state-based (no NavHost) for this vertical slice:
 * tabs at the root, then details, then the player. System back walks back one level.
 */
@Composable
fun HushApp(mediaRepository: MediaRepository) {
    HushTheme {
        var tab by rememberSaveable { mutableStateOf(HushTab.HOME) }
        var detailId by rememberSaveable { mutableStateOf<String?>(null) }
        var playUrl by rememberSaveable { mutableStateOf<String?>(null) }

        BackHandler(enabled = playUrl != null) { playUrl = null }
        BackHandler(enabled = playUrl == null && detailId != null) { detailId = null }

        Box(Modifier.fillMaxSize().background(HushTokens.colors.canvas)) {
            val url = playUrl
            val id = detailId
            when {
                url != null -> HushVideoPlayer(
                    url = url,
                    modifier = Modifier.fillMaxSize(),
                    onBack = { playUrl = null },
                )
                id != null -> DetailsScreen(
                    repository = mediaRepository,
                    mediaId = id,
                    onBack = { detailId = null },
                    onPlay = { playUrl = it },
                )
                else -> Column(Modifier.fillMaxSize()) {
                    GlassTopBar(title = tab.title)
                    Box(Modifier.weight(1f)) {
                        when (tab) {
                            HushTab.HOME -> HomeScreen(repository = mediaRepository, onOpen = { detailId = it }, onPlay = { playUrl = it })
                            HushTab.SEARCH -> SearchScreen(repository = mediaRepository, onOpen = { detailId = it })
                            HushTab.LIBRARY -> LibraryScreen()
                        }
                    }
                    HushNavBar(selected = tab, onSelect = { tab = it })
                }
            }
        }
    }
}
