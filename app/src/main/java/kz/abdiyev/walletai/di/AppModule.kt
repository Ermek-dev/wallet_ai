package kz.abdiyev.walletai.di

import kz.abdiyev.walletai.data.database.AppDatabase
import kz.abdiyev.walletai.data.preferences.SettingsDataStore
import kz.abdiyev.walletai.data.repository.AdviceRepositoryImpl
import kz.abdiyev.walletai.data.repository.CategoryRepositoryImpl
import kz.abdiyev.walletai.data.repository.MerchantRuleRepositoryImpl
import kz.abdiyev.walletai.data.repository.SettingsRepositoryImpl
import kz.abdiyev.walletai.data.repository.TransactionRepositoryImpl
import kz.abdiyev.walletai.domain.repository.AdviceRepository
import kz.abdiyev.walletai.domain.repository.CategoryRepository
import kz.abdiyev.walletai.domain.repository.MerchantRuleRepository
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kz.abdiyev.walletai.ml.BankStatementParser
import kz.abdiyev.walletai.ml.TransactionClassifier
import kz.abdiyev.walletai.ml.VoiceInputParser
import kz.abdiyev.walletai.service.GeminiService
import kz.abdiyev.walletai.ui.viewmodel.AdvisorViewModel
import kz.abdiyev.walletai.ui.viewmodel.AnalyticsViewModel
import kz.abdiyev.walletai.ui.viewmodel.DashboardViewModel
import kz.abdiyev.walletai.ui.viewmodel.ProfileViewModel
import kz.abdiyev.walletai.notification.ReminderScheduler
import kz.abdiyev.walletai.util.Exporter
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { AppDatabase.getDatabase(androidContext()) }
    single { get<AppDatabase>().transactionDao() }
    single { get<AppDatabase>().categoryDao() }
    single { get<AppDatabase>().merchantRuleDao() }
    single { SettingsDataStore(androidContext()) }
    single<TransactionRepository> { TransactionRepositoryImpl(get()) }
    single<CategoryRepository> { CategoryRepositoryImpl(get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<AdviceRepository> { AdviceRepositoryImpl(get()) }
    single<MerchantRuleRepository> { MerchantRuleRepositoryImpl(get()) }
    single { GeminiService() }
    single { Exporter() }
    single { TransactionClassifier() }
    single { BankStatementParser(get()) }
    single { VoiceInputParser() }
    single { ReminderScheduler(androidContext()) }
    viewModel { DashboardViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { AnalyticsViewModel(get(), get(), get(), get()) }
    viewModel { AdvisorViewModel(get(), get(), get(), get()) }
    viewModel { ProfileViewModel(get(), get(), get(), get<AppDatabase>()) }
}
