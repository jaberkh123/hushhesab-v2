package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.utils.PersianFormatter
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.ui.components.AmountNumpadBottomSheet
import com.example.ui.components.ClickableTextInput
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

/**
 * First-run gate screen: shown instead of the main app while the user has no
 * accounts yet. Creating the first account is mandatory because expenses,
 * income and transfers all target an account. The screen disappears
 * automatically as soon as Room emits the newly created account.
 */
@Composable
fun FirstAccountSetupScreen(
    viewModel: MainViewModel
) {
    var accName by remember { mutableStateOf("") }
    var accType by remember { mutableStateOf(AccountType.BANK) }
    var accIcon by remember { mutableStateOf("🏦") }
    var initialBalance by remember { mutableStateOf(0L) }
    var showBalanceSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("first_account_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Hero icon
            Box(
                modifier = Modifier
                    .size(88.dp.scaledDp())
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(EmeraldPrimary, EmeraldLight))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "💼", fontSize = 44.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "خوش آمدید! 👋",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "برای ثبت هزینه و درآمد، ابتدا اولین حساب خود را بسازید. موجودی فعلی‌تان را وارد کنید تا از همان‌جا شروع کنیم.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Form card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    ClickableTextInput(
                        value = accName,
                        onValueChange = { accName = it },
                        label = "نام حساب یا کارت (مثلاً: بانک پاسارگاد)",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "نوع حساب:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            AccountType.BANK to "🏦",
                            AccountType.CARD to "💳",
                            AccountType.CASH to "💵"
                        ).forEach { (t, ic) ->
                            FilterChip(
                                selected = accType == t,
                                onClick = {
                                    accType = t
                                    accIcon = ic
                                },
                                label = { Text("$ic ${t.titleFa}") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Initial balance row → opens the numpad sheet
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showBalanceSheet = true }
                            .testTag("first_account_balance_row"),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "موجودی اولیه",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (initialBalance > 0)
                                    "${PersianFormatter.formatNumberWithCommas(initialBalance)} تومان"
                                else "مبلغ را وارد کنید",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (initialBalance > 0) EmeraldPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (accName.isNotBlank()) {
                        viewModel.addAccount(
                            AccountEntity(
                                name = accName.trim(),
                                type = accType.name,
                                balance = initialBalance,
                                icon = accIcon
                            )
                        )
                    }
                },
                enabled = accName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp.scaledDp())
                    .testTag("create_first_account_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(
                    text = "ساخت حساب و شروع",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    if (showBalanceSheet) {
        AmountNumpadBottomSheet(
            title = "موجودی اولیه حساب",
            amountLabel = "موجودی فعلی (تومان)",
            initialAmount = initialBalance,
            onConfirm = { balance ->
                initialBalance = balance
                showBalanceSheet = false
            },
            onDismiss = { showBalanceSheet = false }
        )
    }
}