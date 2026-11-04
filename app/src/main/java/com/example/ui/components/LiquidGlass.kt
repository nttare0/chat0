package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Liquid Glass shader-inspired aesthetics based on dashersw/liquid-glass-js.
 * Implements refractive specular rim highlights, chromatic aberration edge tints,
 * and dynamic multi-layer liquid fluid background refraction in Jetpack Compose.
 */

// Instagram signature messenger berry/sunset gradient tokens
val InstagramGradientBerry = listOf(
    Color(0xFF782FEF), // Vibrant Violet
    Color(0xFFBC35B8), // Fuchsia / Berry
    Color(0xFFFA7E1E)  // Warm Sunset Coral
)

val InstagramIncomingGlassDark = Color(0xFF26262E).copy(alpha = 0.85f)
val InstagramIncomingGlassLight = Color(0xFFF1F5F9).copy(alpha = 0.88f)

val InstagramBlueAction = Color(0xFF0095F6)
val InstagramHeartRed = Color(0xFFFF3040)

/**
 * Ambient Liquid Glass Canvas Background:
 * Emulates the WebGL multi-layer fluid refraction of liquid-glass-js by drawing
 * soft, floating chromatic orbs beneath glass surfaces, causing the frosted glass
 * bubbles and bars to refract vibrant fluid light.
 */
@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_glass_fluid")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fluid_motion"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0F))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Deep base obsidian gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A0E),
                        Color(0xFF101017),
                        Color(0xFF09090D)
                    )
                )
            )

            // Liquid Orb 1: Upper right violet-purple fluid blob
            val orb1X = width * 0.82f + sin(animOffset.toDouble()).toFloat() * (width * 0.08f)
            val orb1Y = height * 0.22f + cos(animOffset.toDouble()).toFloat() * (height * 0.05f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x38833AB4), // Violet glow
                        Color(0x18782FEF),
                        Color.Transparent
                    ),
                    center = Offset(orb1X, orb1Y),
                    radius = width * 0.65f
                ),
                radius = width * 0.65f,
                center = Offset(orb1X, orb1Y)
            )

            // Liquid Orb 2: Center-left sunset magenta-coral fluid blob
            val orb2X = width * 0.15f + cos((animOffset * 0.8f).toDouble()).toFloat() * (width * 0.06f)
            val orb2Y = height * 0.58f + sin((animOffset * 0.8f).toDouble()).toFloat() * (height * 0.07f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x28C13584), // Magenta/Berry glow
                        Color(0x14FA7E1E), // Subtle Coral
                        Color.Transparent
                    ),
                    center = Offset(orb2X, orb2Y),
                    radius = width * 0.70f
                ),
                radius = width * 0.70f,
                center = Offset(orb2X, orb2Y)
            )

            // Liquid Orb 3: Bottom luminous cyan-violet refraction pool
            val orb3X = width * 0.50f + sin((animOffset * 1.2f).toDouble()).toFloat() * (width * 0.10f)
            val orb3Y = height * 0.90f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x2400F2FE), // Cyan refraction
                        Color(0x144FACFE),
                        Color.Transparent
                    ),
                    center = Offset(orb3X, orb3Y),
                    radius = width * 0.55f
                ),
                radius = width * 0.55f,
                center = Offset(orb3X, orb3Y)
            )
        }

        content()
    }
}

/**
 * Modifier that applies liquid-glass-js inspired refractive specular rim lighting,
 * frosted glass translucent surface, and subtle chromatic sheen.
 */
fun Modifier.liquidGlassEffect(
    shape: Shape = RoundedCornerShape(22.dp),
    tintColor: Color = Color(0xFF1E1E26).copy(alpha = 0.75f),
    specularHighlightAlpha: Float = 0.35f,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(tintColor)
    .drawWithCache {
        // Specular rim gradient mimicking refractive light bending along rounded normals
        val specularBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = specularHighlightAlpha),
                Color(0xFF00F2FE).copy(alpha = specularHighlightAlpha * 0.45f), // subtle cyan chromatic aberration
                Color.White.copy(alpha = specularHighlightAlpha * 0.15f),
                Color.Transparent
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        )

        onDrawWithContent {
            drawContent()
            drawOutline(specularBrush, borderWidth.toPx())
        }
    }

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOutline(
    brush: Brush,
    strokeWidthPx: Float
) {
    drawRoundRect(
        brush = brush,
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f),
        style = Stroke(width = strokeWidthPx)
    )
}

/**
 * A reusable Liquid Glass Pill Surface with specular rim lighting and translucent frosted tint.
 */
@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    backgroundColor: Color = Color(0xFF1C1C24).copy(alpha = 0.82f),
    specularAlpha: Float = 0.32f,
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    val specularBorder = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = specularAlpha),
            Color(0xFF782FEF).copy(alpha = specularAlpha * 0.4f),
            Color.White.copy(alpha = specularAlpha * 0.12f),
            Color.White.copy(alpha = 0.04f)
        ),
        start = Offset(0f, 0f),
        end = Offset(600f, 600f)
    )

    Surface(
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(borderWidth, specularBorder),
        modifier = modifier
    ) {
        content()
    }
}
