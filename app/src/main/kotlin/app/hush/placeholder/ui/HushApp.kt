package app.hush.placeholder.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.hush.placeholder.GuestStore
import app.hush.placeholder.core.domain.account.AuthRepository
import app.hush.placeholder.core.domain.addon.AddonRegistry
import app.hush.placeholder.core.domain.repository.MediaRepository
import app.hush.placeholder.core.domain.sync.LibraryRepository
import app.hush.placeholder.core.player.HushVideoPlayer
import app.hush.placeholder.design.HushTheme
import app.hush.placeholder.design.HushTokens
import app.hush.placeholder.ui.common.GlassTopBar
import app.hush.placeholder.ui.common.HushNavBar
import app.hush.placeholder.ui.common.HushTab
import app.hush.placeholder.ui.details.DetailsScreen
import app.hush.placeholder.ui.home.HomeScreen
import app.hush.placeholder.ui.library.LibraryScreen
import app.hush.placeholder.ui.login.LoginScreen
import app.hush.placeholder.ui.search.SearchScreen
import app.hush.placeholder.ui.sources.SourcesScreen

private enum class Gate { CHECKING, LOGIN, MAIN }

/**
 * Root of the UI. Shows login until the user has a session or chooses guest.
 * Navigation is state-based: tabs, then details, sources or the player. System back walks back one level.
 */
@Composable
fun HushApp(
    mediaRepository: MediaRepository,
    authRepository: AuthRepository,
    libraryRepository: LibraryRepository,
    addonRegistry: AddonRegistry,
    guestStore: GuestStore,
) {
    HushTheme {
        var gate by remember { mutableStateOf(Gate.CHECKING) }
        LaunchedEffect(Unit) {
            gate = if (authRepository.currentSession() != null || guestStore.isGuest) Gate.MAIN else Gate.LOGIN
        }
        when (gate) {
            Gate.CHECKING -> Box(Modifier.fillMaxSize().background(HushTokens.colors.canvas))
            Gate.LOGIN -> LoginScreen(
                auth = authRepository,
                onSignedIn = { gate = Gate.MAIN },
                onGuest = {
                    guestStore.isGuest = true
                    gate = Gate.MAIN
                },
            )
            Gate.MAIN -> AppShell(
                mediaRepository = mediaRepository,
                authRepository = authRepository,
                libraryRepository = libraryRepository,
                addonRegistry = addonRegistry,
                onSignIn = {
                    guestStore.isGuest = false
                    gate = Gate.LOGIN
                },
                onSignedOut = {
                    guestStore.isGuest = false
                    gate = Gate.LOGIN
                },
            )
        }
    }
}

@Composable
private fun AppShell(
    mediaRepository: MediaRepository,
    authRepository: AuthRepository,
    libraryRepository: LibraryRepository,
    addonRegistry: AddonRegistry,
    onSignIn: () -> Unit,
    onSignedOut: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(HushTab.HOME) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var playUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var sourcesOpen by rememberSaveable { mutableStateOf(false) }
    // Bumped when addons change, so Home reloads its catalogue.
    var catalogRev by rememberSaveable { mutableStateOf(0) }

    BackHandler(enabled = playUrl != null) { playUrl = null }
    BackHandler(enabled = playUrl == null && detailId != null) { detailId = null }
    BackHandler(enabled = playUrl == null && detailId == null && sourcesOpen) { sourcesOpen = false }

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
                library = libraryRepository,
                mediaId = id,
                onBack = { detailId = null },
                onPlay = { playUrl = it },
            )
            sourcesOpen -> SourcesScreen(
                registry = addonRegistry,
                onBack = { sourcesOpen = false },
                onChanged = { catalogRev += 1 },
            )
            else -> Column(Modifier.fillMaxSize()) {
                GlassTopBar(title = tab.title)
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        HushTab.HOME -> HomeScreen(
                            repository = mediaRepository,
                            onOpen = { detailId = it },
                            onPlay = { playUrl = it },
                            refreshKey = catalogRev,
                        )
                        HushTab.SEARCH -> SearchScreen(repository = mediaRepository, onOpen = { detailId = it })
                        HushTab.LIBRARY -> LibraryScreen(
                            library = libraryRepository,
                            auth = authRepository,
                            onOpen = { detailId = it },
                            onSignIn = onSignIn,
                            onSignedOut = onSignedOut,
                            onManageSources = { sourcesOpen = true },
                        )
                    }
                }
                HushNavBar(selected = tab, onSelect = { tab = it })
            }
        }
    }
}
