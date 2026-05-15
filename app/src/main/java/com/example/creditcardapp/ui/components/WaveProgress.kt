package com.example.creditcardapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WaveProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = 8.dp,
    amplitude: Dp = 2.dp
) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave-phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
    ) {
        drawTrack(trackColor)
        if (progress > 0f) drawWave(progress.coerceIn(0f, 1f), phase, amplitude.toPx(), color)
    }
}

@Composable
fun IndeterminateWave(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = 6.dp
) {
    val transition = rememberInfiniteTransition(label = "indeterminate")
    val shift by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shift"
    )
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
    ) {
        drawTrack(trackColor)
        val w = size.width
        val barW = w * 0.4f
        val x = shift * w - barW / 2f
        val brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, color, Color.Transparent),
            startX = x,
            endX = x + barW
        )
        drawRect(brush = brush, topLeft = Offset(0f, 0f), size = Size(w, size.height))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrack(color: Color) {
    drawRect(color = color, size = size)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWave(
    progress: Float,
    phase: Float,
    amplitude: Float,
    color: Color
) {
    val width = size.width * progress
    val centerY = size.height / 2f
    val effectiveAmp = amplitude.coerceAtMost(size.height / 2.2f)
    val wavelength = size.height * 4f
    val path = Path().apply {
        moveTo(0f, centerY)
        var x = 0f
        while (x <= width) {
            val y = centerY + sin((x / wavelength) * 2 * PI + phase) * effectiveAmp
            lineTo(x, y.toFloat())
            x += 2f
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = size.height * 0.6f)
    )
}
