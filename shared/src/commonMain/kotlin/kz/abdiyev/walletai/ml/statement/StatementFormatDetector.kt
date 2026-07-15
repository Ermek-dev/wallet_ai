package kz.abdiyev.walletai.ml.statement

class StatementFormatDetector {
    fun detect(text: String): StatementFormat {
        val normalized = text.lowercase()
        if (normalized.contains("kaspi bank") || normalized.contains("kaspi.kz")) {
            return StatementFormat.KASPI
        }
        if (normalized.contains("bankffin.kz") ||
            text.contains("Дата Сумма Валюта Операция Детали")
        ) {
            return StatementFormat.FREEDOM
        }
        return StatementFormat.GENERIC
    }
}
