package kz.abdiyev.walletai.domain.category

import kz.abdiyev.walletai.domain.model.CategoryData

data class CategoryAmountShare(
    val categoryId: Long,
    val amountMinor: Long
)

data class RolledUpCategoryShare(
    val rootCategoryId: Long,
    val amountMinor: Long,
    val subShares: List<CategoryAmountShare> = emptyList()
)

object CategoryRollup {

    fun rootCategoryId(categoryId: Long, categoriesById: Map<Long, CategoryData>): Long {
        val category = categoriesById[categoryId] ?: return categoryId
        return category.parentId ?: category.id
    }

    fun rollup(
        amountsByCategoryId: Map<Long, Long>,
        categories: List<CategoryData>
    ): List<RolledUpCategoryShare> {
        if (amountsByCategoryId.isEmpty()) return emptyList()

        val categoriesById = categories.associateBy { it.id }
        val rootTotals = mutableMapOf<Long, Long>()

        amountsByCategoryId.forEach { (categoryId, amount) ->
            val rootId = rootCategoryId(categoryId, categoriesById)
            rootTotals[rootId] = (rootTotals[rootId] ?: 0L) + amount
        }

        return rootTotals.entries
            .map { (rootId, total) ->
                RolledUpCategoryShare(
                    rootCategoryId = rootId,
                    amountMinor = total,
                    subShares = breakdown(rootId, amountsByCategoryId, categories)
                )
            }
            .sortedByDescending { it.amountMinor }
    }

    fun breakdown(
        rootCategoryId: Long,
        amountsByCategoryId: Map<Long, Long>,
        categories: List<CategoryData>
    ): List<CategoryAmountShare> {
        val categoriesById = categories.associateBy { it.id }
        return amountsByCategoryId.mapNotNull { (categoryId, amount) ->
            if (rootCategoryId(categoryId, categoriesById) != rootCategoryId) return@mapNotNull null
            CategoryAmountShare(categoryId, amount)
        }.sortedByDescending { it.amountMinor }
    }
}

fun CategoryData.displayName(categoriesById: Map<Long, CategoryData>): String {
    val parent = parentId?.let { categoriesById[it] }
    return if (parent != null) "${parent.name} · $name" else name
}

fun CategoryData.rootCategory(categoriesById: Map<Long, CategoryData>): CategoryData {
    return parentId?.let { categoriesById[it] } ?: this
}
