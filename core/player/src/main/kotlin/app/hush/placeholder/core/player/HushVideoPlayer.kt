package app.hush.placeholder.core.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

private const val PROGRESS_INTERVAL_MS = 5_000L
private const val POLL_MS = 500L
private const val AUTO_HIDE_MS = 3_000L
private const val SEEK_STEP_MS = 10_000L

/**
 * Plays one progressive or HLS/DASH URL with Media3, with HUSH's own overlay controls.
 *
 * - Overlay: tap the video to show or hide. Controls auto-hide 3 s after playback starts.
 * - Controls: Back, Play/Pause, -10 s, +10 s, a seek slider with time, all with text labels.
 * - The ExoPlayer is created inside a DisposableEffect keyed by [url] and released once per URL.
 * - [onProgress] fires every 5 s and once on release. Unknown durations are clamped to 0.
 *
 * UNVERIFIED: not run on a device yet. Build is verified by CI only.
 */
@Composable
fun HushVideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    startPositionMs: Long = 0L,
    onBack: () -> Unit,
    onProgress: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val progressCallback by rememberUpdatedState(onProgress)
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var controlsVisible by remember { mutableStateOf(true) }

    DisposableEffect(url) {
        val created = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url), startPositionMs)
            prepare()
            playWhenReady = true
        }
        player = created
        onDispose {
            progressCallback(created.currentPosition, created.duration.coerceAtLeast(0L))
            player = null
            created.release()
        }
    }

    // Reads playback state for the overlay and saves progress every 5 s.
    LaunchedEffect(url) {
        var sinceSave = 0L
        while (true) {
            delay(POLL_MS)
            val p = player ?: continue
            isPlaying = p.isPlaying
            positionMs = p.currentPosition
            durationMs = p.duration.coerceAtLeast(0L)
            sinceSave += POLL_MS
            if (sinceSave >= PROGRESS_INTERVAL_MS) {
                sinceSave = 0L
                progressCallback(positionMs, durationMs)
            }
        }
    }

    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(AUTO_HIDE_MS)
            controlsVisible = false
        }
    }

    DisposableEffect(lifecycleOwner, url) {
        var wasPlaying = false
        val observer = LifecycleEventObserver { _, event ->
            val p = player ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    wasPlaying = p.playWhenReady
                    p.pause()
                }
                Lifecycle.Event.ON_RESUME -> if (wasPlaying) p.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(onBack = onBack)

    fun togglePlay() {
        player?.let { p -> if (p.isPlaying) p.pause() else p.play() }
    }

    fun seekBy(deltaMs: Long) {
        player?.let { p ->
            val max = p.duration.coerceAtLeast(0L)
            p.seekTo((p.currentPosition + deltaMs).coerceIn(0L, max))
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx -> PlayerView(ctx).apply { useController = false } },
            update = { view -> view.player = player },
        )

        // Full-area tap target to show or hide the overlay. Sits under the controls.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { controlsVisible = !controlsVisible },
        )

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Text(text = "Back", color = Color.White)
                }

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { seekBy(-SEEK_STEP_MS) },
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    ) { Text(text = "-10 s", color = Color.White) }
                    TextButton(
                        onClick = { togglePlay() },
                        modifier = Modifier.defaultMinSize(minWidth = 88.dp, minHeight = 48.dp),
                    ) { Text(text = if (isPlaying) "Pause" else "Play", color = Color.White) }
                    TextButton(
                        onClick = { seekBy(SEEK_STEP_MS) },
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    ) { Text(text = "+10 s", color = Color.White) }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                ) {
                    Slider(
                        value = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f,
                        onValueChange = { fraction ->
                            player?.seekTo((fraction * durationMs).toLong())
                        },
                        enabled = durationMs > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Seek position" },
                    )
                    Text(
                        text = "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                        color = Color.White,
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = ms.coerceAtLeast(0L) / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
