package com.example.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.core.utils.PersianFormatter
import com.example.ui.theme.*

@Composable
fun LockScreen(
    onUnlockSuccess: () -> Unit,
    onVerifyPin: (String) -> Boolean
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Biometric availability: shown only when the device has enrolled hardware.
    val biometricStatus = remember {
        val bm = BiometricManager.from(context)
        bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
    }
    val canUseBiometric = biometricStatus == BiometricManager.BIOMETRIC_SUCCESS

    fun showBiometricPrompt() {
        val fragmentActivity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(fragmentActivity)
        val prompt = BiometricPrompt(
            fragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlockSuccess()
                }
                // onAuthenticationError / onAuthenticationFailed: stay locked;
                // the user can retry via the button or fall back to the PIN.
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("ورود به هوش حساب")
            .setSubtitle("احراز هویت با اثر انگشت")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        prompt.authenticate(promptInfo)
    }

    // Offer biometric unlock automatically when the lock screen appears.
    LaunchedEffect(canUseBiometric) {
        if (canUseBiometric) showBiometricPrompt()
    }

    fun handleDigit(d: String) {
        if (enteredPin.length < 4) {
            val newPin = enteredPin + d
            enteredPin = newPin
            isError = false
            if (newPin.length == 4) {
                if (onVerifyPin(newPin)) {
                    onUnlockSuccess()
                } else {
                    isError = true
                    enteredPin = ""
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "قفل",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "ورود به هوش حساب",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isError) "رمز عبور نادرست است. دوباره تلاش کنید."
                    else "رمز ۴ رقمی خود را وارد کنید",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val filled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filled) EmeraldPrimary
                                    else Color.Transparent
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (filled) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            // Keypad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf(if (canUseBiometric) "BIO" else "", "0", "DEL")
                )

                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        row.forEach { key ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp.scaledDp())
                                    .clip(CircleShape)
                                    .clickable {
                                        when (key) {
                                            "DEL" -> {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                    isError = false
                                                }
                                            }
                                            "BIO" -> showBiometricPrompt()
                                            // "" → empty placeholder slot, not clickable
                                            else -> if (key.isNotEmpty()) handleDigit(key)
                                        }
                                    }
                                    .testTag("pin_key_$key"),
                                color = if (key == "BIO" || key == "DEL") Color.Transparent
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    when (key) {
                                        "DEL" -> Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "حذف",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        "BIO" -> Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "اثر انگشت",
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        "" -> {}
                                        else -> Text(
                                            text = PersianFormatter.toPersianDigits(key),
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
