package kz.abdiyev.walletai.util

import java.util.TimeZone

object TimeUtils {
    fun localOffsetMillis(nowMillis: Long = System.currentTimeMillis()): Long =
        TimeZone.getDefault().getOffset(nowMillis).toLong()
}
