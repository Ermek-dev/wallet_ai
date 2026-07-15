package kz.abdiyev.walletai.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kz.abdiyev.walletai.ui.TiltMath
import kz.abdiyev.walletai.ui.TiltOffset
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class TiltSensorMonitor(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val isRotationVector: Boolean =
        sensor?.type == Sensor.TYPE_GAME_ROTATION_VECTOR ||
            sensor?.type == Sensor.TYPE_ROTATION_VECTOR

    private val rawTiltFlow: Flow<TiltOffset> = callbackFlow {
        val currentSensor = sensor
        if (currentSensor == null) {
            trySend(TiltOffset.STATIC)
            awaitClose { }
            return@callbackFlow
        }

        var smoothed = TiltOffset.STATIC

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val raw = if (isRotationVector) {
                    TiltMath.fromRotationVector(event.values.copyOf())
                } else {
                    TiltMath.fromAccelerometer(
                        event.values[0],
                        event.values[1],
                        event.values[2]
                    )
                }
                smoothed = TiltMath.smooth(smoothed, raw, alpha = SMOOTH_ALPHA)
                trySend(smoothed)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(
            listener,
            currentSensor,
            SensorManager.SENSOR_DELAY_GAME
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }.distinctUntilChanged { old, new ->
        old.x == new.x && old.y == new.y
    }

    val tiltOffset: StateFlow<TiltOffset> = rawTiltFlow.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = TiltOffset.STATIC
    )

    private companion object {
        const val SMOOTH_ALPHA = 0.15f
    }
}
