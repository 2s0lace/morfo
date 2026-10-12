package app.morfo.ui.compare

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import app.morfo.ui.theme.MorfoColors

private val PanelShape = RoundedCornerShape(22.dp)
private val PanelColor = MorfoColors.Surface.copy(alpha = 0.9f)

/**
 * Secondary content (tooth info, view settings) that never hides the teeth more than necessary:
 * - portrait: a translucent bottom sheet;
 * - landscape: a rounded, translucent panel floating on the left, so the teeth stay visible beside it.
 *
 * [onSidePanelWidth] reports the panel width in px (0 when closed) so the camera can keep the teeth
 * in the free part of the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSheet(
    onDismiss: () -> Unit,
    onSidePanelWidth: (Float) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val config = LocalConfiguration.current
    val landscape = config.screenWidthDp > config.screenHeightDp

    if (!landscape) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MorfoColors.Surface.copy(alpha = 0.94f),
            scrimColor = Color.Black.copy(alpha = 0.25f),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp), content = content)
        }
        return
    }

    BackHandler(onBack = onDismiss)
    DisposableEffect(Unit) { onDispose { onSidePanelWidth(0f) } }
    val visible = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { visible.targetState = true }

    Box(Modifier.fillMaxSize()) {
        // Light scrim: tap outside the panel to close; the teeth stay clearly visible.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.12f))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
        AnimatedVisibility(
            visibleState = visible,
            enter = slideInHorizontally { -it / 3 } + fadeIn(),
            exit = slideOutHorizontally { -it / 3 } + fadeOut(),
        ) {
            Box(
                Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(12.dp)
                    .width(420.dp)
                    .fillMaxHeight()
                    .onGloballyPositioned { onSidePanelWidth(it.size.width.toFloat() + it.positionInParentX()) }
                    .background(PanelColor, PanelShape)
                    .border(1.dp, MorfoColors.Outline, PanelShape)
                    // Swallow taps so they do not reach the scrim.
                    .clickable(remember { MutableInteractionSource() }, indication = null) {},
            ) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 24.dp),
                    content = content,
                )
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Filled.Close, contentDescription = app.morfo.i18n.LocalStrings.current.close, tint = MorfoColors.TextMuted)
                }
            }
        }
    }
}

private fun androidx.compose.ui.layout.LayoutCoordinates.positionInParentX(): Float =
    parentLayoutCoordinates?.let { it.localPositionOf(this, androidx.compose.ui.geometry.Offset.Zero).x } ?: 0f
