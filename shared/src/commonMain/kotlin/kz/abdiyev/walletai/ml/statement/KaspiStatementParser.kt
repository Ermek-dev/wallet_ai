package kz.abdiyev.walletai.ml.statement

import kz.abdiyev.walletai.util.StatementDate
class KaspiStatementParser : StatementParser {
    private val operationLineRegex = Regex(
        """^(\d{2}\.\d{2}\.\d{2})\s+([+-])\s+([\d\s\u00A0\u202F]+,\d{2})\s+₸\s+(.+)$"""
    )

    override fun parse(text: String, ctx: StatementParseContext): List<RawStatementLine> {
        val result = mutableListOf<RawStatementLine>()
        var pendingContinuation: String? = null

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isBlank() || isJunkLine(line)) continue

            val match = operationLineRegex.matchEntire(line)
            if (match != null) {
                pendingContinuation?.let { cont ->
                    appendContinuation(result, cont)
                }
                pendingContinuation = null

                val (day, month, year) = parseKaspiDateParts(match.groupValues[1])
                val date = StatementDate.kaspiDate(day, month, year)
                val (type, money) = StatementAmount.parseKaspi(
                    sign = match.groupValues[2].first(),
                    amountPart = match.groupValues[3],
                    currency = ctx.defaultCurrency
                )
                val (rawOperation, description) = splitOperationAndDescription(match.groupValues[4])
                val cleanDescription = StatementDescriptionSanitizer.clean(description)
                result += RawStatementLine(
                    money = money,
                    type = type,
                    description = cleanDescription,
                    rawOperation = rawOperation,
                    date = date,
                    isTransfer = TransferDetector.isTransfer(
                        StatementFormat.KASPI,
                        rawOperation,
                        cleanDescription,
                        type
                    )
                )
            } else if (result.isNotEmpty() && isContinuationLine(line)) {
                pendingContinuation = line
            }
        }

        pendingContinuation?.let { appendContinuation(result, it) }
        return result
    }

    private fun appendContinuation(result: MutableList<RawStatementLine>, continuation: String) {
        val lastIndex = result.lastIndex
        if (lastIndex < 0) return
        val last = result[lastIndex]
        val mergedDescription = StatementDescriptionSanitizer.clean("${last.description} $continuation")
        val mergedOperation = if (continuation.equals("счет", ignoreCase = true)) {
            "${last.rawOperation} счет"
        } else {
            last.rawOperation
        }
        result[lastIndex] = last.copy(
            description = mergedDescription,
            rawOperation = mergedOperation,
            isTransfer = TransferDetector.isTransfer(
                StatementFormat.KASPI,
                mergedOperation,
                mergedDescription,
                last.type
            )
        )
    }

    private fun splitOperationAndDescription(rest: String): Pair<String, String> {
        val knownOps = listOf(
            "Перевод на свой счет",
            "Перевод на свой",
            "Пополнение",
            "Перевод",
            "Покупка",
            "Снятие",
            "Разное"
        )
        for (op in knownOps) {
            if (rest.startsWith(op, ignoreCase = false)) {
                val description = rest.removePrefix(op).trim()
                return op to description
            }
        }
        val firstSpace = rest.indexOf(' ')
        return if (firstSpace > 0) {
            rest.substring(0, firstSpace) to rest.substring(firstSpace + 1).trim()
        } else {
            rest to ""
        }
    }

    private fun parseKaspiDateParts(value: String): Triple<Int, Int, Int> {
        val parts = value.split('.')
        return Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    }

    private fun isJunkLine(line: String): Boolean =
        line.startsWith("АО «Kaspi Bank»") ||
            line.startsWith("Приложение к Справке") ||
            line.startsWith("-- ") ||
            line.contains("www.kaspi.kz")

    private fun isContinuationLine(line: String): Boolean =
        line.equals("счет", ignoreCase = true) ||
            line.startsWith("Оплата Kaspi Red", ignoreCase = true)
}
