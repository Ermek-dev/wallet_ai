package kz.abdiyev.walletai.domain.category

import kz.abdiyev.walletai.domain.model.CategoryData

enum class CategoryHierarchyError {
    PARENT_NOT_FOUND,
    PARENT_HAS_PARENT,
    TYPE_MISMATCH,
    SYSTEM_CATEGORY_CANNOT_HAVE_CHILDREN,
    SUBCATEGORY_CANNOT_HAVE_CHILDREN,
    SELF_PARENT
}

object CategoryHierarchyValidator {

    fun validate(category: CategoryData, allCategories: List<CategoryData>): CategoryHierarchyError? {
        val hasChildren = allCategories.any { it.parentId == category.id && it.id != category.id }
        if (hasChildren && category.parentId != null) {
            return CategoryHierarchyError.SUBCATEGORY_CANNOT_HAVE_CHILDREN
        }

        val parentId = category.parentId ?: return null
        if (parentId == category.id) return CategoryHierarchyError.SELF_PARENT

        val parent = allCategories.find { it.id == parentId }
            ?: return CategoryHierarchyError.PARENT_NOT_FOUND

        if (parent.parentId != null) return CategoryHierarchyError.PARENT_HAS_PARENT
        if (parent.type != category.type) return CategoryHierarchyError.TYPE_MISMATCH
        if (parent.isSystem) return CategoryHierarchyError.SYSTEM_CATEGORY_CANNOT_HAVE_CHILDREN

        return null
    }

    fun canHaveSubcategories(category: CategoryData): Boolean =
        !category.isSystem && category.parentId == null
}
