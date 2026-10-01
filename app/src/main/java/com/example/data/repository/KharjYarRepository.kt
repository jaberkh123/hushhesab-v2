package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class KharjYarRepository(
    private val db: KharjYarDatabase,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val goalDao: GoalDao,
    private val debtDao: DebtDao
) {
    // Accounts
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val totalBalance: Flow<Long?> = accountDao.getTotalBalanceFlow()

    suspend fun getAccountById(id: Long): AccountEntity? = accountDao.getAccountById(id)

    suspend fun insertAccount(account: AccountEntity): Long = accountDao.insertAccount(account)

    suspend fun updateAccount(account: AccountEntity) = accountDao.updateAccount(account)

    suspend fun deleteAccount(account: AccountEntity) = accountDao.deleteAccount(account)

    /**
     * Deletes an account while KEEPING its transaction history:
     * all related transactions are detached (accountId = -1 → «بدون حساب»)
     * so they remain visible in the list but stop affecting any balance.
     * Atomic: a crash midway can never leave half-detached rows.
     */
    suspend fun deleteAccountAndUnlinkTransactions(account: AccountEntity) = db.withTransaction {
        transactionDao.detachTransactionsFromAccount(account.id)
        transactionDao.detachTransferDestinations(account.id)
        accountDao.deleteAccount(account)
    }

    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    // Balance mutations and the transaction row must commit atomically — a crash
    // between them would leave account balances out of sync with history forever.
    suspend fun addTransaction(transaction: TransactionEntity): Long = db.withTransaction {
        val insertedId = transactionDao.insertTransaction(transaction)

        // Adjust Account Balance according to rules
        when (transaction.type) {
            TransactionType.EXPENSE.name -> {
                accountDao.updateBalance(transaction.accountId, -transaction.amount)
            }
            TransactionType.INCOME.name -> {
                accountDao.updateBalance(transaction.accountId, transaction.amount)
            }
            TransactionType.TRANSFER.name -> {
                accountDao.updateBalance(transaction.accountId, -transaction.amount)
                transaction.destinationAccountId?.let { destId ->
                    accountDao.updateBalance(destId, transaction.amount)
                }
            }
        }

        insertedId
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = db.withTransaction {
        // Reverse previous effect on balance(s)
        when (transaction.type) {
            TransactionType.EXPENSE.name -> {
                accountDao.updateBalance(transaction.accountId, transaction.amount)
            }
            TransactionType.INCOME.name -> {
                accountDao.updateBalance(transaction.accountId, -transaction.amount)
            }
            TransactionType.TRANSFER.name -> {
                accountDao.updateBalance(transaction.accountId, transaction.amount)
                transaction.destinationAccountId?.let { destId ->
                    accountDao.updateBalance(destId, -transaction.amount)
                }
            }
        }

        transactionDao.deleteTransaction(transaction)
    }

    suspend fun updateTransaction(oldTransaction: TransactionEntity, newTransaction: TransactionEntity) = db.withTransaction {
        // 1. Rollback old transaction balance effect
        when (oldTransaction.type) {
            TransactionType.EXPENSE.name -> {
                accountDao.updateBalance(oldTransaction.accountId, oldTransaction.amount)
            }
            TransactionType.INCOME.name -> {
                accountDao.updateBalance(oldTransaction.accountId, -oldTransaction.amount)
            }
            TransactionType.TRANSFER.name -> {
                accountDao.updateBalance(oldTransaction.accountId, oldTransaction.amount)
                oldTransaction.destinationAccountId?.let { destId ->
                    accountDao.updateBalance(destId, -oldTransaction.amount)
                }
            }
        }

        // 2. Apply new transaction balance effect
        when (newTransaction.type) {
            TransactionType.EXPENSE.name -> {
                accountDao.updateBalance(newTransaction.accountId, -newTransaction.amount)
            }
            TransactionType.INCOME.name -> {
                accountDao.updateBalance(newTransaction.accountId, newTransaction.amount)
            }
            TransactionType.TRANSFER.name -> {
                accountDao.updateBalance(newTransaction.accountId, -newTransaction.amount)
                newTransaction.destinationAccountId?.let { destId ->
                    accountDao.updateBalance(destId, newTransaction.amount)
                }
            }
        }

        transactionDao.updateTransaction(newTransaction)
    }

    // Budgets
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    suspend fun insertBudget(budget: BudgetEntity): Long = budgetDao.insertBudget(budget)

    suspend fun updateBudget(budget: BudgetEntity) = budgetDao.updateBudget(budget)

    suspend fun deleteBudget(budget: BudgetEntity) = budgetDao.deleteBudget(budget)

    // Goals
    val allGoals: Flow<List<GoalEntity>> = goalDao.getAllGoals()

    suspend fun insertGoal(goal: GoalEntity): Long = goalDao.insertGoal(goal)

    suspend fun updateGoal(goal: GoalEntity) = goalDao.updateGoal(goal)

    suspend fun deleteGoal(goal: GoalEntity) = goalDao.deleteGoal(goal)

    suspend fun depositToGoal(goal: GoalEntity, amount: Long, fromAccountId: Long? = null) = db.withTransaction {
        val updated = goal.copy(currentAmount = goal.currentAmount + amount)
        goalDao.updateGoal(updated)
        if (fromAccountId != null) {
            accountDao.updateBalance(fromAccountId, -amount)
        }
    }

    // Debts
    val allDebts: Flow<List<DebtEntity>> = debtDao.getAllDebts()

    suspend fun insertDebt(debt: DebtEntity): Long = debtDao.insertDebt(debt)

    suspend fun updateDebt(debt: DebtEntity) = debtDao.updateDebt(debt)

    suspend fun deleteDebt(debt: DebtEntity) = debtDao.deleteDebt(debt)

    suspend fun markDebtAsPaid(debt: DebtEntity) {
        val updated = debt.copy(status = DebtStatus.PAID.name)
        debtDao.updateDebt(updated)
    }
}
