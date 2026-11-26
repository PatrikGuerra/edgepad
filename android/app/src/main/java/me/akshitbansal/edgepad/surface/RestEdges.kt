package me.akshitbansal.edgepad.surface

/**
 * Rest areas: bands along the surface's edges where a hand may lie. A trackpad touch that STARTS in one is ignored
 * until it lifts, so a resting palm neither moves the pointer nor counts as a finger in a gesture. A finger that starts
 * outside and slides in keeps working. Every band is [fraction] of the surface's shorter side thick, so the same
 * setting gives the same strip upright and sideways. Pure geometry, so it is tested without a device.
 */
data class RestEdges(
    val left: Boolean,
    val top: Boolean,
    val right: Boolean,
    val bottom: Boolean,
    val fraction: Float,
) {
    val any: Boolean get() = fraction > 0f && (left || top || right || bottom)

    /** The band's thickness in px on a [w] by [h] surface. */
    fun band(
        w: Float,
        h: Float,
    ): Float = fraction * minOf(w, h)

    /** Whether ([x], [y]) on a [w] by [h] surface lies in a rest area. */
    fun contains(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
    ): Boolean {
        if (!any) return false
        val b = band(w, h)
        return (left && x < b) || (top && y < b) || (right && x > w - b) || (bottom && y > h - b)
    }
}
