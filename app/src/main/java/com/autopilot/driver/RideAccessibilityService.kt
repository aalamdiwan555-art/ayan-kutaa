package com.autopilot.driver

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicLong

class RideAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val acceptKeywords = OcrKeywords.ACCEPT_KEYWORDS
    private val pricePatterns = OcrKeywords.PRICE_PATTERNS
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
        if (!BotState.isRunning || !AppPrefs.isLoggedIn || !AppPrefs.hasActiveSubscription()) return
        if (System.currentTimeMillis() - lastClickAt.get() < 750L) return

        val rootNode = rootInActiveWindow ?: return
        serviceScope.launch(Dispatchers.Default) {
            try {
                scanAndAccept(rootNode)
            } finally {
                rootNode.recycle()
            }
        }
    }

    private fun scanAndAccept(root: AccessibilityNodeInfo) {
        val acceptButtons = findAcceptButtons(root)
        if (acceptButtons.isEmpty()) return

        val price = findPriceOnScreen(root)
        val minPrice = AppPrefs.minPrice
        val maxPrice = AppPrefs.maxPrice

        if (price != null) {
            if (price < minPrice || price > maxPrice) {
                for (btn in acceptButtons) btn.recycle()
                return
            }
        }

        for (btn in acceptButtons) {
            serviceScope.launch(Dispatchers.Main) {
                if (lastClickAt.compareAndSet(lastClickAt.get(), System.currentTimeMillis())) {
                    val clicked = btn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) {
                        AppPrefs.addRewardPoints(10)
                        Toast.makeText(
                            this@RideAccessibilityService,
                            "Ride accepted! ₹$price (+10 points)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            btn.recycle()
        }
    }

    private fun findAcceptButtons(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isClickable) {
                val text = (node.text?.toString() ?: "") +
                        (node.contentDescription?.toString() ?: "")
                if (acceptKeywords.any { text.contains(it, ignoreCase = true) }) {
                    results.add(AccessibilityNodeInfo.obtain(node))
                }
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return results
    }

    private fun findPriceOnScreen(root: AccessibilityNodeInfo): Double? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var bestPrice: Double? = null

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString() ?: ""

            for (pattern in pricePatterns) {
                val match = pattern.find(text)
                if (match != null) {
                    val raw = match.groupValues[1].replace(",", "")
                    val value = raw.toDoubleOrNull() ?: continue
                    if (bestPrice == null || value > bestPrice) {
                        bestPrice = value
                    }
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return bestPrice
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
