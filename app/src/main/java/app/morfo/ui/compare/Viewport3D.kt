package app.morfo.ui.compare

import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.morfo.render.FilamentRuntime
import app.morfo.render.ViewportRenderer
import app.morfo.workspace.LightingMode
import app.morfo.workspace.Workspace

/** Hosts the Filament SurfaceView and ties the renderer to the Compose + Android lifecycle. */
@Composable
fun rememberViewportRenderer(runtime: FilamentRuntime, workspace: Workspace): ViewportRenderer {
    val renderer = remember { ViewportRenderer(runtime, workspace) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> renderer.resume()
                Lifecycle.Event.ON_PAUSE -> renderer.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) renderer.resume()
        onDispose {
            lifecycle.removeObserver(observer)
            renderer.destroy()
        }
    }
    return renderer
}

@Composable
fun Viewport3D(renderer: ViewportRenderer, lighting: LightingMode, modifier: Modifier = Modifier) {
    LaunchedEffect(lighting) { renderer.setLighting(lighting) }
    AndroidView(
        factory = { context -> SurfaceView(context).also(renderer::attach) },
        modifier = modifier,
    )
}
