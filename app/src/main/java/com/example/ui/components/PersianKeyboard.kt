package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Persian keyboard layout for custom text input.
 * Supports lowercase/uppercase Persian letters, numbers, and symbols.
 */
@Composable
fun PersianKeyboard(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onDone: (() -> Unit)? = null
) {
    var isUpperCase by remember { mutableStateOf(false) }
    var isSymbolMode by remember { mutableStateOf(false) }

    val lowercaseLayout = listOf(
        listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج"),
        listOf("ش", "س", "ی", "ب", "ل", "ا", "ت", "ن", "م", "ک", "گ"),
        listOf("ظ", "ط", "ز", "ر", "ذ", "د", "پ", "و", "ژ", "چ")
    )

    val uppercaseLayout = listOf(
        listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج"),
        listOf("ش", "س", "ی", "ب", "ل", "ا", "ت", "ن", "م", "ک", "گ"),
        listOf("ظ", "ط", "ز", "ر", "ذ", "د", "پ", "و", "ژ", "چ")
    )

    val symbolLayout = listOf(
        listOf("۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹", "۰"),
        listOf("-", "=", "(", ")", "[", "]", "{", "}", "/", "\\"),
        listOf("@", "#", "$", "%", "&", "*", "+", "=", "<", ">")
    )

    val persianNumbers = mapOf(
        "0" to "۰", "1" to "۱", "2" to "۲", "3" to "۳", "4" to "۴",
        "5" to "۵", "6" to "۶", "7" to "۷", "8" to "۸", "9" to "۹"
    )

    val currentLayout = if (isSymbolMode) {
        symbolLayout
    } else if (isUpperCase) {
        uppercaseLayout
    } else {
        lowercaseLayout
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Text display
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                // Layout direction is Rtl here, so CenterStart = visual right edge.
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = if (text.isEmpty()) "متن را تایپ کنید..." else text,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                    color = if (text.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Number quick access row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            listOf("۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹").forEach { num ->
                KeyButton(
                    text = num,
                    onClick = { onTextChange(text + num) },
                    modifier = Modifier.weight(1f),
                    height = 42
                )
            }
        }

        // Main letter rows
        currentLayout.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                row.forEach { char ->
                    KeyButton(
                        text = char,
                        onClick = {
                            onTextChange(text + char)
                        },
                        modifier = Modifier.weight(1f),
                        height = 42
                    )
                }
            }
        }

        // Bottom row: symbols, space, backspace, done
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Symbol toggle
            Surface(
                modifier = Modifier
                    .weight(1.2f)
                    .height(46.dp.scaledDp())
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        isSymbolMode = !isSymbolMode
                    },
                color = if (isSymbolMode) EmeraldPrimary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "۱۲۳",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSymbolMode) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Shift (only in letter mode)
            if (!isSymbolMode) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp.scaledDp())
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { isUpperCase = !isUpperCase },
                    color = if (isUpperCase) EmeraldPrimary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Shift",
                            tint = if (isUpperCase) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Space
            Surface(
                modifier = Modifier
                    .weight(4f)
                    .height(46.dp.scaledDp())
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onTextChange(text + " ") },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "فضا",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Backspace
            Surface(
                modifier = Modifier
                    .weight(1.2f)
                    .height(46.dp.scaledDp())
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        if (text.isNotEmpty()) {
                            onTextChange(text.dropLast(1))
                        }
                    },
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "پاک کردن",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Done button
        if (onDone != null) {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp.scaledDp()),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(
                    text = "تأیید",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
    }
}

@Composable
private fun KeyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Int = 46
) {
    Surface(
        modifier = modifier
            .height(height.dp.scaledDp())
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, fontSize = 20.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
