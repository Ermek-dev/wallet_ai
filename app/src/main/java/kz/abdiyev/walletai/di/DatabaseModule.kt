package kz.abdiyev.walletai.di

import kz.abdiyev.walletai.data.database.AppDatabase
import kz.abdiyev.walletai.data.preferences.AdviceCacheDataStore
import kz.abdiyev.walletai.data.preferences.SettingsDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single { AppDatabase.getDatabase(androidContext()) }
    single { get<AppDatabase>().transactionDao() }
    single { get<AppDatabase>().categoryDao() }
    single { get<AppDatabase>().merchantRuleDao() }
    single { SettingsDataStore(androidContext()) }
    single { AdviceCacheDataStore(androidContext()) }
}
