package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.utils.JalaliCalendarHelper
import com.example.core.utils.PersianFormatter
import com.example.data.model.TransactionType
import com.example.ui.components.AdIconsBanner
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

/**
 * صفحه اول — بازطراحی عیناً مطابق PWA نسخهٔ ۲ (hoosha-hesab-v2):
 *   ۱) سربرگ: لوگوی طلایی «📊 هوش حساب» + نمایش تاریخ شمسی امروز
 *   ۲) کارت هیرو: «💰 موجودی کل حساب‌ها» با گرادیان طلایی — کلیک → مدیریت حساب‌ها
 *   ۳) لیست حساب‌ها (پیش‌نمایش مثل acc-list پنل) + حالت خالی «ساخت اولین حساب»
 *   ۴) شبکهٔ ۲×۲ دکمه‌ها: نمایش هزینه | نمایش درآمد | حساب‌ها (طلایی) | تنظیمات
 *   ۵) دکمهٔ بزرگ طلایی «✏️ ثبت هزینه و درآمد»
 *   ۶) بنر تبلیغات «پیشنهاد ما» + پانوشت
 */
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToTransactionsWithType: (TransactionType) -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onQuickAddClick: (TransactionType) -> Unit
) {
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val isHideBalance by viewModel.isHideBalance.collectAsStateWithLifecycle()
    val currencyUnit by viewModel.currencyUnit.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ۱) سربرگ — لوگوی طلایی + تاریخ امروز (مثل header پنل)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 هوش حساب",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        brush = Brush.linearGradient(
                            colors = listOf(GoldGradientStart, GoldGradientEnd)
                        )
                    ),
                    modifier = Modifier.testTag("app_logo")
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // تاریخ شمسی امروز — مثل date-display پنل
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CardBgSoft,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Text(
                            text = JalaliCalendarHelper.formatJalaliDate(System.currentTimeMillis()),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleHideBalance() },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardBgSoft)
                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                            .testTag("toggle_hide_balance_btn")
                    ) {
                        Icon(
                            imageVector = if (isHideBalance) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "مخفی کردن موجودی",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // ۲) کارت هیرو — موجودی کل حساب‌ها (گرادیان طلایی مثل hero-card پنل)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(colors = listOf(HeroBgStart, HeroBgEnd))
                    )
                    .border(1.dp, HeroBorder, RoundedCornerShape(18.dp))
                    .clickable { onNavigateToAccounts() }
                    .testTag("total_balance_card")
                    .padding(vertical = 22.dp, horizontal = 16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "💰 موجودی کل حساب‌ها",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextHeader
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = PersianFormatter.formatMoney(totalBalance, currencyUnit, isHideBalance),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary,
                            fontSize = 30.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (accounts.isEmpty()) "هنوز حسابی نساخته‌اید"
                        else "${PersianFormatter.toPersianDigits(accounts.size.toString())} حساب فعال • برای مدیریت بزنید",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                }
            }
        }

        // ۳) پیش‌نمایش حساب‌ها — مثل acc-list پنل (نام + موجودی آبی)
        if (accounts.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg.copy(alpha = 0.4f))
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    accounts.take(4).forEach { acc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(text = acc.icon, fontSize = 18.sp)
                                Text(
                                    text = acc.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = PersianFormatter.formatMoney(acc.balance, currencyUnit, isHideBalance),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AccountBalanceBlue
                                )
                            )
                        }
                    }
                    if (accounts.size > 4) {
                        Text(
                            text = "و ${PersianFormatter.toPersianDigits((accounts.size - 4).toString())} حساب دیگر…",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // ۴) شبکهٔ ۲×۲ دکمه‌ها — مثل nav-grid پنل
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NavGridButton(
                    text = "📉 نمایش هزینه",
                    onClick = { onNavigateToTransactionsWithType(TransactionType.EXPENSE) },
                    modifier = Modifier.weight(1f).testTag("nav_expenses_btn")
                )
                NavGridButton(
                    text = "📈 نمایش درآمد",
                    onClick = { onNavigateToTransactionsWithType(TransactionType.INCOME) },
                    modifier = Modifier.weight(1f).testTag("nav_incomes_btn")
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // دکمهٔ طلایی حساب‌ها — مثل nav-btn.gold پنل
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp.scaledDp())
                        .clip(RoundedCornerShape(13.dp))
                        .background(Brush.linearGradient(colors = listOf(GoldGradientStart, GoldGradientEnd)))
                        .clickable { onNavigateToAccounts() }
                        .testTag("nav_accounts_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "💳 حساب‌ها",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = GoldOnDark
                        )
                    )
                }
                NavGridButton(
                    text = "⚙️ تنظیمات",
                    onClick = { onNavigateToSettings() },
                    modifier = Modifier.weight(1f).testTag("nav_settings_btn")
                )
            }
        }

        // ۵) دکمهٔ بزرگ طلایی ثبت هزینه و درآمد — مثل btn-submit پنل
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp.scaledDp())
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(colors = listOf(GoldGradientStart, GoldGradientEnd)))
                    .clickable { onQuickAddClick(TransactionType.EXPENSE) }
                    .testTag("quick_add_btn"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✏️ ثبت هزینه و درآمد",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldOnDark
                    )
                )
            }
        }

        // ۶) بنر تبلیغات «پیشنهاد ما» (سرور شخصی) — در حالت لودینگ/خطا مخفی می‌شود
        item {
            AdIconsBanner(modifier = Modifier.fillMaxWidth())
        }

        // ۷) پانوشت — مثل footer پنل
        item {
            Text(
                text = "اطلاعات به‌صورت خودکار و آفلاین روی همین گوشی ذخیره می‌شود",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF71717F),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/** دکمهٔ معمولی شبکهٔ ۲×۲ — پس‌زمینهٔ سفید نیمه‌شفاف با حاشیه (nav-btn پنل). */
@Composable
private fun NavGridButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(50.dp.scaledDp())
            .clip(RoundedCornerShape(13.dp))
            .background(CardBgSoft)
            .border(1.dp, CardBorderStrong, RoundedCornerShape(13.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
    }
}

@Composable
fun TransactionItemRow(
    transaction: com.example.data.model.TransactionEntity,
    isHideBalance: Boolean,
    currencyUnit: String,
    accountName: String? = null,
    onClick: () -> Unit
) {
    val isExpense = transaction.type == TransactionType.EXPENSE.name
    val isIncome = transaction.type == TransactionType.INCOME.name

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBg.copy(alpha = 0.55f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Icon Bubble
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            when {
                                isExpense -> ExpenseRed.copy(alpha = 0.14f)
                                isIncome -> IncomeGreen.copy(alpha = 0.14f)
                                else -> TransferBlue.copy(alpha = 0.14f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = transaction.categoryIcon.ifEmpty { if (isExpense) "🍔" else if (isIncome) "💼" else "🔄" },
                        fontSize = 22.sp
                    )
                }

                Column {
                    Text(
                        text = if (transaction.note.isNotBlank()) transaction.note else transaction.categoryName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${transaction.categoryName} • ${accountName ?: "بدون حساب"} • ${JalaliCalendarHelper.formatRelativeOrDate(transaction.dateTimestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            // Amount — رنگ‌های دقیق PWA: هزینه قرمز، درآمد سبز
            Column(horizontalAlignment = Alignment.End) {
                val prefix = when {
                    isExpense -> "-"
                    isIncome -> "+"
                    else -> "⇄"
                }
                Text(
                    text = if (isHideBalance) "••••••••" else "$prefix ${PersianFormatter.formatMoney(transaction.amount, currencyUnit)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = when {
                        isExpense -> ExpenseRed
                        isIncome -> IncomeGreen
                        else -> TransferBlue
                    }
                )
            }
        }
    }
}
