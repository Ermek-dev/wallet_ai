package kz.abdiyev.walletai.di

import kz.abdiyev.walletai.BuildConfig
import kz.abdiyev.walletai.ai.AdviceAiClient
import kz.abdiyev.walletai.ai.GeminiClient
import kz.abdiyev.walletai.ai.GroqClient
import kz.abdiyev.walletai.ml.BankStatementParser
import kz.abdiyev.walletai.ml.TransactionClassifier
import kz.abdiyev.walletai.ml.VoiceInputParser
import org.koin.dsl.module

val aiModule = module {
    single { GeminiClient(apiKeyProvider = { BuildConfig.GEMINI_API_KEY }) }
    single { GroqClient(apiKeyProvider = { BuildConfig.GROQ_API_KEY }) }
    single<AdviceAiClient> { get<GroqClient>() }
    single { TransactionClassifier() }
    single { BankStatementParser(get()) }
    single { VoiceInputParser() }
}
