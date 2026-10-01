package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.utils.JalaliCalendarHelper
import com.example.core.utils.PersianFormatter
import com.example.data.model.DefaultCategories
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.CustomTextInputDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: MainViewModel,
    onAddNewTransaction: () -> Unit,
    onBack: () -> Unit,
    defaultFilterType: String? = null
) {

    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedType by viewModel.filterType.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.filterCategory.collectAsStateWithLifecycle()
    val selectedDateRange by viewModel.filterDateRange.collectAsStateWithLifecycle()
    val isHideBalance by viewModel.isHideBalance.collectAsStateWithLifecycle()
    val currencyUnit by viewModel.currencyUnit.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    // Resolve account names once — «بدون حساب» for detached transactions (deleted account)
    val accountNames = remember(accounts) {
        accounts.associate { it.id to "${it.icon} ${it.name}" }
    }

    // Apply initial filter type from navigation
    LaunchedEffect(defaultFilterType) {
        if (defaultFilterType != null) {
            viewModel.filterType.value = defaultFilterType
        }
    }

    // Changing the type filter resets the category filter (default: همه دسته‌ها)
    LaunchedEffect(selectedType) {
        viewModel.filterCategory.value = null
    }

    var transactionToEditOrDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }

    // Date grouping
    val groupedTransactions: Map<String, List<TransactionEntity>> = remember(transactions) {
        transactions.groupBy { tx: TransactionEntity ->
            JalaliCalendarHelper.formatRelativeOrDate(tx.dateTimestamp)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Text(
                        text = "لیست تراکنش‌ها",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewTransaction,
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .testTag("fab_add_transaction")
            ) {
                Icon(Icons.Default.Add, contentDescription = "ثبت تراکنش جدید")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Search Bar (Clickable -> Opens custom Persian keyboard)
            // Compact height + smaller text so it doesn't dominate the toolbar area.
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { },
                placeholder = { Text(
                    "جستجو در نام، دسته، توضیحات...",
                    style = MaterialTheme.typography.bodySmall
                ) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "جستجو", modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "پاک کردن", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { showSearchDialog = true }
                    .testTag("transactions_search_bar"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                readOnly = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContainerColor = Color.Transparent,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Filter Chips: Type (همه، هزینه، درآمد، انتقال)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val types = listOf(
                    "ALL" to "همه",
                    TransactionType.EXPENSE.name to "هزینه‌ها",
                    TransactionType.INCOME.name to "درآمدها",
                    TransactionType.TRANSFER.name to "انتقال‌ها"
                )
                items(types) { (typeKey, label) ->
                    val isSelected = selectedType == typeKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.filterType.value = typeKey },
                        label = { Text(label) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("filter_chip_$typeKey")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2.5 Filter Chips: Category (پیش‌فرض: همه دسته‌ها)
            // Categories follow the selected type; for transfers only «همه» is shown.
            val categoryOptions: List<Pair<String, String>> = remember(selectedType) {
                when (selectedType) {
                    TransactionType.EXPENSE.name ->
                        DefaultCategories.expenseCategories.map { it.id to "${it.icon} ${it.name}" }
                    TransactionType.INCOME.name ->
                        DefaultCategories.incomeCategories.map { it.id to "${it.icon} ${it.name}" }
                    else ->
                        (DefaultCategories.expenseCategories + DefaultCategories.incomeCategories)
                            .map { it.id to "${it.icon} ${it.name}" }
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categoryChips = listOf<Pair<String?, String>>(null to "همه دسته‌ها") + categoryOptions
                items(categoryChips) { (catId, label) ->
                    val isSelected = selectedCategory == catId
                    SuggestionChip(
                        onClick = { viewModel.filterCategory.value = catId },
                        label = {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isSelected) TransferBlue.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = if (isSelected) TransferBlue else Color.Transparent
                        ),
                        modifier = Modifier.testTag("category_chip_${catId ?: "ALL"}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Filter Chips: Date Range (امروز، این هفته، این ماه، همه)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dateRanges = listOf(
                    "ALL" to "کل زمان‌ها",
                    "TODAY" to "امروز",
                    "THIS_WEEK" to "این هفته",
                    "THIS_MONTH" to "این ماه"
                )
                items(dateRanges) { (dateKey, label) ->
                    val isSelected = selectedDateRange == dateKey
                    SuggestionChip(
                        onClick = { viewModel.filterDateRange.value = dateKey },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isSelected) EmeraldPrimary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = if (isSelected) EmeraldPrimary else Color.Transparent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Summary Banner ──────────────────────────────────────
            if (transactions.isNotEmpty()) {
                val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
                val totalIncome = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
                val totalTransfer = transactions.filter { it.type == TransactionType.TRANSFER.name }.sumOf { it.amount }

                val count = transactions.size
                val countText = "${PersianFormatter.toPersianDigits(count)} تراکنش"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            totalExpense > totalIncome -> ExpenseRed.copy(alpha = 0.08f)
                            totalIncome > 0 -> IncomeGreen.copy(alpha = 0.08f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Total summary text
                        when {
                            totalExpense > 0 && totalIncome > 0 -> {
                                // Mixed: show both
                                Text(
                                    text = "مجموع هزینه‌ها: ${PersianFormatter.formatMoney(totalExpense, currencyUnit, isHideBalance)}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ExpenseRed
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "مجموع درآمدها: ${PersianFormatter.formatMoney(totalIncome, currencyUnit, isHideBalance)}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = IncomeGreen
                                )
                            }
                            totalExpense > 0 -> {
                                Text(
                                    text = "مجموع هزینه‌ها: ${PersianFormatter.formatMoney(totalExpense, currencyUnit, isHideBalance)}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ExpenseRed
                                )
                            }
                            totalIncome > 0 -> {
                                Text(
                                    text = "مجموع درآمدها: ${PersianFormatter.formatMoney(totalIncome, currencyUnit, isHideBalance)}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = IncomeGreen
                                )
                            }
                            else -> {
                                Text(
                                    text = "مجموع انتقال‌ها: ${PersianFormatter.formatMoney(totalTransfer, currencyUnit, isHideBalance)}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TransferBlue
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = countText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Grouped Transactions List
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        title = "تراکنشی یافت نشد",
                        description = "با توجه به فیلترهای انتخابی، تراکنشی پیدا نشد.",
                        icon = "🔍"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedTransactions.forEach { (dateHeader, txsInGroup) ->
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        items(txsInGroup, key = { it.id }) { tx ->
                            TransactionItemRow(
                                transaction = tx,
                                isHideBalance = isHideBalance,
                                currencyUnit = currencyUnit,
                                accountName = accountNames[tx.accountId],
                                onClick = {
                                    transactionToEditOrDelete = tx
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Transaction Details & Delete Dialog
    transactionToEditOrDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToEditOrDelete = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(tx.categoryIcon, fontSize = 24.sp)
                    Text(
                        text = tx.categoryName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "مبلغ: ${PersianFormatter.formatMoney(tx.amount, currencyUnit)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = when (tx.type) {
                                TransactionType.EXPENSE.name -> ExpenseRed
                                TransactionType.INCOME.name -> IncomeGreen
                                else -> TransferBlue
                            }
                        )
                    )
                    Text(
                        text = "تاریخ: ${JalaliCalendarHelper.formatJalaliDate(tx.dateTimestamp)} ساعت ${JalaliCalendarHelper.formatTime(tx.dateTimestamp)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "حساب: ${accountNames[tx.accountId] ?: "بدون حساب"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (tx.note.isNotBlank()) {
                        Text(
                            text = "توضیحات: ${tx.note}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { transactionToEditOrDelete = null }
                ) {
                    Text("بستن")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        transactionToEditOrDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("delete_tx_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف تراکنش")
                }
            }
        )
    }

    // Custom Search Dialog with Persian Keyboard
    if (showSearchDialog) {
        CustomTextInputDialog(
            title = "جستجو",
            initialValue = searchQuery,
            placeholder = "نام، دسته، توضیحات...",
            onConfirm = { newText ->
                viewModel.searchQuery.value = newText
                showSearchDialog = false
            },
            onDismiss = { showSearchDialog = false },
            maxLength = 100
        )
    }
}
