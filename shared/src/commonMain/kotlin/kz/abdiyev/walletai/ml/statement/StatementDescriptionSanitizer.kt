package kz.abdiyev.walletai.ml.statement

object StatementDescriptionSanitizer {
    fun clean(raw: String): String {
        var text = raw
            .replace(Regex("(?i)\\s*with requisites.*"), "")
            .replace(Regex("(?i)\\s*account=\\d+.*"), "")
            .replace(Regex("(?i)\\s*total amount of.*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (text.startsWith("Payment for ", ignoreCase = true)) {
            text = text.removePrefix("Payment for ").trim()
        }
        if (text.contains("ОНАЙ", ignoreCase = true) || text.contains("ONAY", ignoreCase = true)) {
            text = "ОНАЙ"
        }

        return text.ifBlank { "Операция по выписке" }
    }
}
