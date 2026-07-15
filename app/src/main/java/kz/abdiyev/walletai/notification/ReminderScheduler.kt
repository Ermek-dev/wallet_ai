package kz.abdiyev.walletai.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kz.abdiyev.walletai.util.ReminderDelayCalculator
import kz.abdiyev.walletai.util.TimeUtils
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    fun scheduleDailyReminder(hour: Int, minute: Int = 0) {
        val now = System.currentTimeMillis()
        val delay = ReminderDelayCalculator.initialDelayMillis(
            nowMillis = now,
            hour = hour,
            minute = minute,
            timeZoneOffsetMs = TimeUtils.localOffsetMillis(now)
        )

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(ReminderWorker.WORK_NAME)
    }
}
