package kz.abdiyev.walletai.di

import kz.abdiyev.walletai.BuildConfig
import kz.abdiyev.walletai.ai.GeminiClient
import kz.abdiyev.walletai.ml.BankStatementParser
import kz.abdiyev.walletai.ml.TransactionClassifier
import kz.abdiyev.walletai.ml.VoiceInputParser
import org.koin.dsl.module

val aiModule = module {
    single { GeminiClient(apiKeyProvider = { BuildConfig.GEMINI_API_KEY }) }
    single { TransactionClassifier() }
    single { BankStatementParser(get()) }
    single { VoiceInputParser() }
}
