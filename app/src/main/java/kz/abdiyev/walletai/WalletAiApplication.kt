package kz.abdiyev.walletai

import android.app.Application
import kz.abdiyev.walletai.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class WalletAiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "Uncaught exception in thread %s", thread.name)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        startKoin {
            androidLogger()
            androidContext(this@WalletAiApplication)
            modules(appModule)
        }
    }
}
