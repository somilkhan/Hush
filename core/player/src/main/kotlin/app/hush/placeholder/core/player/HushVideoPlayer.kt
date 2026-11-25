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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

/**
 * Plays one progressive or HLS/DASH URL with Media3.
 *
 * - The ExoPlayer is created inside a DisposableEffect keyed by [url], so it is released exactly once per URL.
 * - Pauses on ON_PAUSE and resumes on ON_RESUME only if it was playing before.
 * - [onBack] is called from the on-screen Back control and from the system back gesture.
 *
 * UNVERIFIED: not run on a device yet. Build is verified by CI only.
 */
@Composable
fun HushVideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(url) {
        val created = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
        player = created
        onDispose {
            player = null
            created.release()
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
