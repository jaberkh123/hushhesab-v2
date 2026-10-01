package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

enum class AccountType(val titleFa: String) {
    BANK("حساب بانکی"),
    CARD("کارت بانکی"),
    CASH("وجه نقد"),
    SAVINGS("پس‌انداز"),
    WALLET("کیف پول دیجیتال"),
    OTHER("سایر")
}

enum class DebtType(val titleFa: String) {
    OWE("من بدهکارم"),
    LENT("از دیگران طلب دارم")
}

enum class DebtStatus(val titleFa: String) {
    PENDING("پرداخت نشده"),
    PAID("تسویه شده")
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String = AccountType.BANK.name,
    val balance: Long = 0L,
    val currency: String = "تومان",
    val icon: String = "🏦",
    val colorHex: String = "#10B981",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val destinationAccountId: Long? = null,
    val type: String = TransactionType.EXPENSE.name,
    val amount: Long,
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val currency: String = "تومان",
    val dateTimestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val tags: String = "",
    val isRecurring: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: String, // "ALL" for total budget or specific categoryId like "food"
    val categoryName: String,
    val categoryIcon: String,
    val monthlyLimit: Long,
    val monthYear: String // e.g. "1403-05" or "2026-08"
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Long,
    val currentAmount: Long = 0L,
    val targetDate: Long? = null,
    val icon: String = "🎯",
    val colorHex: String = "#3B82F6",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,
    val type: String = DebtType.OWE.name, // OWE or LENT
    val amount: Long,
    val dueDateTimestamp: Long? = null,
    val note: String = "",
    val status: String = DebtStatus.PENDING.name,
    val createdAt: Long = System.currentTimeMillis()
)

data class CategoryItem(
    val id: String,
    val name: String,
    val icon: String,
    val type: TransactionType,
    val colorHex: String = "#10B981"
)

object DefaultCategories {
    val expenseCategories = listOf(
        CategoryItem("food", "غذا و رستوران", "🍔", TransactionType.EXPENSE, "#F59E0B"),
        CategoryItem("shopping", "خرید و بازار", "🛒", TransactionType.EXPENSE, "#EC4899"),
        CategoryItem("transport", "حمل‌ونقل", "🚕", TransactionType.EXPENSE, "#3B82F6"),
        CategoryItem("home", "مسکن و اجاره", "🏠", TransactionType.EXPENSE, "#8B5CF6"),
        CategoryItem("bills", "قبض و خدمات", "💡", TransactionType.EXPENSE, "#EAB308"),
        CategoryItem("entertainment", "تفریح و سرگرمی", "🎮", TransactionType.EXPENSE, "#6366F1"),
        CategoryItem("health", "سلامت و درمان", "💊", TransactionType.EXPENSE, "#EF4444"),
        CategoryItem("education", "آموزش و کتاب", "📚", TransactionType.EXPENSE, "#10B981"),
        CategoryItem("clothes", "پوشاک و مد", "👕", TransactionType.EXPENSE, "#14B8A6"),
        CategoryItem("tech", "تکنولوژی و اینترنت", "💻", TransactionType.EXPENSE, "#06B6D4"),
        CategoryItem("car", "خودرو و بنزین", "🚗", TransactionType.EXPENSE, "#F97316"),
        CategoryItem("travel", "سفر و گردش", "✈️", TransactionType.EXPENSE, "#0EA5E9"),
        CategoryItem("installment", "قسط و بدهی", "💳", TransactionType.EXPENSE, "#D946EF"),
        CategoryItem("gift", "هدیه و بخشش", "🎁", TransactionType.EXPENSE, "#A855F7"),
        CategoryItem("pets", "حیوانات خانگی", "🐾", TransactionType.EXPENSE, "#84CC16"),
        CategoryItem("other_expense", "سایر هزینه‌ها", "📦", TransactionType.EXPENSE, "#64748B")
    )

    val incomeCategories = listOf(
        CategoryItem("salary", "حقوق و دستمزد", "💼", TransactionType.INCOME, "#10B981"),
        CategoryItem("project", "پروژه و قرارداد", "💻", TransactionType.INCOME, "#3B82F6"),
        CategoryItem("freelance", "فریلنسری", "🧑‍💻", TransactionType.INCOME, "#8B5CF6"),
        CategoryItem("sales", "فروش کالا/خدمات", "🛍️", TransactionType.INCOME, "#F59E0B"),
        CategoryItem("investment", "سود و سرمایه‌گذاری", "📈", TransactionType.INCOME, "#06B6D4"),
        CategoryItem("gift_in", "هدیه و پاداش", "🎁", TransactionType.INCOME, "#EC4899"),
        CategoryItem("rent_in", "اجاره دریافتی", "🏠", TransactionType.INCOME, "#14B8A6"),
        CategoryItem("other_income", "سایر درآمدها", "💰", TransactionType.INCOME, "#64748B")
    )

    fun findCategory(id: String): CategoryItem? {
        return (expenseCategories + incomeCategories).find { it.id == id }
    }
}
