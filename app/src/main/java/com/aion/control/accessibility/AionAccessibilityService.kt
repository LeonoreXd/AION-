package com.aion.control.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent
import android.util.Log

/** Explicit, user-triggered UI actions. No continuous screen recording or remote listener. */
class AionAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() { super.onServiceConnected(); current = this }
    override fun onDestroy() { if (current === this) current = null; super.onDestroy() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* No event contents retained. */ }
    override fun onInterrupt() { Log.i(TAG, "Interrupted") }

    fun goBack() = performGlobalAction(GLOBAL_ACTION_BACK)
    fun goHome() = performGlobalAction(GLOBAL_ACTION_HOME)
    fun openRecents() = performGlobalAction(GLOBAL_ACTION_RECENTS)

    fun clickText(query: String): Boolean {
        if (query.isBlank()) return false
        val root = rootInActiveWindow ?: return false
        val node = root.findAccessibilityNodeInfosByText(query.trim()).firstOrNull() ?: return false
        var target: AccessibilityNodeInfo? = node
        repeat(8) {
            val n = target ?: return@repeat
            if (n.isClickable && n.isEnabled) return n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            target = n.parent
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun typeIntoFocusedField(value: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun scrollDown(): Boolean = scroll(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    fun scrollUp(): Boolean = scroll(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    private fun scroll(action: Int): Boolean {
        val root = rootInActiveWindow ?: return false
        fun find(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.isScrollable && node.isEnabled) return node
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val found = find(child)
                if (found != null) return found
            }
            return null
        }
        return find(root)?.performAction(action) ?: false
    }

    companion object {
        private const val TAG = "AION.Accessibility"
        @Volatile var current: AionAccessibilityService? = null
            private set
    }
}
