package kz.abdiyev.walletai.test

import kz.abdiyev.walletai.util.Clock

class FakeClock(private var millis: Long = 1_700_000_000_000L) : Clock {
    override fun nowMillis(): Long = millis

    fun setMillis(value: Long) {
        millis = value
    }
}
