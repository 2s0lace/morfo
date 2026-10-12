package app.morfo.ui.compare

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.morfo.AppViewModel
import app.morfo.math.Quat
import app.morfo.math.Vec3
import app.morfo.render.ViewportRenderer
import app.morfo.ui.theme.MorfoColors
import app.morfo.workspace.LabelMode
import app.morfo.workspace.Workspace
import kotlin.math.PI

@Composable
fun CompareScreen(vm: AppViewModel) {
    val workspace = vm.workspace
    val renderer = rememberViewportRenderer(vm.runtime, workspace)
    var showSettings by remember { mutableStateOf(false) }
    var infoKey by remember { mutableStateOf<Long?>(null) }

    BackHandler { vm.backToSelector() }
    LaunchedEffect(vm.labelMode) {
        workspace.labelSpaceMm = when (vm.labelMode) {
            LabelMode.NONE -> 0f
            LabelMode.FDI -> 4f
            else -> 7f
        }
        workspace.labelGapMm = if (vm.labelMode == LabelMode.FDI_EN || vm.labelMode == LabelMode.FDI_PL) 4f else 0f
        workspace.requestFraming()
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(MorfoColors.Background)) {
        // Landscape phone: too little height for bars above and below the teeth.
        val compact = maxHeight < 480.dp
        Viewport3D(renderer, vm.lightingMode, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .viewportGestures(remember(renderer) { GestureController(workspace, renderer) }),
        )
        ToothLabels(workspace, renderer, vm.labelMode)

        val density = LocalDensity.current
        // Landscape: the preset column takes the full right edge; the top bar stops next to it.
        var presetWidth by remember { mutableIntStateOf(0) }
        val presetWidthDp = if (compact) with(density) { presetWidth.toDp() } else 0.dp
        CompareTopBar(
            workspace = workspace,
            onBack = vm::backToSelector,
            onSettings = { showSettings = true },
            compact = compact,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(end = presetWidthDp)
                .onGloballyPositioned { renderer.rig.insetTopPx = it.size.height.toFloat() },
        )

        if (compact) {
            PresetColumn(
                workspace,
                Modifier
                    .align(Alignment.TopEnd)
                    .onGloballyPositioned {
                        presetWidth = it.size.width
                        renderer.rig.insetRightPx = it.size.width.toFloat()
                    },
            )
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(end = presetWidthDp)
                    .onGloballyPositioned { renderer.rig.insetBottomPx = it.size.height.toFloat() },
            ) {
                SelectedToothBar(workspace, onInfo = { infoKey = it }, onVariant = vm::setVariant, compact = true)
            }
        } else {
            renderer.rig.insetRightPx = 0f
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onGloballyPositioned { renderer.rig.insetBottomPx = it.size.height.toFloat() },
            ) {
                SelectedToothBar(workspace, onInfo = { infoKey = it }, onVariant = vm::setVariant)
                PresetBar(workspace)
            }
        }

        AnimatedVisibility(renderer.isLoading, Modifier.align(Alignment.Center), enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier.background(MorfoColors.Glass, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = MorfoColors.Accent, strokeWidth = 2.dp)
                    Text(app.morfo.i18n.LocalStrings.current.loadingModels, Modifier.padding(top = 8.dp), color = MorfoColors.TextMuted)
                }
            }
        }
    }

    // In landscape the panels float on the left; the camera keeps the teeth in the free area.
    val sidePanel: (Float) -> Unit = { renderer.rig.insetLeftPx = it }
    if (showSettings) ViewSettingsSheet(vm, onDismiss = { showSettings = false }, onSidePanelWidth = sidePanel)
    infoKey?.let { key ->
        workspace.find(key)?.let {
            ToothInfoSheet(it, onVariant = { v -> vm.setVariant(key, v) }, onDismiss = { infoKey = null }, onSidePanelWidth = sidePanel)
        } ?: run { infoKey = null }
    }
}

/** Maps raw gestures to workspace and camera operations. */
private class GestureController(
    private val workspace: Workspace,
    private val renderer: ViewportRenderer,
) : ViewportGestureHandler {
    private var movingKey: Long? = null

    override fun onTap(position: Offset) {
        renderer.pick(position.x, position.y) { key ->
            if (key == null) workspace.select(null) else workspace.toggleSelect(key)
        }
    }

    override fun onDoubleTap(position: Offset) {
        renderer.pick(position.x, position.y) { key ->
            val tooth = key?.let(workspace::find)
            val bounds = tooth?.worldBounds
            if (tooth != null && bounds != null) {
                workspace.select(tooth.key)
                renderer.rig.focusOn(bounds)
            } else {
                workspace.requestFraming()
            }
        }
    }

    override fun onRotateDrag(delta: Offset) {
        val radiansPerPixel = (PI / renderer.rig.width).toFloat() * 1.1f
        val yaw = Quat.axisAngle(Vec3.Y, delta.x * radiansPerPixel)
        val pitch = Quat.axisAngle(Vec3.X, delta.y * radiansPerPixel)
        workspace.rotateBy(yaw * pitch)
    }

    override fun onMoveStart(position: Offset) {
        renderer.pick(position.x, position.y) { key ->
            movingKey = key
            if (key != null) workspace.select(key)
        }
    }

    override fun onMoveDrag(delta: Offset) {
        val key = movingKey ?: return
        val tooth = workspace.find(key) ?: return
        workspace.moveBy(key, renderer.rig.screenDeltaToWorld(delta.x, delta.y, tooth.position.z))
    }

    override fun onMoveEnd() {
        movingKey = null
    }

    override fun onTransform(pan: Offset, zoom: Float, twistRadians: Float, centroid: Offset) {
        renderer.rig.pan(pan.x, pan.y)
        if (zoom != 1f) renderer.rig.zoom(zoom, centroid)
        if (twistRadians != 0f) workspace.rotateBy(Quat.axisAngle(Vec3.Z, -twistRadians))
    }
}
