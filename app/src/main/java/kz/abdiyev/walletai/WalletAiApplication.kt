package kz.abdiyev.walletai

import android.app.Application
import kz.abdiyev.walletai.di.appModules
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.notification.ReminderBootstrap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class WalletAiApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (BuildConfig.DEBUG) {
                Timber.e(throwable, "Uncaught exception in thread %s", thread.name)
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
        startKoin {
            androidLogger()
            androidContext(this@WalletAiApplication)
            modules(appModules)
        }
        applicationScope.launch {
            val settingsRepository = org.koin.java.KoinJavaComponent.getKoin().get<SettingsRepository>()
            val settings = settingsRepository.settings.first()
            ReminderBootstrap.restoreSchedule(this@WalletAiApplication, settings)
        }
    }
}
