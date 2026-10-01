package com.example.core.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecurityManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("kharjyar_prefs", Context.MODE_PRIVATE)
    private val securePrefs: SharedPreferences =
        context.getSharedPreferences("kharjyar_secure_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN = "security_pin"
        private const val KEY_PIN_ENABLED = "pin_enabled"
        private const val KEY_HIDE_BALANCE = "hide_balance"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_DARK_MODE = "dark_mode_pref" // "SYSTEM", "LIGHT", "DARK"
        private const val KEY_CURRENCY = "currency_unit" // "تومان", "ریال", "دلار"
        private const val KEY_BUDGET_ALARM = "budget_alarm_enabled"

        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val PIN_KEY_ALIAS = "kharjyar_pin_key"
        private const val GCM_IV_LENGTH_BYTES = 12
    }

    var isPinEnabled: Boolean
        get() = prefs.getBoolean(KEY_PIN_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PIN_ENABLED, value).apply()

    var pinCode: String
        get() {
            // One-time migration from the legacy plaintext pref to encrypted storage.
            val legacy = prefs.getString(KEY_PIN, null)
            if (legacy != null) {
                prefs.edit().remove(KEY_PIN).apply()
                if (legacy.isNotEmpty()) {
                    val encrypted = encryptPin(legacy)
                    if (encrypted != null) {
                        securePrefs.edit().putString(KEY_PIN, encrypted).apply()
                    } else {
                        // Keystore unavailable — keep working with the legacy location.
                        prefs.edit().putString(KEY_PIN, legacy).apply()
                        return legacy
                    }
                }
            }

            val stored = securePrefs.getString(KEY_PIN, null) ?: return ""
            return decryptPin(stored) ?: ""
        }
        set(value) {
            if (value.isEmpty()) {
                securePrefs.edit().remove(KEY_PIN).apply()
                prefs.edit().remove(KEY_PIN).apply()
                return
            }
            val encrypted = encryptPin(value)
            if (encrypted != null) {
                securePrefs.edit().putString(KEY_PIN, encrypted).apply()
                prefs.edit().remove(KEY_PIN).apply()
            } else {
                // Keystore unavailable — fall back to the legacy location so the
                // PIN feature keeps working instead of locking the user out.
                prefs.edit().putString(KEY_PIN, value).apply()
            }
        }

    var isBudgetAlarmEnabled: Boolean
        get() = prefs.getBoolean(KEY_BUDGET_ALARM, false)
        set(value) = prefs.edit().putBoolean(KEY_BUDGET_ALARM, value).apply()

    var isHideBalance: Boolean
        get() = prefs.getBoolean(KEY_HIDE_BALANCE, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_BALANCE, value).apply()

    var isOnboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    var darkModePref: String
        get() = prefs.getString(KEY_DARK_MODE, "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString(KEY_DARK_MODE, value).apply()

    var currencyUnit: String
        get() = prefs.getString(KEY_CURRENCY, "تومان") ?: "تومان"
        set(value) = prefs.edit().putString(KEY_CURRENCY, value).apply()

    fun verifyPin(enteredPin: String): Boolean {
        if (!isPinEnabled || pinCode.isEmpty()) return true
        return pinCode == enteredPin
    }

    // ── PIN encryption (AES/GCM via AndroidKeyStore) ──────────────────

    private fun getOrCreatePinKey(): SecretKey? = try {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(PIN_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
            ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
                init(
                    KeyGenParameterSpec.Builder(
                        PIN_KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                )
            }.generateKey()
    } catch (e: Exception) {
        null
    }

    private fun encryptPin(plain: String): String? = try {
        val key = getOrCreatePinKey() ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        Base64.encodeToString(iv + cipherText, Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }

    private fun decryptPin(stored: String): String? = try {
        val key = getOrCreatePinKey() ?: return null
        val data = Base64.decode(stored, Base64.NO_WRAP)
        val iv = data.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val cipherText = data.copyOfRange(GCM_IV_LENGTH_BYTES, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        String(cipher.doFinal(cipherText), Charsets.UTF_8)
    } catch (e: Exception) {
        null
    }
}
