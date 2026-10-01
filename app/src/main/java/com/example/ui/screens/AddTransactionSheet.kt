package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.utils.PersianFormatter
import com.example.data.model.*
import com.example.ui.components.KharjYarNumberPad
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    viewModel: MainViewModel,
    initialType: TransactionType = TransactionType.EXPENSE,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var rawAmountString by remember { mutableStateOf("") }
    var selectedCategory by remember {
        mutableStateOf(
            if (initialType == TransactionType.INCOME) DefaultCategories.incomeCategories.first()
            else DefaultCategories.expenseCategories.first()
        )
    }

    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    // 0 = no account selected yet; a real id is picked as soon as accounts load.
    // Never fall back to a hardcoded id like 1 — that silently corrupted balances.
    var selectedSourceAccountId by remember { mutableStateOf(0L) }

    var isShowingNumpad by remember { mutableStateOf(true) }



    LaunchedEffect(selectedType) {
        selectedCategory = if (selectedType == TransactionType.INCOME) {
            DefaultCategories.incomeCategories.first()
        } else {
            DefaultCategories.expenseCategories.first()
        }
    }

    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty()) {
            if (accounts.none { it.id == selectedSourceAccountId }) {
                selectedSourceAccountId = accounts.first().id
            }
        }
    }

    val amountLong = rawAmountString.toLongOrNull() ?: 0L
    val keyboardController = LocalSoftwareKeyboardController.current

    // A transaction must target a real account. Prevents saving rows that
    // corrupt or vanish. Transfers simply move money out of the selected
    // account — no destination account is chosen in the UI anymore.
    val hasValidSource = accounts.any { it.id == selectedSourceAccountId }
    val canSave = amountLong > 0 && hasValidSource

    ModalBottomSheet(
        onDismissRequest = {
            keyboardController?.hide()
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Scrollable content area: takes only as much space as its content
            // needs, but scrolls whenever the screen (or the on-screen
            // keyboard) cannot fit everything, so no field is ever cut off.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // 1. Tab Selector
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp.scaledDp()),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        TransactionType.EXPENSE to "هزینه",
                        TransactionType.INCOME to "درآمد",
                        TransactionType.TRANSFER to "انتقال"
                    ).forEach { (type, label) ->
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) {
                                        when (type) {
                                            TransactionType.EXPENSE -> ExpenseRed
                                            TransactionType.INCOME -> IncomeGreen
                                            TransactionType.TRANSFER -> TransferBlue
                                        }
                                    } else Color.Transparent
                                )
                                .clickable { selectedType = type }
                                .testTag("type_tab_${type.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Amount Display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { isShowingNumpad = !isShowingNumpad }
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "مبلغ تراکنش",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (amountLong == 0L) "۰ تومان" else "${PersianFormatter.formatNumberWithCommas(amountLong)} تومان",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = when (selectedType) {
                            TransactionType.EXPENSE -> ExpenseRed
                            TransactionType.INCOME -> IncomeGreen
                            TransactionType.TRANSFER -> TransferBlue
                        }
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Number Pad
            AnimatedVisibility(visible = isShowingNumpad) {
                Column {
                    KharjYarNumberPad(
                        onDigitClick = { digit ->
                            if (rawAmountString.length < 11) {
                                rawAmountString += digit
                            }
                        },
                        onBackspaceClick = {
                            if (rawAmountString.isNotEmpty()) {
                                rawAmountString = rawAmountString.dropLast(1)
                            }
                        },
                        onClearClick = { rawAmountString = "" },
                        onQuickAdd = { increment ->
                            val cur = rawAmountString.toLongOrNull() ?: 0L
                            rawAmountString = (cur + increment).toString()
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 5. Category Dropdown
            if (selectedType != TransactionType.TRANSFER) {
                var catExpanded by remember { mutableStateOf(false) }
                val catList = if (selectedType == TransactionType.EXPENSE) DefaultCategories.expenseCategories
                              else DefaultCategories.incomeCategories

                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                    OutlinedTextField(
                        value = "${selectedCategory.icon} ${selectedCategory.name}",
                        onValueChange = {}, readOnly = true,
                        label = { Text("دسته‌بندی") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                        leadingIcon = { Icon(Icons.Default.Category, null, tint = EmeraldPrimary) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        catList.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.icon} ${cat.name}", fontWeight = if (selectedCategory.id == cat.id) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { selectedCategory = cat; catExpanded = false },
                                leadingIcon = if (selectedCategory.id == cat.id) {
                                    { Icon(Icons.Default.Check, null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 6. Account Dropdown
            // 6. Account Dropdown — single account selector for all types.
            // Transfers move money out of the selected account only; no
            // destination account is needed anymore.
            var accExpanded by remember { mutableStateOf(false) }
            val accountLabel = when (selectedType) {
                TransactionType.EXPENSE -> "حساب پرداختی"
                TransactionType.INCOME -> "حساب دریافت‌کننده"
                TransactionType.TRANSFER -> "از حساب"
            }
            val accName = accounts.find { it.id == selectedSourceAccountId }?.let { "${it.icon} ${it.name} (${PersianFormatter.formatMoney(it.balance)})" } ?: ""
            ExposedDropdownMenuBox(expanded = accExpanded, onExpandedChange = { accExpanded = it }) {
                OutlinedTextField(value = accName, onValueChange = {}, readOnly = true,
                    label = { Text(accountLabel) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(accExpanded) },
                    leadingIcon = { Icon(Icons.Default.AccountBalance, null, tint = EmeraldPrimary) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = MaterialTheme.colorScheme.outline))
                ExposedDropdownMenu(expanded = accExpanded, onDismissRequest = { accExpanded = false }) {
                    accounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text("${acc.icon} ${acc.name} (${PersianFormatter.formatMoney(acc.balance)})") },
                            onClick = { selectedSourceAccountId = acc.id; accExpanded = false },
                            leadingIcon = if (selectedSourceAccountId == acc.id) { { Icon(Icons.Default.Check, null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp)) } } else null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            }

            // 9. Save Button — pinned to the bottom of the sheet (outside the
            // scrollable area) so it is always visible and tappable on every
            // screen size. Fixes the bug where the button ended up below the
            // visible area and the transaction could not be saved.
            Button(
                onClick = {
                    keyboardController?.hide()
                    if (!canSave) return@Button
                    val transaction = TransactionEntity(
                            accountId = selectedSourceAccountId,
                            destinationAccountId = null,
                            type = selectedType.name,
                            amount = amountLong,
                            categoryId = if (selectedType == TransactionType.TRANSFER) "transfer" else selectedCategory.id,
                            categoryName = if (selectedType == TransactionType.TRANSFER) "انتقال وجه" else selectedCategory.name,
                            categoryIcon = if (selectedType == TransactionType.TRANSFER) "🔄" else selectedCategory.icon,
                            note = ""
                        )
                        viewModel.addTransaction(transaction)
                        rawAmountString = ""
                        onSaved()
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 20.dp)
                    .height(56.dp.scaledDp())
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (selectedType) {
                        TransactionType.EXPENSE -> ExpenseRed
                        TransactionType.INCOME -> IncomeGreen
                        TransactionType.TRANSFER -> TransferBlue
                    }
                )
            ) {
                Text(
                    text = when (selectedType) {
                        TransactionType.EXPENSE -> "💸 ثبت هزینه"
                        TransactionType.INCOME -> "💰 ثبت درآمد"
                        TransactionType.TRANSFER -> "🔄 انتقال وجه"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
