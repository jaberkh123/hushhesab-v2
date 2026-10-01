package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.AdIconsBanner
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

/**
 * صفحه اول: کارت «موجودی کل حساب‌ها» + دکمه‌های ثبت هزینه/درآمد +
 * ۴ دکمه: نمایش هزینه‌ها، نمایش درآمدها، حساب‌ها، تنظیمات.
 * (خلاصه مالی ماه، بودجه ماه، نمودارها و گزارش‌ها حذف شدند)
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header: Hide Balance toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.toggleHideBalance() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("toggle_hide_balance_btn")
                ) {
                    Icon(
                        imageVector = if (isHideBalance) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = "مخفی کردن موجودی",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Total Balance Hero Card — کلیک روی کارت → مدیریت حساب‌ها
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onNavigateToAccounts() }
                    .testTag("total_balance_card"),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF065F46), Color(0xFF047857), Color(0xFF0F172A))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "موجودی کل حساب‌ها",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFA7F3D0)
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "مدیریت حساب‌ها",
                                    tint = Color(0xFFA7F3D0),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = JalaliCalendarHelper.getCurrentPersianMonthName(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = PersianFormatter.formatMoney(totalBalance, currencyUnit, isHideBalance),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 26.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick action pills inside Hero
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { onQuickAddClick(TransactionType.EXPENSE) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp.scaledDp())
                                    .testTag("quick_expense_btn"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFEF4444).copy(alpha = 0.35f),
                                    contentColor = Color.White
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFEF4444))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ثبت هزینه", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp))
                                }
                            }

                            FilledTonalButton(
                                onClick = { onQuickAddClick(TransactionType.INCOME) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp.scaledDp())
                                    .testTag("quick_income_btn"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF3B82F6).copy(alpha = 0.35f),
                                    contentColor = Color.White
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF60A5FA))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ثبت درآمد", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Quick Navigation: نمایش هزینه‌ها | نمایش درآمدها
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onNavigateToTransactionsWithType(TransactionType.EXPENSE) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp.scaledDp())
                        .testTag("nav_expenses_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                ) {
                    Icon(Icons.Default.TrendingDown, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نمایش هزینه‌ها", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }

                OutlinedButton(
                    onClick = { onNavigateToTransactionsWithType(TransactionType.INCOME) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp.scaledDp())
                        .testTag("nav_incomes_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IncomeGreen),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نمایش درآمدها", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // 4. Quick Navigation: حساب‌ها | تنظیمات
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onNavigateToAccounts() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp.scaledDp())
                        .testTag("nav_accounts_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TransferBlue),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حساب‌ها", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }

                OutlinedButton(
                    onClick = { onNavigateToSettings() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp.scaledDp())
                        .testTag("nav_settings_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B)),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تنظیمات", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // 5. بنر تبلیغات «پیشنهاد ما» (سرور شخصی) — پایین صفحه اول؛
        //    در حالت لودینگ/خطا/بدون اینترنت خودش مخفی می‌شود و هیچ فضایی نمی‌گیرد.
        item {
            AdIconsBanner(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun TransactionItemRow(
    transaction: TransactionEntity,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                        .clip(CircleShape)
                        .background(
                            when {
                                isExpense -> ExpenseRed.copy(alpha = 0.12f)
                                isIncome -> IncomeGreen.copy(alpha = 0.12f)
                                else -> TransferBlue.copy(alpha = 0.12f)
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${transaction.categoryName} • ${accountName ?: "بدون حساب"} • ${JalaliCalendarHelper.formatRelativeOrDate(transaction.dateTimestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Amount
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
