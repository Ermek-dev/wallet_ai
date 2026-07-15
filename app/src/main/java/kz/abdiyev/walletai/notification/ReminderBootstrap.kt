package kz.abdiyev.walletai.notification

import android.content.Context
import kz.abdiyev.walletai.domain.model.Settings

object ReminderBootstrap {
    fun restoreSchedule(context: Context, settings: Settings) {
        val scheduler = ReminderScheduler(context)
        if (settings.notificationsEnabled) {
            scheduler.scheduleDailyReminder(settings.notificationHour, settings.notificationMinute)
        } else {
            scheduler.cancel()
        }
    }
}
