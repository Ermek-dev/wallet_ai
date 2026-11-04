package kz.abdiyev.walletai.di

import kz.abdiyev.walletai.domain.port.MerchantRuleStore
import kz.abdiyev.walletai.domain.port.TransactionStore
import kz.abdiyev.walletai.domain.repository.MerchantRuleRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kz.abdiyev.walletai.domain.usecase.ImportStatementUseCase
import kz.abdiyev.walletai.domain.usecase.SaveTransactionUseCase
import org.koin.dsl.module

val useCaseModule = module {
    single<TransactionStore> { get<TransactionRepository>() }
    single<MerchantRuleStore> { get<MerchantRuleRepository>() }
    single { SaveTransactionUseCase(get(), get(), get()) }
    single { ImportStatementUseCase(get(), get(), get()) }
}
