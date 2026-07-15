package kz.abdiyev.walletai.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.provider.Settings as AndroidSettings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kz.abdiyev.walletai.sensor.TiltSensorMonitor
import kz.abdiyev.walletai.ui.TiltOffset
import kz.abdiyev.walletai.ui.theme.CardShape
import kotlinx.coroutines.flow.flowOf
import org.koin.compose.koinInject

val LocalTiltOffset = compositionLocalOf { TiltOffset.STATIC }

@Composable
fun rememberDynamicTiltOffset(dynamicEffectsEnabled: Boolean): TiltOffset {
    val context = LocalContext.current
    val tiltMonitor: TiltSensorMonitor = koinInject()
    val animatorDisabled = remember {
        AndroidSettings.Global.getFloat(
            context.contentResolver,
            AndroidSettings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
    val effectsActive = dynamicEffectsEnabled && !animatorDisabled
    val tiltFlow = remember(effectsActive) {
        if (effectsActive) tiltMonitor.tiltOffset else flowOf(TiltOffset.STATIC)
    }
    val tiltOffset by tiltFlow.collectAsStateWithLifecycle(
        initialValue = TiltOffset.STATIC,
        minActiveState = Lifecycle.State.RESUMED
    )
    return tiltOffset
}

@Composable
fun TiltGlowProvider(
    tiltOffset: TiltOffset,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalTiltOffset provides tiltOffset) {
        content()
    }
}

fun Modifier.tiltGlow(
    shape: Shape = CardShape,
    intensity: Float = 0.3f,
    maxShiftDp: Dp = 8.dp,
    glowColor: Color? = null
): Modifier = composed {
    val tilt = LocalTiltOffset.current
    val density = LocalDensity.current
    val resolvedColor = glowColor ?: MaterialTheme.colorScheme.primary
    val maxShiftPx = with(density) { maxShiftDp.toPx() }

    val animatedShift by animateOffsetAsState(
        targetValue = Offset(tilt.x * maxShiftPx, tilt.y * maxShiftPx),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "tiltGlowShift"
    )

    val alpha = intensity.coerceIn(0f, 1f)

    this.drawBehind {
        val center = Offset(
            x = size.width / 2f + animatedShift.x,
            y = size.height / 2f + animatedShift.y
        )
        val radius = maxOf(size.width, size.height) * 0.75f

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    resolvedColor.copy(alpha = alpha),
                    resolvedColor.copy(alpha = alpha * 0.35f),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            )
        )
    }
}
