package com.ultron.assistant

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * PHASE 1 — stub only. This is what will let Ultron read screen content and
 * tap/type inside OTHER apps (real cross-app control), once wired up in
 * Phase 2. The user has to manually enable this in Settings > Accessibility
 * — Android requires an explicit, informed opt-in and won't let any app
 * grant itself this permission silently.
 *
 * Helper methods below are provided but unused for now — Phase 2 connects
 * them to voice commands (e.g. "tap send", "read this screen").
 */
class UltronAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Phase 2: inspect event.source / rootInActiveWindow here to know
        // what's on screen, and react to specific apps/screens if needed.
    }

    override fun onInterrupt() {}

    /** Finds the first visible node whose text or content-description contains `label`. */
    fun findNodeByText(label: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return searchNode(root, label.lowercase())
    }

    private fun searchNode(node: AccessibilityNodeInfo, label: String): AccessibilityNodeInfo? {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        if (text.contains(label) || desc.contains(label)) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = searchNode(child, label)
            if (found != null) return found
        }
        return null
    }

    /** Simulates a tap on a given node, if it's clickable. */
    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }
}
