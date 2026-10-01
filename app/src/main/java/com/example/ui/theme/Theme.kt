package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * تم «هوش حساب نسخه ۲» — همیشه تیره، عیناً مطابق PWA نسخه ۲.
 * مثل PWA دو حالت روشن/تیره وجود ندارد؛ اپ همیشه روی پس‌زمینهٔ
 * بنفش تیره با لهجهٔ طلایی رندر می‌شود.
 */

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = GoldOnDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = GoldPrimary,
    secondary = IndigoLight,
    onSecondary = GoldOnDark,
    secondaryContainer = Color(0xFF3D3D80),
    onSecondaryContainer = Color(0xFFD5D5FF),
    tertiary = AmberAccent,
    onTertiary = GoldOnDark,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextMuted,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceVariantDark,
    surfaceContainerHighest = SurfaceVariantDark,
    outline = CardBorderStrong,
    outlineVariant = CardBorder,
    error = ExpenseRed,
    onError = Color(0xFF1B1735)
)

// سازگاری کامپایل — هرگز استفادهٔ واقعی نمی‌شود (اپ همیشه تیره است)
private val LightColorScheme = DarkColorScheme

@Composable
fun KharjYarTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalUiScale provides rememberUiScale()) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
