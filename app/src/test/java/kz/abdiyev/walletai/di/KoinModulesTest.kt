package kz.abdiyev.walletai.di

import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.stopKoin
import org.koin.dsl.koinApplication
import org.koin.test.check.checkModules
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class KoinModulesTest {
    @Before
    @After
    fun resetKoin() {
        stopKoin()
    }

    @Test
    fun appModulesAreValid() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        koinApplication {
            androidContext(context)
            modules(appModules)
            checkModules()
        }
    }
}
