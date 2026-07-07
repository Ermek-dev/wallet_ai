package kz.abdiyev.walletai.domain.model

enum class TransactionType(val dbValue: String) {
    INCOME("INCOME"),
    EXPENSE("EXPENSE");

    companion object {
        fun fromDb(value: String): TransactionType =
            entries.find { it.dbValue == value } ?: EXPENSE
    }
}
