package kz.abdiyev.walletai.di

import kz.abdiyev.walletai.data.repository.AdviceRepositoryImpl
import kz.abdiyev.walletai.data.repository.CategoryRepositoryImpl
import kz.abdiyev.walletai.data.repository.MaintenanceRepositoryImpl
import kz.abdiyev.walletai.data.repository.MerchantRuleRepositoryImpl
import kz.abdiyev.walletai.data.repository.SettingsRepositoryImpl
import kz.abdiyev.walletai.data.repository.TransactionRepositoryImpl
import kz.abdiyev.walletai.domain.repository.AdviceRepository
import kz.abdiyev.walletai.domain.repository.CategoryRepository
import kz.abdiyev.walletai.domain.repository.MaintenanceRepository
import kz.abdiyev.walletai.domain.repository.MerchantRuleRepository
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kz.abdiyev.walletai.util.Exporter
import org.koin.dsl.module

val repositoryModule = module {
    single<TransactionRepository> { TransactionRepositoryImpl(get()) }
    single<CategoryRepository> { CategoryRepositoryImpl(get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<AdviceRepository> { AdviceRepositoryImpl(get()) }
    single<MerchantRuleRepository> { MerchantRuleRepositoryImpl(get()) }
    single<MaintenanceRepository> { MaintenanceRepositoryImpl(get()) }
    single { Exporter() }
}
