package com.ezla.oslauncher.compass

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class HeadingMathTest {

    @Test fun normalizeWrapsNegativeAndOver360() {
        assertEquals(350f, HeadingMath.normalize(-10f), 0.001f)
        assertEquals(10f, HeadingMath.normalize(370f), 0.001f)
        assertEquals(0f, HeadingMath.normalize(360f), 0.001f)
    }

    @Test fun smoothTakesShortestPathAcrossNorth() {
        assertEquals(0f, HeadingMath.smooth(358f, 2f, 0.5f), 0.001f)
        assertEquals(355f, HeadingMath.smooth(5f, 345f, 0.5f), 0.001f)
        assertEquals(20f, HeadingMath.smooth(10f, 30f, 0.5f), 0.001f)
    }

    @Test fun directionIndexBoundaries() {
        assertEquals(0, HeadingMath.directionIndex(0f))
        assertEquals(0, HeadingMath.directionIndex(22.4f))
        assertEquals(1, HeadingMath.directionIndex(22.5f))
        assertEquals(1, HeadingMath.directionIndex(48f))
        assertEquals(4, HeadingMath.directionIndex(180f))
        assertEquals(7, HeadingMath.directionIndex(330f))
        assertEquals(0, HeadingMath.directionIndex(350f))
    }

    @Test fun displayDegreesRoundsAndWraps() {
        assertEquals(0, HeadingMath.displayDegrees(359.6f))
        assertEquals(48, HeadingMath.displayDegrees(47.6f))
    }

    @Test fun dmsCarriesRoundedSeconds() {
        assertArrayEquals(intArrayOf(21, 1, 42), HeadingMath.dms(21.028333))
        assertArrayEquals(intArrayOf(11, 0, 0), HeadingMath.dms(10.9999999))
    }

    @Test fun formatCoordinateHemispheres() {
        val h = arrayOf("N", "E", "S", "W")
        assertEquals("21°1′42″N  105°51′12″E", HeadingMath.formatCoordinate(21.028333, 105.853333) { h[it] })
        assertEquals("33°52′0″S  151°12′0″W", HeadingMath.formatCoordinate(-33.866667, -151.2) { h[it] })
    }
}
