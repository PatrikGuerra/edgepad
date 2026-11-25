package me.akshitbansal.edgepad.surface

import android.annotation.SuppressLint
import android.content.Context
import me.akshitbansal.edgepad.Settings
import kotlin.math.roundToInt

/**
 * The display's corner radius in px before Android 12, which has no API for it: the radius set in Dial feel, or at
 * Auto the system's own internal dimen, which many phones fill in and some leave at 0.
 */
object ScreenCorner {
    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    fun legacyRadius(
        context: Context,
        settings: Settings,
    ): Int {
        val res = context.resources
        val dp = settings.screenCorner
        if (dp > 0f) return (dp * res.displayMetrics.density).roundToInt()
        val id = res.getIdentifier("rounded_corner_radius", "dimen", "android")
        return if (id != 0) res.getDimensionPixelSize(id) else 0
    }
}
