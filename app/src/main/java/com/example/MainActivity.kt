package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.AdManager
import com.example.data.AdiveryAds
import com.example.data.model.TransactionType
import com.example.ui.screens.*
import com.example.ui.theme.BgDeep1
import com.example.ui.theme.BgDeep2
import com.example.ui.theme.BgDeep3
import com.example.ui.theme.KharjYarTheme
import com.example.ui.viewmodel.MainViewModel

/**
 * تمام منوی پایین حذف شد. ناوبری حالا فقط از صفحه اصلی انجام می‌شود:
 * HOME → TRANSACTIONS (نمایش هزینه‌ها/درآمدها) | ACCOUNTS (حساب‌ها) | SETTINGS (تنظیمات)
 */
enum class AppView {
    HOME,
    TRANSACTIONS,
    ACCOUNTS,
    SETTINGS
}

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ── تبلیغات ──
        AdiveryAds.init(this)                              // ادیوری: بازگشت به برنامه + میان‌صفحه‌ای
        AdManager.init(applicationContext, lifecycleScope) // بنر آیکونی «پیشنهاد ما»

        setContent {
            KharjYarTheme {
                // Force Persian Right-to-Left (RTL) Layout Direction
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    KharjYarApp(viewModel = viewModel)
                }
            }
        }
    }

    // فلگ: اپ به بک‌گراند رفته است (برای تشخیص «بازگشت واقعی از بیرون»)
    private var wasInBackground = false

    override fun onResume() {
        super.onResume()
        // بازگشت به اپ → اگر بنر تبلیغ شخصی Hidden است فوراً دوباره fetch می‌شود
        AdManager.onAppForegrounded()
        if (AdiveryAds.foregroundJustAfterAd) {
            // تازه یک تبلیغ تمام‌صفحه بسته شده؛ این resume ناشی از بستن آن است
            AdiveryAds.foregroundJustAfterAd = false
        } else if (wasInBackground) {
            // ورود واقعی از بیرون اپ → تبلیغ «بازگشت به برنامه» (یک‌درمیان)
            wasInBackground = false
            AdiveryAds.onAppReturned(this)
        }
    }

    override fun onStop() {
        super.onStop()
        // اگر تبلیغ تمام‌صفحه روی صفحه است، این stop ناشی از خروج کاربر نیست
        if (!AdiveryAds.isShowingFullscreenAd) {
            wasInBackground = true
        }
    }
}

@Composable
fun KharjYarApp(viewModel: MainViewModel) {
    // نسخهٔ ۲: آنبوردینگ، قفل PIN و صفحهٔ اجباری «ساخت اولین حساب» حذف شدند —
    // اپ مستقیم روی صفحهٔ اول باز می‌شود (مطابق PWA)؛ اگر حسابی نباشد،
    // همان‌جا دکمهٔ بزرگ «ساخت اولین حساب» دیده می‌شود.

    var currentView by remember { mutableStateOf(AppView.HOME) }
    var showAddTransactionSheet by remember { mutableStateOf(false) }
    var initialAddType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var showExitDialog by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity

    // ── Back Press: sheet → back to home → exit confirmation ──
    BackHandler {
                when {
                    showAddTransactionSheet -> showAddTransactionSheet = false
                    currentView != AppView.HOME -> {
                        currentView = AppView.HOME
                    }
                    else -> showExitDialog = true
                }
            }

            if (showExitDialog) {
                AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    title = {
                        Text(
                            text = "خروج از برنامه",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    text = {
                        Text(text = "آیا مایل به خروج از هوش حساب هستید؟")
                    },
                    confirmButton = {
                        Button(
                            onClick = { activity?.finish() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("بله، خروج")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExitDialog = false }) {
                            Text("خیر")
                        }
                    }
                )
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            // پس‌زمینهٔ گرادیان بنفش — عیناً مثل body پنل PWA
                            // (linear-gradient(135deg,#0f0c29,#302b63,#24243e))
                            Brush.linearGradient(
                                colors = listOf(BgDeep1, BgDeep2, BgDeep3)
                            )
                        )
                        .padding(innerPadding)
                ) {
                    when (currentView) {
                        AppView.HOME -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToTransactionsWithType = { type ->
                                AdiveryAds.registerButtonClick(activity)
                                viewModel.filterType.value = type.name
                                viewModel.filterCategory.value = null
                                currentView = AppView.TRANSACTIONS
                            },
                            onNavigateToAccounts = {
                                AdiveryAds.registerButtonClick(activity)
                                currentView = AppView.ACCOUNTS
                            },
                            onNavigateToSettings = {
                                AdiveryAds.registerButtonClick(activity)
                                currentView = AppView.SETTINGS
                            },
                            onQuickAddClick = { type ->
                                AdiveryAds.registerButtonClick(activity)
                                initialAddType = type
                                showAddTransactionSheet = true
                            }
                        )

                        AppView.TRANSACTIONS -> TransactionsScreen(
                            viewModel = viewModel,
                            onAddNewTransaction = {
                                AdiveryAds.registerButtonClick(activity)
                                initialAddType = TransactionType.EXPENSE
                                showAddTransactionSheet = true
                            },
                            onBack = {
                                currentView = AppView.HOME
                            },
                            defaultFilterType = viewModel.filterType.value
                        )

                        AppView.ACCOUNTS -> AccountsScreen(
                            viewModel = viewModel,
                            onBack = {
                                currentView = AppView.HOME
                            }
                        )

                        AppView.SETTINGS -> SettingsScreen(
                            viewModel = viewModel,
                            onBack = {
                                currentView = AppView.HOME
                            }
                        )
                    }
                }
            }

            // Quick Add Transaction Modal Bottom Sheet
            if (showAddTransactionSheet) {
                AddTransactionSheet(
                    viewModel = viewModel,
                    initialType = initialAddType,
                    onDismiss = { showAddTransactionSheet = false },
                    onSaved = {
                        AdiveryAds.registerButtonClick(activity)
                        showAddTransactionSheet = false
                    }
                )
            }
}
