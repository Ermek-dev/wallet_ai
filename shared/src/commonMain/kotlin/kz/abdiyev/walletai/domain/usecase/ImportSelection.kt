package kz.abdiyev.walletai.domain.usecase

import kz.abdiyev.walletai.domain.model.TransactionPreview

object ImportSelection {
    fun select(
        previews: List<TransactionPreview>, existingKeys: List<String>, skipTransfers: Boolean
    ): List<TransactionPreview> {
        val existingCounts = existingKeys.groupingBy { it }.eachCount()
        val occurrences = mutableMapOf<String, Int>()
        return previews.filter { preview ->
            val key = requireNotNull(preview.sourceKey)
            val ordinal = (occurrences[key] ?: 0) + 1
            occurrences[key] = ordinal
            val alreadySaved = ordinal <= (existingCounts[key] ?: 0)
            ((!preview.isDuplicate && !alreadySaved) || preview.forceImport) && (!skipTransfers || !preview.isTransfer)
        }
    }
}
