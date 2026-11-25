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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.hush.placeholder.GuestStore
import app.hush.placeholder.core.domain.account.AuthRepository
import app.hush.placeholder.core.domain.addon.AddonRegistry
import app.hush.placeholder.core.domain.repository.MediaRepository
import app.hush.placeholder.core.domain.sync.LibraryRepository
import app.hush.placeholder.core.domain.sync.WatchEntry
import app.hush.placeholder.core.domain.sync.WatchHistoryRepository
import app.hush.placeholder.core.player.HushVideoPlayer
import app.hush.placeholder.design.HushTheme
import app.hush.placeholder.design.HushTokens
import app.hush.placeholder.ui.common.GlassTopBar
import app.hush.placeholder.ui.common.HushNavBar
import app.hush.placeholder.ui.common.HushTab
import app.hush.placeholder.ui.details.DetailsScreen
import app.hush.placeholder.ui.details.PlayRequest
import app.hush.placeholder.ui.home.HomeScreen
import app.hush.placeholder.ui.library.LibraryScreen
import app.hush.placeholder.ui.login.LoginScreen
import app.hush.placeholder.ui.search.SearchScreen
import app.hush.placeholder.ui.settings.SettingsScreen
import app.hush.placeholder.ui.sources.SourcesScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private enum class Gate { CHECKING, LOGIN, MAIN }

/** Watched fraction at which an episode counts as completed and leaves Continue watching. */
private const val COMPLETE_FRACTION = 0.9

/**
 * Root of the UI. Shows login until the user has a session or chooses guest.
 * Navigation is state-based: tabs, then details, sources or the player. System back walks back one level.
 */
@Composable
fun HushApp(
    mediaRepository: MediaRepository,
    authRepository: AuthRepository,
    libraryRepository: LibraryRepository,
    historyRepository: WatchHistoryRepository,
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
                historyRepository = historyRepository,
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
    historyRepository: WatchHistoryRepository,
    addonRegistry: AddonRegistry,
    onSignIn: () -> Unit,
    onSignedOut: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(HushTab.HOME) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var sourcesOpen by rememberSaveable { mutableStateOf(false) }
    // Bumped when addons change, so Home reloads its catalogue.
    var catalogRev by rememberSaveable { mutableStateOf(0) }
    // Playback state is saveable, so rotation during playback keeps the player open.
    var playUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var playMediaId by rememberSaveable { mutableStateOf("") }
    var playEpisodeId by rememberSaveable { mutableStateOf("") }
    var playTitle by rememberSaveable { mutableStateOf("") }
    var playStartMs by rememberSaveable { mutableStateOf(0L) }
    fun startPlaying(r: PlayRequest) {
        playUrl = r.url
        playMediaId = r.mediaId
        playEpisodeId = r.episodeId
        playTitle = r.title
        playStartMs = r.startMs
    }
    fun stopPlaying() {
        playUrl = null
    }
    var continueItems by remember { mutableStateOf<List<WatchEntry>>(emptyList()) }
    // Progress is written after the player is released, so it outlives the composition.
    val recordScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    val resumeScope = rememberCoroutineScope()

    LaunchedEffect(playUrl, catalogRev, tab, detailId) {
        continueItems = historyRepository.all()
            .filter { !it.completed && it.positionMs > 0 }
            .groupBy { it.mediaId }
            .values
            .map { entries -> entries.maxBy { it.updatedAtEpochMs } }
            .sortedByDescending { it.updatedAtEpochMs }
    }

    val request = playUrl?.let { PlayRequest(it, playMediaId, playEpisodeId, playTitle, playStartMs) }
    BackHandler(enabled = request != null) { stopPlaying() }
    BackHandler(enabled = request == null && detailId != null) { detailId = null }
    BackHandler(enabled = request == null && detailId == null && sourcesOpen) { sourcesOpen = false }

    Box(Modifier.fillMaxSize().background(HushTokens.colors.canvas)) {
        val id = detailId
        when {
            request != null -> HushVideoPlayer(
                url = request.url,
                startPositionMs = request.startMs,
                modifier = Modifier.fillMaxSize(),
                onBack = { stopPlaying() },
                onProgress = { positionMs, durationMs ->
                    // Ignore the 0 reported while buffering, so it cannot erase a saved position.
                    if (positionMs > 0) recordScope.launch {
                        historyRepository.record(
                            WatchEntry(
                                mediaId = request.mediaId,
                                episodeId = request.episodeId,
                                positionMs = positionMs,
                                durationMs = durationMs,
                                completed = durationMs > 0 && positionMs >= durationMs * COMPLETE_FRACTION,
                                updatedAtEpochMs = System.currentTimeMillis(),
                                title = request.title,
                            ),
                        )
                    }
                },
            )
            id != null -> DetailsScreen(
                repository = mediaRepository,
                library = libraryRepository,
                mediaId = id,
                onBack = { detailId = null },
                onPlay = { startPlaying(it) },
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
                            continueWatching = continueItems,
                            onOpen = { detailId = it },
                            onResume = { entry ->
                                resumeScope.launch {
                                    val stream = mediaRepository.details(entry.mediaId)
                                        ?.let { item -> mediaRepository.streams(item, entry.episodeId).firstOrNull() }
                                    if (stream != null) {
                                        startPlaying(PlayRequest(stream.url, entry.mediaId, entry.episodeId, entry.title, entry.positionMs))
                                    } else {
                                        detailId = entry.mediaId
                                    }
                                }
                            },
                            refreshKey = catalogRev,
                        )
                        HushTab.SEARCH -> SearchScreen(repository = mediaRepository, onOpen = { detailId = it })
                        HushTab.LIBRARY -> LibraryScreen(
                            library = libraryRepository,
                            onOpen = { detailId = it },
                        )
                        HushTab.SETTINGS -> SettingsScreen(
                            auth = authRepository,
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
