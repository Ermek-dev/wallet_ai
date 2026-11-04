package kz.abdiyev.walletai.di

import kz.abdiyev.walletai.sensor.TiltSensorMonitor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val sensorModule = module {
    single { TiltSensorMonitor(androidContext()) }
}
