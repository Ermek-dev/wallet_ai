package kz.abdiyev.walletai.ml.statement

import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.ml.statement.StatementFormat.FREEDOM
import kz.abdiyev.walletai.ml.statement.StatementFormat.GENERIC
import kz.abdiyev.walletai.ml.statement.StatementFormat.KASPI

object TransferDetector {
    fun isTransfer(
        format: StatementFormat,
        rawOperation: String,
        description: String,
        type: TransactionType
    ): Boolean {
        val op = rawOperation.lowercase()
        val desc = description.lowercase()
        return when (format) {
            KASPI -> isKaspiTransfer(op, desc)
            FREEDOM -> desc.contains("перевод с карты на карту") ||
                op.contains("перевод с карты на карту")
            GENERIC -> false
        }
    }

    private fun isKaspiTransfer(operation: String, description: String): Boolean {
        if (description.contains("зарплата")) return false
        if (operation.contains("разное") && description.contains("комиссия")) return false
        if (operation.contains("перевод на свой")) return true
        if (operation.contains("пополнение") && description.contains("с карты другого банка")) return true
        if (operation.contains("пополнение") &&
            (description.contains("kaspi банкомат") || description.contains("kaspi терминал"))
        ) {
            return true
        }
        return false
    }
}
