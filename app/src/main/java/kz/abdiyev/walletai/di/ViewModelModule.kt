package kz.abdiyev.walletai.di

import kz.abdiyev.walletai.notification.ReminderScheduler
import kz.abdiyev.walletai.ui.viewmodel.AdvisorViewModel
import kz.abdiyev.walletai.ui.viewmodel.AnalyticsViewModel
import kz.abdiyev.walletai.ui.viewmodel.DashboardViewModel
import kz.abdiyev.walletai.ui.viewmodel.HistoryViewModel
import kz.abdiyev.walletai.ui.viewmodel.ImportViewModel
import kz.abdiyev.walletai.ui.viewmodel.ProfileViewModel
import kz.abdiyev.walletai.util.Clock
import kz.abdiyev.walletai.util.SystemClock
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    single<Clock> { SystemClock }
    single { ReminderScheduler(androidContext()) }
    viewModelOf(::DashboardViewModel)
    viewModelOf(::ImportViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::AnalyticsViewModel)
    viewModelOf(::AdvisorViewModel)
    viewModelOf(::ProfileViewModel)
}

val appModules = listOf(databaseModule, repositoryModule, aiModule, sensorModule, viewModelModule)

/** @deprecated Use [appModules] instead. */
val appModule = module {
    includes(databaseModule, repositoryModule, aiModule, sensorModule, viewModelModule)
}
