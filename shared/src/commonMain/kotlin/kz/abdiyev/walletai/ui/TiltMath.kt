package kz.abdiyev.walletai.ui

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Normalized device tilt for UI glow positioning.
 * [x] and [y] are in [-1, 1] where positive x = tilt right, positive y = tilt down.
 */
data class TiltOffset(
    val x: Float,
    val y: Float
) {
    companion object {
        /** Static highlight position: top-center of the element. */
        val STATIC = TiltOffset(x = 0f, y = -0.6f)
    }
}

object TiltMath {
    private const val MAX_TILT_DEGREES = 30f
    private const val MAX_TILT_RADIANS = MAX_TILT_DEGREES * (Math.PI / 180.0).toFloat()

    /**
     * Converts a rotation vector sensor reading to normalized tilt offset.
     * Accepts 3-component (x, y, z) or 4-component (x, y, z, w) quaternion.
     */
    fun fromRotationVector(rotationVector: FloatArray): TiltOffset {
        val (qx, qy, qz, qw) = when (rotationVector.size) {
            4 -> Quaternion(rotationVector[0], rotationVector[1], rotationVector[2], rotationVector[3])
            3 -> {
                val q = rotationVectorToQuaternion(rotationVector[0], rotationVector[1], rotationVector[2])
                Quaternion(q[0], q[1], q[2], q[3])
            }
            else -> return TiltOffset.STATIC
        }

        // Rotation matrix from quaternion (row-major, Android convention)
        val r00 = 1f - 2f * (qy * qy + qz * qz)
        val r10 = 2f * (qx * qy + qz * qw)
        val r20 = 2f * (qx * qz - qy * qw)
        val r21 = 2f * (qy * qz + qx * qw)
        val r22 = 1f - 2f * (qx * qx + qy * qy)

        // Pitch and roll from rotation matrix (device tilt relative to gravity)
        val pitch = asin((-r20).coerceIn(-1f, 1f))
        val roll = atan2(r21, r22)

        val x = clampNormalized(pitch / MAX_TILT_RADIANS)
        val y = clampNormalized(roll / MAX_TILT_RADIANS)
        return TiltOffset(x, y)
    }

    /**
     * Fallback: derive tilt from raw accelerometer values (m/s²).
     */
    fun fromAccelerometer(ax: Float, ay: Float, az: Float): TiltOffset {
        val magnitude = sqrt(ax * ax + ay * ay + az * az)
        if (magnitude < 0.1f) return TiltOffset.STATIC

        val nx = ax / magnitude
        val ny = ay / magnitude
        val nz = az / magnitude

        // Pitch: forward/back tilt; roll: left/right tilt
        val pitch = asin((-nx).coerceIn(-1f, 1f))
        val roll = atan2(ny, -nz)

        val x = clampNormalized(pitch / MAX_TILT_RADIANS)
        val y = clampNormalized(roll / MAX_TILT_RADIANS)
        return TiltOffset(x, y)
    }

    /** Exponential low-pass filter for smooth glow movement. */
    fun smooth(prev: TiltOffset, next: TiltOffset, alpha: Float): TiltOffset {
        val a = alpha.coerceIn(0f, 1f)
        return TiltOffset(
            x = prev.x + a * (next.x - prev.x),
            y = prev.y + a * (next.y - prev.y)
        )
    }

    fun clampNormalized(value: Float): Float = value.coerceIn(-1f, 1f)

    private data class Quaternion(val x: Float, val y: Float, val z: Float, val w: Float)

    /**
     * Converts 3-component rotation vector to unit quaternion.
     * See Android SensorManager.getQuaternionFromVector.
     */
    private fun rotationVectorToQuaternion(x: Float, y: Float, z: Float): FloatArray {
        val lengthSq = x * x + y * y + z * z
        val w = if (lengthSq < 1f) sqrt(1f - lengthSq) else 0f
        return floatArrayOf(x, y, z, w)
    }
}
