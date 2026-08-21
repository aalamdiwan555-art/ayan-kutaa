package com.autopilot.driver

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class RideAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val acceptKeywords = OcrKeywords.ACCEPT_KEYWORDS
    private val pricePatterns = OcrKeywords.PRICE_PATTERNS
    private val scanInProgress = AtomicBoolean(false)
    private val lastScanAt = AtomicLong(0L)
    private val lastClickAt = AtomicLong(0L)

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!AppPrefs.isLoggedIn || !BotState.isRunning || !AppPrefs.hasActiveSubscription()) {
            stopBotState()
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastScanAt.get() < SCAN_DEBOUNCE_MS ||
            !scanInProgress.compareAndSet(false, true)
        ) return
        lastScanAt.set(now)

        serviceScope.launch {
            val root = rootInActiveWindow
            try {
                if (root != null) scanAndAccept(root)
            } finally {
                root?.recycle()
                scanInProgress.set(false)
            }
        }
    }

    private fun scanAndAccept(root: AccessibilityNodeInfo) {
        val acceptButton = findAcceptButton(root) ?: return
        try {
            val price = findPriceOnScreen(root)
            val minPrice = AppPrefs.minPrice
            val maxPrice = AppPrefs.maxPrice
            if (price != null && (price < minPrice || price > maxPrice)) return

            val now = System.currentTimeMillis()
            if (now - lastClickAt.get() < CLICK_DEBOUNCE_MS) return
            lastClickAt.set(now)

            val clicked = runCatching {
                acceptButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }.getOrDefault(false)
            if (clicked) {
                AppPrefs.addRewardPoints(10)
                Toast.makeText(
                    this,
                    "Ride accepted! ₹$price (+10 points)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } finally {
            acceptButton.recycle()
        }
    }

    private fun findAcceptButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var result: AccessibilityNodeInfo? = null
        traverse(root) { node ->
            if (result == null && node.isClickable && acceptKeywords.any { keyword ->
                    node.nodeText().contains(keyword, ignoreCase = true)
                }) {
                result = AccessibilityNodeInfo.obtain(node)
            }
        }
        return result
    }

    private fun findPriceOnScreen(root: AccessibilityNodeInfo): Double? {
        var bestPrice: Double? = null
        traverse(root) { node ->
            val text = node.nodeText().normalizeDigits()
            for (pattern in pricePatterns) {
                val match = pattern.find(text) ?: continue
                val raw = match.groupValues.getOrNull(1)?.replace(",", "") ?: continue
                val value = raw.toDoubleOrNull() ?: continue
                if (bestPrice == null || value > bestPrice!!) bestPrice = value
            }
        }
        return bestPrice
    }

    private fun traverse(root: AccessibilityNodeInfo, visitor: (AccessibilityNodeInfo) -> Unit) {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(AccessibilityNodeInfo.obtain(root))
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            try {
                visitor(node)
                val childCount = runCatching { node.childCount }.getOrDefault(0)
                for (index in 0 until childCount) {
                    runCatching { node.getChild(index) }.getOrNull()?.let(queue::add)
                }
            } finally {
                node.recycle()
            }
        }
    }

    private fun AccessibilityNodeInfo.nodeText(): String {
        val text = runCatching { text?.toString() }.getOrNull().orEmpty()
        val description = runCatching { contentDescription?.toString() }.getOrNull().orEmpty()
        return "$text $description".trim()
    }

    private fun String.normalizeDigits(): String = buildString(length) {
        for (character in this@normalizeDigits) {
            append(
                when (character) {
                    in '\u0966'..'\u096F' -> character - '\u0966' + '0'.code
                    in '\u0660'..'\u0669' -> character - '\u0660' + '0'.code
                    in '\u06F0'..'\u06F9' -> character - '\u06F0' + '0'.code
                    else -> character.code
                }.toChar()
            )
        }
    }

    private fun stopBotState() {
        BotState.isRunning = false
        AppPrefs.isBotRunning = false
    }

    override fun onInterrupt() {
        stopBotState()
        scanInProgress.set(false)
    }

    override fun onDestroy() {
        stopBotState()
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val SCAN_DEBOUNCE_MS = 300L
        const val CLICK_DEBOUNCE_MS = 750L
    }
}
