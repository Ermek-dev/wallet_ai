package kz.abdiyev.walletai

import kz.abdiyev.walletai.ui.TiltMath
import kz.abdiyev.walletai.ui.TiltOffset
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TiltMathTest {

    @Test
    fun fromRotationVector_flatDevice_returnsNearZero() {
        // Identity quaternion: w=1, x=y=z=0 → device flat, no tilt
        val offset = TiltMath.fromRotationVector(floatArrayOf(0f, 0f, 0f, 1f))
        assertTrue(abs(offset.x) < 0.05f, "x should be near 0, was ${offset.x}")
        assertTrue(abs(offset.y) < 0.05f, "y should be near 0, was ${offset.y}")
    }

    @Test
    fun fromRotationVector_threeComponent_flat_returnsNearZero() {
        val offset = TiltMath.fromRotationVector(floatArrayOf(0f, 0f, 0f))
        assertTrue(abs(offset.x) < 0.05f)
        assertTrue(abs(offset.y) < 0.05f)
    }

    @Test
    fun fromRotationVector_resultAlwaysInRange() {
        val offset = TiltMath.fromRotationVector(floatArrayOf(0f, 0f, 0.707f, 0.707f))
        assertTrue(offset.x in -1f..1f)
        assertTrue(offset.y in -1f..1f)
    }

    @Test
    fun fromAccelerometer_clampsBeyond30Degrees() {
        // ~60° forward tilt — normalized value should clamp to ±1
        val offset = TiltMath.fromAccelerometer(0f, 8.5f, -5f)
        assertTrue(offset.x in -1f..1f)
        assertTrue(offset.y in -1f..1f)
        assertTrue(abs(offset.y) >= 0.9f)
    }

    @Test
    fun fromAccelerometer_flat_returnsNearZero() {
        // Device flat on table: gravity along -Z
        val offset = TiltMath.fromAccelerometer(0f, 0f, -9.81f)
        assertTrue(abs(offset.x) < 0.05f)
        assertTrue(abs(offset.y) < 0.05f)
    }

    @Test
    fun fromAccelerometer_tilted_returnsNonZero() {
        // Tilted forward: gravity component on Y axis
        val offset = TiltMath.fromAccelerometer(0f, 5f, -8f)
        assertTrue(abs(offset.y) > 0.1f)
    }

    @Test
    fun fromAccelerometer_resultAlwaysInRange() {
        val offset = TiltMath.fromAccelerometer(3f, 4f, 5f)
        assertTrue(offset.x in -1f..1f)
        assertTrue(offset.y in -1f..1f)
    }

    @Test
    fun smooth_convergesTowardTarget() {
        val prev = TiltOffset(0f, 0f)
        val target = TiltOffset(1f, -0.5f)
        var current = prev
        repeat(20) {
            current = TiltMath.smooth(current, target, alpha = 0.3f)
        }
        assertTrue(abs(current.x - target.x) < 0.05f)
        assertTrue(abs(current.y - target.y) < 0.05f)
    }

    @Test
    fun smooth_firstStepIsPartial() {
        val result = TiltMath.smooth(TiltOffset(0f, 0f), TiltOffset(1f, 0f), alpha = 0.2f)
        assertEquals(0.2f, result.x, absoluteTolerance = 0.001f)
        assertEquals(0f, result.y, absoluteTolerance = 0.001f)
    }

    @Test
    fun clampNormalized_clampsToUnitRange() {
        assertEquals(1f, TiltMath.clampNormalized(2f))
        assertEquals(-1f, TiltMath.clampNormalized(-2f))
        assertEquals(0.5f, TiltMath.clampNormalized(0.5f))
    }

    @Test
    fun staticOffset_isTopCenter() {
        assertEquals(0f, TiltOffset.STATIC.x)
        assertTrue(TiltOffset.STATIC.y < 0f)
    }
}
