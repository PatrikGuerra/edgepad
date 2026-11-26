package me.akshitbansal.edgepad.surface

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestEdgesTest {
    // A 2560 x 1600 tablet held sideways: the band is 10 % of 1600, 160 px.
    private val w = 2560f
    private val h = 1600f

    @Test
    fun bandFollowsTheShorterSide() {
        assertEquals(160f, RestEdges(false, false, false, true, 0.1f).band(w, h), 0f)
        assertEquals(160f, RestEdges(false, false, false, true, 0.1f).band(h, w), 0f)
    }

    @Test
    fun onlyTheEdgesSwitchedOnRest() {
        val bottom = RestEdges(left = false, top = false, right = false, bottom = true, fraction = 0.1f)
        assertTrue(bottom.contains(1280f, 1500f, w, h))
        assertFalse(bottom.contains(1280f, 1400f, w, h))
        assertFalse(bottom.contains(50f, 800f, w, h))
    }

    @Test
    fun everyEdge() {
        val all = RestEdges(left = true, top = true, right = true, bottom = true, fraction = 0.1f)
        assertTrue(all.contains(100f, 800f, w, h))
        assertTrue(all.contains(1280f, 100f, w, h))
        assertTrue(all.contains(2500f, 800f, w, h))
        assertFalse(all.contains(1280f, 800f, w, h))
    }

    @Test
    fun zeroWidthOrNoEdgeRestsNothing() {
        assertFalse(RestEdges(true, true, true, true, 0f).contains(0f, 0f, w, h))
        assertFalse(RestEdges(false, false, false, false, 0.3f).contains(0f, 0f, w, h))
    }
}
