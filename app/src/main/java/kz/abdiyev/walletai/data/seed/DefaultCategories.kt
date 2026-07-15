package kz.abdiyev.walletai.data.seed

import kz.abdiyev.walletai.data.database.CategoryEntity

object DefaultCategories {
    val entities = listOf(
        CategoryEntity(name = "Продукты", type = "EXPENSE", colorHex = "#EF5350", iconName = "shopping_cart"),
        CategoryEntity(name = "ЖКХ", type = "EXPENSE", colorHex = "#42A5F5", iconName = "home"),
        CategoryEntity(name = "Кофе и Рестораны", type = "EXPENSE", colorHex = "#FFA726", iconName = "restaurant"),
        CategoryEntity(name = "Транспорт", type = "EXPENSE", colorHex = "#26A69A", iconName = "commute"),
        CategoryEntity(name = "Развлечения", type = "EXPENSE", colorHex = "#AB47BC", iconName = "movie"),
        CategoryEntity(name = "Здоровье", type = "EXPENSE", colorHex = "#66BB6A", iconName = "healing"),
        CategoryEntity(name = "Подписки", type = "EXPENSE", colorHex = "#78909C", iconName = "subscriptions"),
        CategoryEntity(name = "Зарплата", type = "INCOME", colorHex = "#9CCC65", iconName = "payments"),
        CategoryEntity(name = "Переводы", type = "EXPENSE", colorHex = "#B0BEC5", iconName = "swap_horiz", isSystem = true),
        CategoryEntity(name = "Другое", type = "EXPENSE", colorHex = "#757575", iconName = "paid", isSystem = true)
    )
}
