package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

/**
 * Custom text input dialog with Persian keyboard.
 * Replaces system keyboard for all text input fields.
 */
@Composable
fun CustomTextInputDialog(
    title: String,
    initialValue: String = "",
    placeholder: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    singleLine: Boolean = true,
    maxLength: Int = 500
) {
    var text by remember { mutableStateOf(initialValue) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        // Hide system keyboard when dialog opens
        keyboardController?.hide()
    }

    Dialog(
        onDismissRequest = {
            keyboardController?.hide()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Text Preview
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    tonalElevation = 1.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* Keep focus */ }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        // Rtl layout: Start = visual right edge, matching the keyboard preview
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                text = placeholder.ifEmpty { "متن را تایپ کنید..." },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                // Rtl layout: Start = visual right edge
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Character count
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "${text.length}/$maxLength",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (text.length > maxLength * 0.9) ExpenseRed
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Persian Keyboard
                PersianKeyboard(
                    text = text,
                    onTextChange = { newText ->
                        if (newText.length <= maxLength) {
                            text = newText
                        }
                    },
                    modifier = Modifier.weight(1f),
                    onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        onConfirm(text.trim())
                    }
                )
            }
        }
    }
}

/**
 * Clickable text field that opens CustomInputDialog when tapped.
 * Use this instead of TextField/OutlinedTextField throughout the app.
 */
@Composable
fun ClickableTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLength: Int = 500
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = { /* Read-only - dialog handles input */ },
        label = { Text(label) },
        modifier = modifier.clickable { showDialog = true },
        shape = RoundedCornerShape(14.dp),
        readOnly = true,
        enabled = false,
        singleLine = singleLine,
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = Color.Transparent
        )
    )

    if (showDialog) {
        CustomTextInputDialog(
            title = label,
            initialValue = value,
            placeholder = placeholder,
            onConfirm = { newText ->
                onValueChange(newText)
                showDialog = false
            },
            onDismiss = { showDialog = false },
            singleLine = singleLine,
            maxLength = maxLength
        )
    }
}
