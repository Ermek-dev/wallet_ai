package kz.abdiyev.walletai

import kz.abdiyev.walletai.domain.category.CategoryHierarchyError
import kz.abdiyev.walletai.domain.category.CategoryHierarchyValidator
import kz.abdiyev.walletai.domain.category.CategoryRollup
import kz.abdiyev.walletai.domain.category.displayName
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryHierarchyValidatorTest {

    private val roots = listOf(
        CategoryData(1, "Продукты", TransactionType.EXPENSE, "#EF5350", "shopping_cart"),
        CategoryData(2, "Транспорт", TransactionType.EXPENSE, "#26A69A", "commute"),
        CategoryData(10, "Переводы", TransactionType.EXPENSE, "#B0BEC5", "swap_horiz", isSystem = true)
    )

    @Test
    fun rootCategory_isValid() {
        val category = CategoryData(1, "Продукты", TransactionType.EXPENSE, "#EF5350", "shopping_cart")
        assertNull(CategoryHierarchyValidator.validate(category, roots))
    }

    @Test
    fun subcategory_withValidParent_isValid() {
        val sub = CategoryData(3, "Magnum", TransactionType.EXPENSE, "#EF5350", "store", parentId = 1)
        val all = roots + sub
        assertNull(CategoryHierarchyValidator.validate(sub, all))
    }

    @Test
    fun subcategory_withParentThatHasParent_rejected() {
        val sub = CategoryData(3, "Magnum", TransactionType.EXPENSE, "#EF5350", "store", parentId = 1)
        val nested = CategoryData(4, "Мясо", TransactionType.EXPENSE, "#EF5350", "store", parentId = 3)
        val all = roots + sub + nested
        assertEquals(
            CategoryHierarchyError.PARENT_HAS_PARENT,
            CategoryHierarchyValidator.validate(nested, all)
        )
    }

    @Test
    fun subcategory_withTypeMismatch_rejected() {
        val sub = CategoryData(3, "Bonus", TransactionType.INCOME, "#9CCC65", "payments", parentId = 1)
        assertEquals(
            CategoryHierarchyError.TYPE_MISMATCH,
            CategoryHierarchyValidator.validate(sub, roots)
        )
    }

    @Test
    fun subcategory_underSystemCategory_rejected() {
        val sub = CategoryData(11, "Internal", TransactionType.EXPENSE, "#B0BEC5", "swap", parentId = 10)
        assertEquals(
            CategoryHierarchyError.SYSTEM_CATEGORY_CANNOT_HAVE_CHILDREN,
            CategoryHierarchyValidator.validate(sub, roots)
        )
    }

    @Test
    fun categoryWithChildren_cannotBecomeSubcategory() {
        val parent = CategoryData(1, "Продукты", TransactionType.EXPENSE, "#EF5350", "shopping_cart")
        val child = CategoryData(3, "Magnum", TransactionType.EXPENSE, "#EF5350", "store", parentId = 1)
        val parentAsSub = parent.copy(parentId = 2)
        val all = listOf(parent, child, roots[1])
        assertEquals(
            CategoryHierarchyError.SUBCATEGORY_CANNOT_HAVE_CHILDREN,
            CategoryHierarchyValidator.validate(parentAsSub, all)
        )
    }

    @Test
    fun canHaveSubcategories_falseForSystemAndSubcategories() {
        val root = CategoryData(1, "Продукты", TransactionType.EXPENSE, "#EF5350", "shopping_cart")
        val sub = CategoryData(3, "Magnum", TransactionType.EXPENSE, "#EF5350", "store", parentId = 1)
        val system = CategoryData(10, "Переводы", TransactionType.EXPENSE, "#B0BEC5", "swap_horiz", isSystem = true)
        assertTrue(CategoryHierarchyValidator.canHaveSubcategories(root))
        assertTrue(!CategoryHierarchyValidator.canHaveSubcategories(sub))
        assertTrue(!CategoryHierarchyValidator.canHaveSubcategories(system))
    }
}

class CategoryRollupTest {

    private val categories = listOf(
        CategoryData(1, "Продукты", TransactionType.EXPENSE, "#EF5350", "shopping_cart"),
        CategoryData(2, "Magnum", TransactionType.EXPENSE, "#EF5350", "store", parentId = 1),
        CategoryData(3, "Мясо", TransactionType.EXPENSE, "#EF5350", "store", parentId = 1),
        CategoryData(4, "Транспорт", TransactionType.EXPENSE, "#26A69A", "commute")
    )

    @Test
    fun rollup_sumsSubcategoriesIntoRoot() {
        val amounts = mapOf(
            1L to 1_000L,
            2L to 2_000L,
            3L to 500L,
            4L to 800L
        )
        val rolled = CategoryRollup.rollup(amounts, categories)
        val products = rolled.first { it.rootCategoryId == 1L }
        assertEquals(3_500L, products.amountMinor)
        assertEquals(3, products.subShares.size)
    }

    @Test
    fun breakdown_listsOnlyCategoriesUnderRoot() {
        val amounts = mapOf(2L to 2_000L, 3L to 500L, 4L to 800L)
        val breakdown = CategoryRollup.breakdown(1L, amounts, categories)
        assertEquals(2, breakdown.size)
        assertTrue(breakdown.all { CategoryRollup.rootCategoryId(it.categoryId, categories.associateBy { c -> c.id }) == 1L })
    }

    @Test
    fun displayName_showsParentAndSubcategory() {
        val byId = categories.associateBy { it.id }
        assertEquals("Продукты · Magnum", categories[1].displayName(byId))
        assertEquals("Продукты", categories[0].displayName(byId))
    }
}
