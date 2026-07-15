package kz.abdiyev.walletai.ml.statement

import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.ml.ParsedStatementLine

/** Raw operation parsed from statement text before categorization. */
data class RawStatementLine(
    val money: Money,
    val type: TransactionType,
    val description: String,
    val rawOperation: String,
    val date: Long,
    val isTransfer: Boolean
)

data class StatementParseContext(
    val defaultCurrency: Currency = Currency.KZT,
    val defaultDate: Long = 0L
)

interface StatementParser {
    fun parse(text: String, ctx: StatementParseContext): List<RawStatementLine>
}
