package kz.abdiyev.walletai.util

import kz.abdiyev.walletai.domain.model.TransactionData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun groupTransactionsByDay(transactions: List<TransactionData>): List<Pair<String, List<TransactionData>>> {
    if (transactions.isEmpty()) return emptyList()
    val dayFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    fun dayLabel(dateMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        return when {
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Сегодня"
            cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Вчера"
            else -> dayFormat.format(Date(dateMillis))
        }
    }

    return transactions
        .groupBy { dayLabel(it.date) }
        .toList()
        .sortedByDescending { (_, txs) -> txs.maxOf { it.date } }
}
