package kz.abdiyev.walletai.ml.statement

import kz.abdiyev.walletai.util.StatementDate
class FreedomStatementParser : StatementParser {
    private val dateAnchorRegex = Regex("""^\d{2}\.\d{2}\.\d{4}""")
    private val blockRegex = Regex(
        """^(\d{2}\.\d{2}\.\d{4})\s+([+-]?[\d,]+\.\d{2})\s+[₸$]\s+(\w{3})\s+(.+)$""",
        RegexOption.DOT_MATCHES_ALL
    )

    override fun parse(text: String, ctx: StatementParseContext): List<RawStatementLine> {
        val blocks = buildBlocks(text)
        val result = mutableListOf<RawStatementLine>()

        for (block in blocks) {
            val normalized = block
                .replace(Regex("Сумма в\\s+обработке", RegexOption.IGNORE_CASE), "Сумма в обработке")
                .replace(Regex("\\s+"), " ")
                .trim()

            val match = blockRegex.matchEntire(normalized) ?: continue
            val currencyCode = match.groupValues[3].uppercase()
            if (currencyCode != "KZT") continue

            val (day, month, year) = parseFreedomDateParts(match.groupValues[1])
            val date = StatementDate.freedomDate(day, month, year)
            val (type, money) = StatementAmount.parseFreedom(match.groupValues[2], ctx.defaultCurrency)
            val (rawOperation, description) = splitFreedomOperation(match.groupValues[4])
            val cleanDescription = StatementDescriptionSanitizer.clean(description)

            result += RawStatementLine(
                money = money,
                type = type,
                description = cleanDescription,
                rawOperation = rawOperation,
                date = date,
                isTransfer = TransferDetector.isTransfer(
                    StatementFormat.FREEDOM,
                    rawOperation,
                    cleanDescription,
                    type
                )
            )
        }
        return result
    }

    private fun buildBlocks(text: String): List<String> {
        val blocks = mutableListOf<StringBuilder>()
        var current: StringBuilder? = null

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isBlank() || isJunkLine(line)) continue

            if (dateAnchorRegex.containsMatchIn(line)) {
                current = StringBuilder(line)
                blocks += current
            } else if (current != null) {
                current.append(' ')
                current.append(line)
            }
        }
        return blocks.map { it.toString() }
    }

    private fun splitFreedomOperation(rest: String): Pair<String, String> {
        val knownOps = listOf(
            "Сумма в обработке",
            "Пополнение",
            "Перевод",
            "Платеж",
            "Покупка"
        )
        for (op in knownOps) {
            if (rest.startsWith(op, ignoreCase = true)) {
                return op to rest.removePrefix(op).trim()
            }
        }
        val firstSpace = rest.indexOf(' ')
        return if (firstSpace > 0) {
            rest.substring(0, firstSpace) to rest.substring(firstSpace + 1).trim()
        } else {
            rest to ""
        }
    }

    private fun parseFreedomDateParts(value: String): Triple<Int, Int, Int> {
        val parts = value.split('.')
        return Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    }

    private fun isJunkLine(line: String): Boolean =
        line.startsWith("Подлинность справки") ||
            line.startsWith("просканировав QR-код") ||
            line.startsWith("https://bankffin.kz") ||
            line.startsWith("По курсу, установленному") ||
            line.startsWith("Дата Сумма Валюта Операция Детали") ||
            line.startsWith("Сумма в обработке. Банк ожидает") ||
            line.startsWith("Возмещение за QR оплату") ||
            line.startsWith("-- ")
}
