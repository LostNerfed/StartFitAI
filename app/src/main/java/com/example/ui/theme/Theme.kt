package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = MonoPrimary,
    onPrimary = Color.Black,
    secondary = MonoSecondary,
    onSecondary = Color.White,
    tertiary = MonoTertiary,
    onTertiary = Color.White,
    background = AmoledBg,
    onBackground = TextPrincipal,
    surface = AmoledSurface,
    onSurface = TextPrincipal,
    surfaceVariant = AmoledBg,
    onSurfaceVariant = TextSecundario,
    outline = BorderColor,
    outlineVariant = BorderColorSubtle
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

// ── Glass Modifiers (matching HomeDailySupercard style) ──

fun Modifier.supercardGlassModifier(
    shape: Shape = RoundedCornerShape(24.dp)
): Modifier = this
    .clip(shape)
    .background(AmoledSurface)

fun Modifier.metricCellGlassModifier(
    shape: Shape = RoundedCornerShape(18.dp)
): Modifier = this
    .clip(shape)
    .background(Color(0xFF1C1C1E))

fun Modifier.liquidGlassModifier(
    shape: Shape = RoundedCornerShape(20.dp)
): Modifier = this
    .clip(shape)
    .background(AmoledSurface)

fun Modifier.glassClickable(
    shape: Shape = RoundedCornerShape(12.dp),
    onClick: () -> Unit
): Modifier = composed {
    this
        .clip(shape)
        .background(AmoledSurface)
        .clickable(onClick = onClick)
}

@Composable
fun SynergyBackground(
    content: @Composable () -> Unit
) {
    val dotPositions = remember {
        listOf(
            0.08f to 0.12f, 0.22f to 0.35f, 0.40f to 0.18f, 0.55f to 0.42f,
            0.70f to 0.10f, 0.85f to 0.28f, 0.15f to 0.55f, 0.35f to 0.65f,
            0.60f to 0.58f, 0.78f to 0.50f, 0.92f to 0.40f, 0.05f to 0.75f,
            0.25f to 0.82f, 0.48f to 0.78f, 0.65f to 0.72f, 0.82f to 0.68f,
            0.12f to 0.90f, 0.38f to 0.92f, 0.55f to 0.88f, 0.72f to 0.85f
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBg)
    ) {
        content()
    }
}

fun Modifier.bounceClick(
    scaleDown: Float = 0.95f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) scaleDown else 1f, label = "bounce")

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            onClick = onClick
        )
}
