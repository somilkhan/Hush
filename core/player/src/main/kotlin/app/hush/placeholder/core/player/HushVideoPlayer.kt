package app.hush.placeholder.core.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

/**
 * Plays one progressive or HLS/DASH URL with Media3.
 *
 * - The ExoPlayer is created inside a DisposableEffect keyed by [url] and released once per URL.
 * - [onProgress] fires every 5 s while playing and once more when the player is released.
 *   Durations are clamped to 0 when Media3 reports an unknown duration.
 * - Pauses on ON_PAUSE and resumes on ON_RESUME only if it was playing before.
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

    LaunchedEffect(url) {
        while (true) {
            delay(PROGRESS_INTERVAL_MS)
            val p = player ?: continue
            progressCallback(p.currentPosition, p.duration.coerceAtLeast(0L))
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

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx -> PlayerView(ctx).apply { useController = true } },
            update = { view -> view.player = player },
        )

        TextButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        ) {
            Text(text = "Back", color = Color.White)
        }
    }
}
