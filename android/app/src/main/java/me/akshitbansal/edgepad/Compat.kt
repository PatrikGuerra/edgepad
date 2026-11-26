@file:Suppress("DEPRECATION")

package me.akshitbansal.edgepad

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.accessibility.AccessibilityNodeInfo

/**
 * The few platform calls newer than Android 7.1 (API 25) that the app makes in many places, each answering with the
 * nearest older behaviour, or doing nothing where there is none. One-off calls are guarded where they are made.
 */

/** View.stateDescription (API 30): a spoken state; older Androids have no slot for it. */
var View.stateDesc: CharSequence?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) stateDescription else null
    set(value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) stateDescription = value
    }

var AccessibilityNodeInfo.stateDesc: CharSequence?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) stateDescription else null
    set(value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) stateDescription = value
    }

/** View.isAccessibilityHeading (API 28). */
var View.a11yHeading: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isAccessibilityHeading
    set(value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isAccessibilityHeading = value
    }

/** View.accessibilityPaneTitle (API 28). */
var View.paneTitle: CharSequence?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) accessibilityPaneTitle else null
    set(value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) accessibilityPaneTitle = value
    }

/** View.tooltipText (API 26). */
var View.tooltip: CharSequence?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) tooltipText else null
    set(value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) tooltipText = value
    }

/** View.importantForAutofill = NO (API 26); older Androids have no autofill to keep out. */
fun View.noAutofill() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
}

/** ValueAnimator.areAnimatorsEnabled (API 26); before it animators always run. */
fun animatorsEnabled(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()

/** The system bars and the display cutout together, as a Rect of insets (WindowInsets.getInsets is API 30). */
fun WindowInsets.barsAndCutout(): Rect {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val i = getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
        return Rect(i.left, i.top, i.right, i.bottom)
    }
    // Before 30 the system-window insets already cover the bars and, from 28, the cutout.
    return Rect(systemWindowInsetLeft, systemWindowInsetTop, systemWindowInsetRight, systemWindowInsetBottom)
}

/** The open keyboard's height; before API 30 it is part of the system-window bottom inset. */
fun WindowInsets.imeBottom(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) getInsets(WindowInsets.Type.ime()).bottom else systemWindowInsetBottom

/** Notification.Builder on [channel] (API 26), or the channel-less builder before channels existed. */
fun notificationBuilder(
    context: Context,
    channel: String,
): Notification.Builder =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(context, channel) else Notification.Builder(context)

/** Creates a notification channel (API 26); before channels, nothing to create. */
fun NotificationManager.channel(
    id: String,
    name: CharSequence,
    importance: Int,
    showBadge: Boolean = true,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    createNotificationChannel(NotificationChannel(id, name, importance).apply { setShowBadge(showBadge) })
}

/** AccessibilityNodeInfo(host[, virtualId]) is API 30; obtain() is the older spelling of the same thing. */
fun nodeInfo(
    host: View,
    virtualViewId: Int? = null,
): AccessibilityNodeInfo =
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && virtualViewId == null -> AccessibilityNodeInfo(host)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> AccessibilityNodeInfo(host, virtualViewId!!)
        virtualViewId == null -> AccessibilityNodeInfo.obtain(host)
        else -> AccessibilityNodeInfo.obtain(host, virtualViewId)
    }
