package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.utils.PersianFormatter
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.ui.components.AmountNumpadBottomSheet
import com.example.ui.components.ClickableTextInput
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

// ----------------------------------------------------
// 1. Accounts Management Screen
//    افزودن حساب + ویرایش مستقیم موجودی + حذف حساب
//    (تراکنش‌های حساب حذف‌شده به «بدون حساب» منتقل می‌شوند)
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val isHideBalance by viewModel.isHideBalance.collectAsStateWithLifecycle()
    val currencyUnit by viewModel.currencyUnit.collectAsStateWithLifecycle()

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var deletingAccount by remember { mutableStateOf<AccountEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مدیریت حساب‌ها و کارت‌ها", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddAccountDialog = true },
                containerColor = TransferBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "افزودن حساب جدید")
            }
        }
    ) { paddingValues ->
        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    title = "حسابی ثبت نشده است",
                    description = "برای شروع، اولین حساب یا کارت خود را اضافه کنید.",
                    icon = "💳",
                    buttonText = "افزودن حساب",
                    onButtonClick = { showAddAccountDialog = true }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(accounts, key = { it.id }) { acc ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(TransferBlue.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(acc.icon, fontSize = 24.sp)
                                }

                                Column {
                                    Text(
                                        text = acc.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = AccountType.valueOf(acc.type).titleFa,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = PersianFormatter.formatMoney(acc.balance, currencyUnit, isHideBalance),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (acc.balance >= 0) EmeraldPrimary else ExpenseRed
                                )
                            )

                            // ویرایش موجودی (مستقیم)
                            IconButton(
                                onClick = { editingAccount = acc },
                                modifier = Modifier.testTag("edit_account_${acc.id}")
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "ویرایش موجودی",
                                    tint = TransferBlue
                                )
                            }

                            // حذف حساب
                            IconButton(
                                onClick = { deletingAccount = acc },
                                modifier = Modifier.testTag("delete_account_${acc.id}")
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "حذف حساب",
                                    tint = ExpenseRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Account Dialog
    if (showAddAccountDialog) {
        var accName by remember { mutableStateOf("") }
        var accType by remember { mutableStateOf(AccountType.BANK) }
        var accIcon by remember { mutableStateOf("🏦") }

        AmountNumpadBottomSheet(
            title = "افزودن حساب جدید",
            amountLabel = "موجودی فعلی (تومان)",
            onConfirm = { bal ->
                if (accName.isNotBlank()) {
                    viewModel.addAccount(
                        AccountEntity(
                            name = accName.trim(),
                            type = accType.name,
                            balance = bal,
                            icon = accIcon
                        )
                    )
                    showAddAccountDialog = false
                }
            },
            onDismiss = { showAddAccountDialog = false },
            extraContent = {
                ClickableTextInput(
                    value = accName,
                    onValueChange = { accName = it },
                    label = "نام حساب یا کارت (مثلاً: بانک پاسارگاد)",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("نوع حساب:", style = MaterialTheme.typography.labelMedium)
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
            }
        )
    }

    // Edit Account Dialog — ویرایش مستقیم موجودی و نام
    editingAccount?.let { acc ->
        var editName by remember(acc.id) { mutableStateOf(acc.name) }

        AmountNumpadBottomSheet(
            title = "ویرایش حساب",
            amountLabel = "موجودی جدید (تومان)",
            initialAmount = acc.balance,
            onConfirm = { newBalance ->
                viewModel.updateAccount(
                    acc.copy(
                        name = if (editName.isNotBlank()) editName.trim() else acc.name,
                        balance = newBalance
                    )
                )
                editingAccount = null
            },
            onDismiss = { editingAccount = null },
            extraContent = {
                ClickableTextInput(
                    value = editName,
                    onValueChange = { editName = it },
                    label = "نام حساب",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "با تغییر موجودی، مقدار جدید جایگزین موجودی فعلی می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }

    // Delete Account Confirmation
    deletingAccount?.let { acc ->
        AlertDialog(
            onDismissRequest = { deletingAccount = null },
            title = {
                Text(
                    text = "حذف حساب",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "آیا از حذف «${acc.name}» مطمئن هستید؟\n\nتراکنش‌های این حساب حذف نمی‌شوند اما به «بدون حساب» منتقل می‌شوند و دیگر در موجودی کل شما محاسبه نمی‌شوند."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccountAndUnlink(acc)
                        deletingAccount = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("بله، حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingAccount = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}

// ----------------------------------------------------
// 2. Settings & Security Screen
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isHideBalance by viewModel.isHideBalance.collectAsStateWithLifecycle()
    val isPinEnabled by remember { derivedStateOf { viewModel.securityManager.isPinEnabled } }
    val currencyUnit by viewModel.currencyUnit.collectAsStateWithLifecycle()

    var showPinDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات و امنیت", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Security Section
            item {
                Text(
                    text = "امنیت و حریم خصوصی",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("قفل با رمز ۴ رقمی (PIN)") },
                            supportingContent = { Text("هنگام باز کردن برنامه رمز عبور درخواست شود") },
                            leadingContent = { Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary) },
                            trailingContent = {
                                Switch(
                                    checked = isPinEnabled,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            showPinDialog = true
                                        } else {
                                            viewModel.setPinCode("")
                                        }
                                    }
                                )
                            }
                        )

                        Divider(modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text("مخفی کردن مبالغ در داشبورد") },
                            supportingContent = { Text("نمایش به صورت •••••••• برای جلوگیری از دیده شدن") },
                            leadingContent = { Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = IndigoSecondary) },
                            trailingContent = {
                                Switch(
                                    checked = isHideBalance,
                                    onCheckedChange = { viewModel.toggleHideBalance() }
                                )
                            }
                        )
                    }
                }
            }

            // Preferences Section
            item {
                Text(
                    text = "واحد پول و اطلاعات",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("واحد پول پیش‌فرض") },
                            supportingContent = { Text("واحد جاری: $currencyUnit") },
                            leadingContent = { Icon(Icons.Default.Paid, contentDescription = null, tint = AmberAccent) },
                            trailingContent = {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("تومان", "ریال").forEach { unit ->
                                        FilterChip(
                                            selected = currencyUnit == unit,
                                            onClick = { viewModel.setCurrency(unit) },
                                            label = { Text(unit) }
                                        )
                                    }
                                }
                            }
                        )

                        Divider(modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text("خروجی گزارش مالی (CSV/Text)") },
                            supportingContent = { Text("اشتراک‌گذاری و پشتیبان‌گیری از تمام تراکنش‌ها") },
                            leadingContent = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = EmeraldPrimary) },
                            modifier = Modifier.clickable { viewModel.exportTransactionsCsv(context) }
                        )
                    }
                }
            }

            // About Section
            item {
                Text(
                    text = "درباره هوش حساب",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "هوش حساب — HooshHesab",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "نسخه ۱.۱.۰ • توسعه‌یافته با Jetpack Compose",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "مدیریت هوشمند و آفلاین درآمد و هزینه شخصی با رعایت حریم خصوصی و امنیت کامل داده‌ها.",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        var newPin by remember { mutableStateOf("") }
        Dialog(
            onDismissRequest = { showPinDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "تنظیم رمز ۴ رقمی جدید",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "یک رمز ۴ رقمی برای قفل برنامه وارد کنید:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // PIN Display
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(4) { index ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (index < newPin.length) EmeraldPrimary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (index < newPin.length) {
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Number Pad
                    val pinRows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("DEL", "0", "OK")
                    )
                    pinRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { key ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            when (key) {
                                                "DEL" -> {
                                                    if (newPin.isNotEmpty()) newPin = newPin.dropLast(1)
                                                }
                                                "OK" -> {
                                                    if (newPin.length == 4) {
                                                        viewModel.setPinCode(newPin)
                                                        showPinDialog = false
                                                    }
                                                }
                                                else -> {
                                                    if (newPin.length < 4) newPin += key
                                                }
                                            }
                                        },
                                    color = when (key) {
                                        "OK" -> EmeraldPrimary
                                        "DEL" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        when (key) {
                                            "DEL" -> Icon(
                                                Icons.Default.Backspace,
                                                contentDescription = "پاک کردن",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            "OK" -> Text(
                                                text = "تأیید",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                            else -> Text(
                                                text = PersianFormatter.toPersianDigits(key),
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { showPinDialog = false }) {
                        Text("انصراف")
                    }
                }
            }
        }
    }
}
