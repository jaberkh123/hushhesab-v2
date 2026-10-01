package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.security.SecurityManager
import com.example.core.utils.JalaliCalendarHelper
import com.example.core.utils.PersianFormatter
import com.example.data.local.KharjYarDatabase
import com.example.data.model.*
import com.example.data.repository.KharjYarRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = KharjYarDatabase.getDatabase(application)
    val repository = KharjYarRepository(
        db = database,
        accountDao = database.accountDao(),
        transactionDao = database.transactionDao(),
        budgetDao = database.budgetDao(),
        goalDao = database.goalDao(),
        debtDao = database.debtDao()
    )

    val securityManager = SecurityManager(application)

    // Security & App State
    val isAppLocked = MutableStateFlow(securityManager.isPinEnabled)
    val isHideBalance = MutableStateFlow(securityManager.isHideBalance)
    val isOnboardingDone = MutableStateFlow(securityManager.isOnboardingDone)
    val currencyUnit = MutableStateFlow(securityManager.currencyUnit)
    val darkModePref = MutableStateFlow(securityManager.darkModePref)

    // Data streams
    val accounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * True only when the accounts query has actually emitted and the user has
     * no accounts — the real "first run" signal for the setup gate. Derived
     * from the raw Room flow (which has no initial value), so an existing
     * user's app start never flashes the setup screen while the placeholder
     * emptyList of [accounts] is still in flight.
     */
    val needsFirstAccount: StateFlow<Boolean> = repository.allAccounts
        .map { it.isEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val totalBalance: StateFlow<Long> = repository.totalBalance
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Emits at every local midnight so day-scoped windows (today / this week /
    // this Jalali month) roll over correctly even if the app stays open.
    private val dayTicker: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(JalaliCalendarHelper.millisUntilTomorrow())
        }
    }

    // Transactions Screen Filters
    val searchQuery = MutableStateFlow("")
    val filterType = MutableStateFlow("ALL") // ALL, EXPENSE, INCOME, TRANSFER
    val filterCategory = MutableStateFlow<String?>(null)
    val filterAccount = MutableStateFlow<Long?>(null)
    val filterDateRange = MutableStateFlow("THIS_MONTH") // ALL, TODAY, THIS_WEEK, THIS_MONTH

    data class FilterState(
        val query: String = "",
        val type: String = "ALL",
        val category: String? = null,
        val accountId: Long? = null,
        val dateRange: String = "THIS_MONTH"
    )

    private val filterStateFlow = combine(
        searchQuery,
        filterType,
        filterCategory,
        filterAccount,
        filterDateRange
    ) { query, type, cat, acc, dateRange ->
        FilterState(query, type, cat, acc, dateRange)
    }

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        filterStateFlow,
        dayTicker
    ) { txs, filters, _ ->
        // Calendar-day boundaries, not rolling 24h/7d windows: a transaction from
        // yesterday 23:00 must not appear under «امروز» until 23:00 today.
        val todayStart = JalaliCalendarHelper.getStartOfToday()
        val weekStart = JalaliCalendarHelper.getStartOfCurrentWeek()
        val monthStart = JalaliCalendarHelper.getStartOfCurrentJalaliMonth()

        txs.filter { tx ->
            // Search query match
            val matchesQuery = filters.query.isBlank() ||
                    tx.categoryName.contains(filters.query, ignoreCase = true) ||
                    tx.note.contains(filters.query, ignoreCase = true) ||
                    tx.tags.contains(filters.query, ignoreCase = true) ||
                    PersianFormatter.formatMoney(tx.amount).contains(filters.query)

            // Type match
            val matchesType = filters.type == "ALL" || tx.type == filters.type

            // Category match
            val matchesCat = filters.category == null || tx.categoryId == filters.category

            // Account match
            val matchesAcc = filters.accountId == null || tx.accountId == filters.accountId || tx.destinationAccountId == filters.accountId

            // Date match
            val matchesDate = when (filters.dateRange) {
                "TODAY" -> tx.dateTimestamp >= todayStart
                "THIS_WEEK" -> tx.dateTimestamp >= weekStart
                "THIS_MONTH" -> tx.dateTimestamp >= monthStart
                else -> true
            }

            matchesQuery && matchesType && matchesCat && matchesAcc && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun unlockApp(pin: String): Boolean {
        val verified = securityManager.verifyPin(pin)
        if (verified) {
            isAppLocked.value = false
        }
        return verified
    }

    fun toggleHideBalance() {
        val newVal = !isHideBalance.value
        isHideBalance.value = newVal
        securityManager.isHideBalance = newVal
    }

    fun setPinCode(pin: String) {
        securityManager.pinCode = pin
        securityManager.isPinEnabled = pin.isNotEmpty()
        isAppLocked.value = false
    }

    fun togglePinEnabled(enabled: Boolean) {
        securityManager.isPinEnabled = enabled
    }

    fun completeOnboarding() {
        securityManager.isOnboardingDone = true
        isOnboardingDone.value = true
    }

    fun setCurrency(unit: String) {
        currencyUnit.value = unit
        securityManager.currencyUnit = unit
    }

    fun setDarkMode(mode: String) {
        darkModePref.value = mode
        securityManager.darkModePref = mode
    }

    fun addTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.addTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun updateTransaction(oldTx: TransactionEntity, newTx: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(oldTx, newTx)
        }
    }

    fun addAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.insertAccount(account)
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    /**
     * Deletes the account but keeps its transactions: they become «بدون حساب»
     * (accountId = -1) and stop affecting any account balance.
     */
    fun deleteAccountAndUnlink(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccountAndUnlinkTransactions(account)
        }
    }

    // ─── CSV Export / Backup ─────────────────────────────────────────
    fun exportTransactionsCsv(context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            // Full backup: ALL transactions, independent of any screen filter.
            val list = allTransactions.value
            val sb = StringBuilder()
            sb.append("\uFEFF") // UTF-8 BOM so Excel renders Persian text correctly
            sb.append("شناسه,نوع,مبلغ,دسته,تاریخ,توضیحات,واحد\n")
            list.forEach { tx ->
                val typeFa = when (tx.type) {
                    TransactionType.EXPENSE.name -> "هزینه"
                    TransactionType.INCOME.name -> "درآمد"
                    else -> "انتقال"
                }
                val dateStr = JalaliCalendarHelper.formatJalaliDate(tx.dateTimestamp)
                sb.append("${tx.id},$typeFa,${tx.amount},${csvEscape(tx.categoryName)},${csvEscape(dateStr)},${csvEscape(tx.note)},${csvEscape(tx.currency)}\n")
            }

            withContext(Dispatchers.Main) {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, sb.toString())
                    putExtra(Intent.EXTRA_TITLE, "گزارش مالی هوش حساب")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "اشتراک‌گذاری گزارش")
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(shareIntent)
            }
        }
    }

    /** RFC 4180: wrap in quotes and double any embedded quote character. */
    private fun csvEscape(field: String): String {
        val needsQuoting = field.contains('"') || field.contains(',') ||
                field.contains('\n') || field.contains('\r')
        return if (needsQuoting) "\"${field.replace("\"", "\"\"")}\"" else field
    }
}
