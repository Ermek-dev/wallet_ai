package kz.abdiyev.walletai.domain.usecase

import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.util.DescriptionNormalizer

object ImportIdentity {
    fun key(scope: String, date: Long, money: Money, type: TransactionType, description: String): String =
        listOf(scope, date.toString(), money.amountMinor.toString(), money.currency.code, type.name,
            DescriptionNormalizer.normalize(description)).joinToString("") { "${it.length}:$it" }

    fun merchantKey(scope: String, description: String): String =
        listOf(scope, DescriptionNormalizer.normalize(description)).joinToString("") { "${it.length}:$it" }
}
