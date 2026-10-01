package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/**
 * Responsive scaling for KharjYar.
 *
 * All fixed dimensions (button heights, numpad keys, key paddings) can be
 * multiplied by [LocalUiScale] so the UI stays proportional on every phone:
 * small screens shrink components instead of pushing them off-screen, and
 * large screens grow them slightly.
 *
 * Baseline "design" screen is 360 x 720 dp (typical Android phone). The scale
 * is the geometric mean of the width and height ratios, clamped so extreme
 * devices (tablets, tiny phones) stay sane.
 */
val LocalUiScale = compositionLocalOf { 1f }

private const val BASE_WIDTH_DP = 360
private const val BASE_HEIGHT_DP = 720

private const val MIN_UI_SCALE = 0.82f
private const val MAX_UI_SCALE = 1.25f

@Composable
fun rememberUiScale(): Float {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp
    val heightDp = configuration.screenHeightDp
    return remember(widthDp, heightDp) {
        val widthRatio = widthDp.toFloat() / BASE_WIDTH_DP
        val heightRatio = heightDp.toFloat() / BASE_HEIGHT_DP
        sqrt(widthRatio * heightRatio).coerceIn(MIN_UI_SCALE, MAX_UI_SCALE)
    }
}

/**
 * Scales a dp value with the current UI scale.
 * Usage: `Modifier.height(56.dp.scaledDp())` — must be called from a
 * composable context.
 */
@Composable
fun Dp.scaledDp(): Dp = (value * LocalUiScale.current).dp

/** Non-composable variant for use with a pre-computed scale factor. */
fun Dp.scaledBy(scale: Float): Dp = (value * scale).dp