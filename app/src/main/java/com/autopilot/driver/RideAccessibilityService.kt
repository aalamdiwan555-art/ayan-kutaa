package com.autopilot.driver

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.PowerManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import android.util.Log
import kotlin.math.hypot
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class RideAccessibilityService : AccessibilityService() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.w(TAG, "Accessibility scan failed", throwable)
        scanInProgress.set(false)
    }
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob() + exceptionHandler)
    private val acceptKeywords = OcrKeywords.ACCEPT_KEYWORDS
    private val pricePatterns = OcrKeywords.PRICE_PATTERNS
    private val scanInProgress = AtomicBoolean(false)
    private val lastScanAt = AtomicLong(0L)
    private val lastClickAt = AtomicLong(0L)

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (!AppPrefs.isInitialized()) AppPrefs.init(this)
        scanInProgress.set(false)
        lastScanAt.set(0L)
        lastClickAt.set(0L)
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
        val packageName = event.packageName?.toString() ?: return
        
        // FIX #1: Allow ALL apps, not just hardcoded 3
        // If you want to restrict, add your app package here
        // For now: scan every app to catch all ride offers
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isInteractive) return
        
        if (!AppPrefs.isLoggedIn || !BotState.isRunning || !AppPrefs.hasActiveSubscription()) {
            stopBotState()
            return
        }

        if (!scanInProgress.compareAndSet(false, true)) return
        val now = System.currentTimeMillis()
        if (now - lastScanAt.get() < SCAN_DEBOUNCE_MS) {
            scanInProgress.set(false)
            return
        }
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
            val price = findPriceNearButton(acceptButton, root)
            val minPrice = AppPrefs.minPrice
            val maxPrice = AppPrefs.maxPrice
            
            if (price != null && (price < minPrice || price > maxPrice)) {
                Log.d(TAG, "Price ₹$price out of range [₹$minPrice - ₹$maxPrice]")
                return
            }

            val now = System.currentTimeMillis()
            if (now - lastClickAt.get() < CLICK_DEBOUNCE_MS) return
            lastClickAt.set(now)

            // FIX #2: Try multiple click methods
            val clicked = tryClickButton(acceptButton)
            if (clicked) {
                AppPrefs.addRewardPoints(10)
                serviceScope.launch(Dispatchers.Main) {
                    Toast.makeText(this@RideAccessibilityService, "Ride accepted! ₹$price (+10 points)", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.w(TAG, "All click methods failed for button: ${acceptButton.nodeText()}")
            }
        } finally {
            acceptButton.recycle()
        }
    }

    // FIX #3: Look for clickable OR its clickable parent
    private fun findAcceptButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val candidates = mutableListOf<Pair<AccessibilityNodeInfo, Int>>()
        
        traverse(root) { node ->
            val text = node.nodeText()
            val score = acceptKeywords.sumOf { keyword ->
                if (text.contains(keyword, ignoreCase = true)) keyword.length else 0
            }
            if (score > 0) {
                // FIX #3a: If node itself is clickable, use it
                // FIX #3b: If parent is clickable, use parent instead
                val clickableNode = if (node.isClickable) {
                    node
                } else {
                    findClickableParent(node)
                }
                if (clickableNode != null) {
                    candidates.add(AccessibilityNodeInfo.obtain(clickableNode) to score)
                }
            }
        }
        
        val best = candidates.maxByOrNull { it.second }?.first
        candidates.filter { it.first !== best }.forEach { it.first.recycle() }
        return best
    }
    
    private fun findClickableParent(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var current = node?.parent
        var depth = 0
        while (current != null && depth < 5) {
            if (current.isClickable) return current
            val next = current.parent
            current.recycle()
            current = next
            depth++
        }
        return null
    }

    private fun findPriceNearButton(button: AccessibilityNodeInfo, root: AccessibilityNodeInfo): Double? {
        val buttonBounds = Rect()
        button.getBoundsInScreen(buttonBounds)
        var bestPrice: Pair<Double, Double>? = null
        traverse(root) { node ->
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            val distance = hypot(
                (buttonBounds.centerX() - bounds.centerX()).toDouble(),
                (buttonBounds.centerY() - bounds.centerY()).toDouble()
            )
            val nodeText = node.nodeText()
            if (nodeText != button.nodeText() && distance <= PRICE_PROXIMITY_PX) {
                parsePrice(nodeText)?.let { value ->
                    if (bestPrice == null || distance < bestPrice!!.second) {
                        bestPrice = value to distance
                    }
                }
            }
        }
        return bestPrice?.first
    }

    private fun parsePrice(text: String): Double? {
        val normalized = text.normalizeDigits()
        return pricePatterns.asSequence().mapNotNull { pattern ->
            pattern.find(normalized)?.groupValues?.getOrNull(1)?.let(::parseNumericValue)
        }.firstOrNull()
    }

    private fun parseNumericValue(raw: String): Double? {
        val compact = raw.replace(",", "").replace(" ", "")
        return if (compact.count { it == '.' } > 1 && compact.length >= 7) {
            compact.replace(".", "").toDoubleOrNull()
        } else {
            compact.toDoubleOrNull()
        }
    }

    // FIX #4: Try multiple click methods in order
    private fun tryClickButton(button: AccessibilityNodeInfo): Boolean {
        // Method 1: Standard accessibility click
        val clicked = runCatching {
            button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }.getOrDefault(false)
        if (clicked) {
            Log.d(TAG, "Clicked via ACTION_CLICK")
            return true
        }
        
        // Method 2: Gesture fallback with longer duration
        val gestureClicked = dispatchGestureFallback(button)
        if (gestureClicked) {
            Log.d(TAG, "Clicked via gesture fallback")
            return true
        }
        
        // Method 3: Try clicking parent if button itself failed
        val parent = button.parent
        if (parent != null) {
            val parentClicked = runCatching {
                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }.getOrDefault(false)
            parent.recycle()
            if (parentClicked) {
                Log.d(TAG, "Clicked via parent")
                return true
            }
        }
        
        return false
    }

    private fun dispatchGestureFallback(button: AccessibilityNodeInfo): Boolean {
        val rect = Rect()
        button.getBoundsInScreen(rect)
        if (rect.isEmpty) return false
        val path = Path().apply { moveTo(rect.centerX().toFloat(), rect.centerY().toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 250)) // FIX: longer duration
            .build()
        return dispatchGesture(gesture, null, null)
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
                    in '\u0966'..'\u096F' -> (character.code - '\u0966'.code + '0'.code).toChar()
                    in '\u0660'..'\u0669' -> (character.code - '\u0660'.code + '0'.code).toChar()
                    in '\u06F0'..'\u06F9' -> (character.code - '\u06F0'.code + '0'.code).toChar()
                    else -> character
                }
            )
        }
    }

    private fun stopBotState() {
        BotState.isRunning = false
        AppPrefs.isBotRunning = false
    }

    override fun onInterrupt() {
        scanInProgress.set(false)
    }

    override fun onDestroy() {
        stopBotState()
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "RideService"
        const val SCAN_DEBOUNCE_MS = 150L
        const val CLICK_DEBOUNCE_MS = 500L
        const val PRICE_PROXIMITY_PX = 800.0 // FIX: increased from 500
    }
}
