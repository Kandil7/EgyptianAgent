package com.egyptian.agent.navigation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import com.egyptian.agent.accessibility.ui.AccessibilityTreeParser
import com.egyptian.agent.accessibility.ui.UIElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * UI Navigation Engine for EgyptianAgent Hybrid Architecture.
 * 
 * Executes UI navigation actions via AccessibilityService.
 * Implements DroidClaw-style perception → reason → action loop.
 *
 * Features:
 * - 28 UI actions (tap, swipe, type, scroll, etc.)
 * - Action execution via AccessibilityService
 * - Action feedback (did it work?)
 * - Error recovery
 * - Perception → Reason → Action loop
 *
 * @author EgyptianAgent Team
 * @version 1.0.0
 */
class UINavigationEngine(
    private val accessibilityService: AccessibilityService,
    private val context: Context
) {
    companion object {
        private const val TAG = "UINavigationEngine"
        private const val DEFAULT_TAP_DURATION = 150L
        private const val DEFAULT_SWIPE_DURATION = 300L
        private const val MAX_ACTION_RETRIES = 3
    }

    private val treeParser = AccessibilityTreeParser(accessibilityService)
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var actionHistory = mutableListOf<Pair<UIAction, ActionResult>>()
    private var lastTreeHash: Int = 0

    /**
     * Execute a single UI action.
     * 
     * @param action Action to execute
     * @return ActionResult with success/failure
     */
    suspend fun executeAction(action: UIAction): ActionResult = withContext(Dispatchers.Main) {
        Log.d(TAG, "Executing action: ${action.name} - ${action.description}")
        
        val startTime = System.currentTimeMillis()
        val treeBefore = treeParser.parseCurrentTree()
        
        try {
            val result = when (action) {
                is Tap -> executeTap(action)
                is Type -> executeType(action)
                is Enter -> executeEnter(action)
                is LongPress -> executeLongPress(action)
                is Clear -> executeClear(action)
                is Paste -> executePaste(action)
                is Swipe -> executeSwipe(action)
                is Scroll -> executeScroll(action)
                is Home -> executeHome(action)
                is Back -> executeBack(action)
                is Launch -> executeLaunch(action)
                is SwitchApp -> executeSwitchApp(action)
                is OpenUrl -> executeOpenUrl(action)
                is OpenSettings -> executeOpenSettings(action)
                is Notifications -> executeNotifications(action)
                is ClipboardGet -> executeClipboardGet(action)
                is ClipboardSet -> executeClipboardSet(action)
                is ReadScreen -> executeReadScreen(action)
                is SubmitMessage -> executeSubmitMessage(action)
                is CopyVisibleText -> executeCopyVisibleText(action)
                is WaitForContent -> executeWaitForContent(action)
                is FindAndTap -> executeFindAndTap(action)
                is ComposeEmail -> executeComposeEmail(action)
                is Screenshot -> executeScreenshot(action)
                is Shell -> executeShell(action)
                is KeyEvent -> executeKeyEvent(action)
                is PullFile -> executePullFile(action)
                is PushFile -> executePushFile(action)
                is Wait -> executeWait(action)
                is Done -> executeDone(action)
            }
            
            // Check if screen changed
            val treeAfter = treeParser.parseCurrentTree()
            val diff = treeParser.diffTrees(treeBefore, treeAfter)
            
            val finalResult = result.copy(
                screenChanged = diff.screenChanged,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
            
            // Record action in history
            actionHistory.add(Pair(action, finalResult))
            
            Log.d(TAG, "Action ${action.name} completed: ${finalResult.success} in ${finalResult.executionTimeMs}ms")
            return@withContext finalResult
            
        } catch (e: Exception) {
            Log.e(TAG, "Error executing action ${action.name}", e)
            return@withContext ActionResult.failure(
                message = "Action failed: ${e.message}",
                error = e
            )
        }
    }

    // ========================================================================
    // Basic Interactions
    // ========================================================================

    private suspend fun executeTap(action: Tap): ActionResult = withContext(Dispatchers.Main) {
        try {
            if (action.elementId != null) {
                // Find element by ID and tap
                val node = findNodeById(action.elementId)
                if (node != null) {
                    val clicked = performClick(node)
                    node.recycle()
                    if (clicked) {
                        return@withContext ActionResult.success("Tapped element: ${action.elementId}")
                    }
                }
                return@withContext ActionResult.failure("Element not found: ${action.elementId}")
            } else if (action.x != null && action.y != null) {
                // Tap at coordinates
                val success = performTap(action.x, action.y)
                return@withContext if (success) {
                    ActionResult.success("Tapped at coordinates (${action.x}, ${action.y})")
                } else {
                    ActionResult.failure("Failed to tap at coordinates")
                }
            }
            
            return@withContext ActionResult.failure("Invalid tap parameters")
        } catch (e: Exception) {
            ActionResult.failure("Tap failed: ${e.message}", e)
        }
    }

    private suspend fun executeType(action: Type): ActionResult = withContext(Dispatchers.Main) {
        try {
            val node = if (action.targetId != null) {
                findNodeById(action.targetId)
            } else {
                findFocusableEditText()
            }
            
            if (node == null) {
                return@withContext ActionResult.failure("No input field found")
            }
            
            // Clear first if requested
            if (action.clearFirst) {
                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, 
                    Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                    })
                Thread.sleep(50)
            }
            
            // Type text
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,
                Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, action.text)
                })
            
            node.recycle()
            return@withContext ActionResult.success("Typed: ${action.text.take(50)}...")
            
        } catch (e: Exception) {
            ActionResult.failure("Type failed: ${e.message}", e)
        }
    }

    private suspend fun executeEnter(action: Enter): ActionResult = withContext(Dispatchers.Main) {
        try {
            val node = if (action.targetId != null) {
                findNodeById(action.targetId)
            } else {
                accessibilityService.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            }
            
            if (node != null) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                node.recycle()
                return@withContext ActionResult.success("Enter pressed")
            }
            
            // Fallback: send Enter key event
            return@withContext executeKeyEvent(KeyEvent(UIActions.KeyCodes.KEYCODE_ENTER))
            
        } catch (e: Exception) {
            ActionResult.failure("Enter failed: ${e.message}", e)
        }
    }

    private suspend fun executeLongPress(action: LongPress): ActionResult = withContext(Dispatchers.Main) {
        try {
            if (action.elementId != null) {
                val node = findNodeById(action.elementId)
                if (node != null) {
                    val success = performLongPress(node, action.duration)
                    node.recycle()
                    return@withContext if (success) {
                        ActionResult.success("Long pressed element: ${action.elementId}")
                    } else {
                        ActionResult.failure("Long press failed")
                    }
                }
            } else if (action.x != null && action.y != null) {
                val success = performLongPress(action.x, action.y, action.duration)
                return@withContext if (success) {
                    ActionResult.success("Long pressed at (${action.x}, ${action.y})")
                } else {
                    ActionResult.failure("Long press failed")
                }
            }
            
            return@withContext ActionResult.failure("Invalid long press parameters")
        } catch (e: Exception) {
            ActionResult.failure("Long press failed: ${e.message}", e)
        }
    }

    private suspend fun executeClear(action: Clear): ActionResult = withContext(Dispatchers.Main) {
        try {
            val node = if (action.targetId != null) {
                findNodeById(action.targetId)
            } else {
                findFocusableEditText()
            }
            
            if (node != null) {
                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,
                    Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                    })
                node.recycle()
                return@withContext ActionResult.success("Text field cleared")
            }
            
            return@withContext ActionResult.failure("No input field found")
        } catch (e: Exception) {
            ActionResult.failure("Clear failed: ${e.message}", e)
        }
    }

    private suspend fun executePaste(action: Paste): ActionResult = withContext(Dispatchers.Main) {
        try {
            val clipboardText = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
            if (clipboardText.isNullOrBlank()) {
                return@withContext ActionResult.failure("Clipboard is empty")
            }
            
            return@withContext executeType(Type(clipboardText, action.targetId))
        } catch (e: Exception) {
            ActionResult.failure("Paste failed: ${e.message}", e)
        }
    }

    private suspend fun executeSwipe(action: Swipe): ActionResult = withContext(Dispatchers.Main) {
        try {
            val path = Path().apply {
                moveTo(action.startX.toFloat(), action.startY.toFloat())
                lineTo(action.endX.toFloat(), action.endY.toFloat())
            }
            
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, action.duration))
                .build()
            
            val success = accessibilityService.dispatchGesture(gesture, null, null)
            
            return@withContext if (success) {
                ActionResult.success("Swiped from (${action.startX},${action.startY}) to (${action.endX},${action.endY})")
            } else {
                ActionResult.failure("Swipe gesture failed")
            }
        } catch (e: Exception) {
            ActionResult.failure("Swipe failed: ${e.message}", e)
        }
    }

    private suspend fun executeScroll(action: Scroll): ActionResult = withContext(Dispatchers.Main) {
        try {
            val screenHeight = context.resources.displayMetrics.heightPixels
            val screenWidth = context.resources.displayMetrics.widthPixels
            
            val swipe = when (action.direction) {
                ScrollDirection.UP -> Swipe(
                    startX = screenWidth / 2,
                    startY = (screenHeight * 0.7).toInt(),
                    endX = screenWidth / 2,
                    endY = (screenHeight * 0.3).toInt()
                )
                ScrollDirection.DOWN -> Swipe(
                    startX = screenWidth / 2,
                    startY = (screenHeight * 0.3).toInt(),
                    endX = screenWidth / 2,
                    endY = (screenHeight * 0.7).toInt()
                )
                ScrollDirection.LEFT -> Swipe(
                    startX = (screenWidth * 0.7).toInt(),
                    startY = screenHeight / 2,
                    endX = (screenWidth * 0.3).toInt(),
                    endY = screenHeight / 2
                )
                ScrollDirection.RIGHT -> Swipe(
                    startX = (screenWidth * 0.3).toInt(),
                    startY = screenHeight / 2,
                    endX = (screenWidth * 0.7).toInt(),
                    endY = screenHeight / 2
                )
            }
            
            return@withContext executeSwipe(swipe)
        } catch (e: Exception) {
            ActionResult.failure("Scroll failed: ${e.message}", e)
        }
    }

    // ========================================================================
    // Navigation Actions
    // ========================================================================

    private suspend fun executeHome(action: Home): ActionResult {
        return executeKeyEvent(KeyEvent(UIActions.KeyCodes.KEYCODE_HOME))
    }

    private suspend fun executeBack(action: Back): ActionResult {
        return executeKeyEvent(KeyEvent(UIActions.KeyCodes.KEYCODE_BACK))
    }

    private suspend fun executeLaunch(action: Launch): ActionResult = withContext(Dispatchers.Main) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(action.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return@withContext ActionResult.success("Launched app: ${action.packageName}")
            }
            return@withContext ActionResult.failure("Cannot launch app: ${action.packageName}")
        } catch (e: Exception) {
            ActionResult.failure("Launch failed: ${e.message}", e)
        }
    }

    private suspend fun executeSwitchApp(action: SwitchApp): ActionResult = withContext(Dispatchers.Main) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(action.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return@withContext ActionResult.success("Switched to app: ${action.packageName}")
            }
            return@withContext ActionResult.failure("Cannot switch to app: ${action.packageName}")
        } catch (e: Exception) {
            ActionResult.failure("Switch app failed: ${e.message}", e)
        }
    }

    private suspend fun executeOpenUrl(action: OpenUrl): ActionResult = withContext(Dispatchers.Main) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setData(android.net.Uri.parse(action.url))
                if (action.browserPackage != null) {
                    setPackage(action.browserPackage)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return@withContext ActionResult.success("Opened URL: ${action.url}")
        } catch (e: Exception) {
            ActionResult.failure("Open URL failed: ${e.message}", e)
        }
    }

    private suspend fun executeOpenSettings(action: OpenSettings): ActionResult = withContext(Dispatchers.Main) {
        try {
            val intent = if (action.settingsPage != null) {
                Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
            return@withContext ActionResult.success("Opened settings")
        } catch (e: Exception) {
            ActionResult.failure("Open settings failed: ${e.message}", e)
        }
    }

    private suspend fun executeNotifications(action: Notifications): ActionResult = withContext(Dispatchers.Main) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                return@withContext ActionResult.success("Opened notifications")
            }
            return@withContext ActionResult.failure("Notifications not supported on this Android version")
        } catch (e: Exception) {
            ActionResult.failure("Open notifications failed: ${e.message}", e)
        }
    }

    // ========================================================================
    // Clipboard Actions
    // ========================================================================

    private suspend fun executeClipboardGet(action: ClipboardGet): ActionResult {
        try {
            val text = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
            return if (text != null) {
                ActionResult.success("Clipboard content retrieved", data = mapOf("text" to text))
            } else {
                ActionResult.failure("Clipboard is empty")
            }
        } catch (e: Exception) {
            return ActionResult.failure("Clipboard get failed: ${e.message}", e)
        }
    }

    private suspend fun executeClipboardSet(action: ClipboardSet): ActionResult {
        try {
            val clip = ClipData.newPlainText("EgyptianAgent", action.text)
            clipboardManager.setPrimaryClip(clip)
            return ActionResult.success("Text copied to clipboard")
        } catch (e: Exception) {
            return ActionResult.failure("Clipboard set failed: ${e.message}", e)
        }
    }

    // ========================================================================
    // Multi-step Skills
    // ========================================================================

    private suspend fun executeReadScreen(action: ReadScreen): ActionResult = withContext(Dispatchers.Main) {
        try {
            val allText = StringBuilder()
            var scrollsPerformed = 0
            
            repeat(action.maxScrolls) { scrollIndex ->
                val tree = treeParser.parseCurrentTree()
                val textElements = tree.elements.filter { !it.text.isNullOrBlank() }
                
                textElements.forEach { element ->
                    element.text?.let { allText.appendLine(it) }
                }
                
                // Try to scroll down
                val scrollResult = executeScroll(Scroll(ScrollDirection.DOWN))
                if (!scrollResult.screenChanged) {
                    // No more content to scroll
                    return@repeat
                }
                scrollsPerformed++
                
                Thread.sleep(300) // Wait for scroll animation
            }
            
            return@withContext ActionResult.success(
                "Read screen content (${scrollsPerformed + 1} pages)",
                data = mapOf("text" to allText.toString(), "scrolls" to scrollsPerformed)
            )
        } catch (e: Exception) {
            ActionResult.failure("Read screen failed: ${e.message}", e)
        }
    }

    private suspend fun executeSubmitMessage(action: SubmitMessage): ActionResult {
        // Type message
        val typeResult = executeType(Type(action.message, action.inputId))
        if (!typeResult.success) return typeResult
        
        // Find and tap send button
        if (action.sendButtonId != null) {
            return executeTap(Tap(elementId = action.sendButtonId))
        } else {
            // Try common send button identifiers
            val sendButtonIds = listOf("send", "compose_send", "ib_send", "btn_send")
            for (id in sendButtonIds) {
                val result = executeTap(Tap(elementId = id, descriptionText = "Tap send button"))
                if (result.success) return result
            }
            
            // Fallback: look for send button by text/description
            return executeFindAndTap(FindAndTap("Send", descriptionText = "Find and tap send"))
        }
    }

    private suspend fun executeCopyVisibleText(action: CopyVisibleText): ActionResult {
        val readResult = executeReadScreen(ReadScreen(maxScrolls = 1))
        if (!readResult.success) return readResult
        
        val text = readResult.data["text"] as? String ?: ""
        return executeClipboardSet(ClipboardSet(text))
    }

    private suspend fun executeWaitForContent(action: WaitForContent): ActionResult = withContext(Dispatchers.Main) {
        try {
            val startTime = System.currentTimeMillis()
            
            while (System.currentTimeMillis() - startTime < action.timeoutMs) {
                val tree = treeParser.parseCurrentTree()
                val found = tree.elements.any { it.matchesSearch(action.searchText) }
                
                if (found) {
                    return@withContext ActionResult.success("Found content: ${action.searchText}")
                }
                
                Thread.sleep(action.pollIntervalMs)
            }
            
            return@withContext ActionResult.failure("Content not found within timeout: ${action.searchText}")
        } catch (e: Exception) {
            ActionResult.failure("Wait for content failed: ${e.message}", e)
        }
    }

    private suspend fun executeFindAndTap(action: FindAndTap): ActionResult = withContext(Dispatchers.Main) {
        try {
            val startTime = System.currentTimeMillis()
            
            while (System.currentTimeMillis() - startTime < action.timeoutMs) {
                val element = treeParser.findClickableElementByText(action.searchText)
                if (element != null) {
                    return@withContext executeTap(Tap(elementId = element.id))
                }
                
                Thread.sleep(500)
            }
            
            return@withContext ActionResult.failure("Element not found: ${action.searchText}")
        } catch (e: Exception) {
            ActionResult.failure("Find and tap failed: ${e.message}", e)
        }
    }

    private suspend fun executeComposeEmail(action: ComposeEmail): ActionResult {
        // This is a complex multi-step action
        // 1. Launch email app
        val launchResult = executeLaunch(Launch("com.google.android.gm"))
        if (!launchResult.success) {
            // Try alternative email apps
            executeLaunch(Launch("com.samsung.android.email.provider"))
        }
        
        Thread.sleep(1000)
        
        // 2. Find and tap compose button
        val composeResult = executeFindAndTap(FindAndTap("Compose", descriptionText = "Tap compose"))
        if (!composeResult.success) {
            return ActionResult.failure("Cannot open compose window")
        }
        
        Thread.sleep(500)
        
        // 3. Fill To field
        executeType(Type(action.to, descriptionText = "Fill To field"))
        Thread.sleep(300)

        // 4. Find and tap Subject field
        executeFindAndTap(FindAndTap("Subject", descriptionText = "Focus subject"))
        Thread.sleep(300)

        // 5. Fill Subject
        executeType(Type(action.subject, descriptionText = "Fill subject"))
        Thread.sleep(300)

        // 6. Fill Body
        executeType(Type(action.body, descriptionText = "Fill body"))
        
        return ActionResult.success("Email composed (ready to send)")
    }

    // ========================================================================
    // System Actions
    // ========================================================================

    private suspend fun executeScreenshot(action: Screenshot): ActionResult = withContext(Dispatchers.Main) {
        try {
            val screenshotPath = action.outputPath ?:
                "${context.cacheDir.absolutePath}/screenshot_${System.currentTimeMillis()}.png"

            // Note: Actual screenshot capture requires MediaProjection API
            // This is a stub that prepares the path for future implementation
            return@withContext ActionResult.success("Screenshot path prepared: $screenshotPath",
                data = mapOf("path" to screenshotPath))
        } catch (e: Exception) {
            return@withContext ActionResult.failure("Screenshot failed: ${e.message}", e)
        }
    }

    private suspend fun executeShell(action: Shell): ActionResult {
        // Shell commands require root or ADB
        return ActionResult.failure("Shell commands require root access")
    }

    private suspend fun executeKeyEvent(action: KeyEvent): ActionResult = withContext(Dispatchers.Main) {
        try {
            // Use root input for key events
            val process = Runtime.getRuntime().exec("su")
            process.outputStream.write("input keyevent ${action.keyCode}\n".toByteArray())
            process.outputStream.write("exit\n".toByteArray())
            process.outputStream.flush()
            process.waitFor()
            
            return@withContext ActionResult.success("Key event sent: ${action.keyCode}")
        } catch (e: Exception) {
            // Fallback: try without root
            try {
                val process = Runtime.getRuntime().exec("input keyevent ${action.keyCode}")
                process.waitFor()
                return@withContext ActionResult.success("Key event sent: ${action.keyCode}")
            } catch (e2: Exception) {
                ActionResult.failure("Key event failed: ${e.message}", e)
            }
        }
    }

    private suspend fun executePullFile(action: PullFile): ActionResult {
        return ActionResult.failure("Pull file requires ADB connection")
    }

    private suspend fun executePushFile(action: PushFile): ActionResult {
        return ActionResult.failure("Push file requires ADB connection")
    }

    private suspend fun executeWait(action: Wait): ActionResult {
        Thread.sleep(action.durationMs)
        return ActionResult.success("Waited for ${action.durationMs}ms")
    }

    private suspend fun executeDone(action: Done): ActionResult {
        return ActionResult.success(
            action.message,
            data = mapOf("success" to action.success)
        )
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private fun findNodeById(nodeId: String): AccessibilityNodeInfo? {
        val rootNode = accessibilityService.rootInActiveWindow ?: return null
        
        // Try to find by view ID
        val nodes = rootNode.findAccessibilityNodeInfosByViewId(nodeId)
        if (nodes.isNotEmpty()) {
            return nodes.first()
        }
        
        // Try to find by text match
        val textNodes = rootNode.findAccessibilityNodeInfosByText(nodeId)
        if (textNodes.isNotEmpty()) {
            return textNodes.first { it.isClickable }
        }
        
        return null
    }

    private fun findFocusableEditText(): AccessibilityNodeInfo? {
        val rootNode = accessibilityService.rootInActiveWindow ?: return null
        
        // Search for editable text fields
        return findNodeByClass(rootNode, "android.widget.EditText")
    }

    private fun findNodeByClass(node: AccessibilityNodeInfo, className: String): AccessibilityNodeInfo? {
        if (node.className?.toString()?.contains(className, ignoreCase = true) == true) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                try {
                    val result = findNodeByClass(child, className)
                    if (result != null) return result
                } finally {
                    child.recycle()
                }
            }
        }
        
        return null
    }

    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        return if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            // Try parent
            node.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
        }
    }

    private suspend fun performTap(x: Int, y: Int): Boolean = withContext(Dispatchers.Main) {
        try {
            val path = Path().apply {
                moveTo(x.toFloat(), y.toFloat())
            }
            
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, DEFAULT_TAP_DURATION))
                .build()
            
            return@withContext accessibilityService.dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Tap gesture failed", e)
            false
        }
    }

    private suspend fun performLongPress(node: AccessibilityNodeInfo, duration: Long): Boolean = withContext(Dispatchers.Main) {
        try {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            return@withContext performLongPress(rect.centerX(), rect.centerY(), duration)
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun performLongPress(x: Int, y: Int, duration: Long): Boolean = withContext(Dispatchers.Main) {
        try {
            val path = Path().apply {
                moveTo(x.toFloat(), y.toFloat())
            }
            
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                .build()
            
            return@withContext accessibilityService.dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Long press gesture failed", e)
            false
        }
    }

    // ========================================================================
    // State Management
    // ========================================================================

    /**
     * Get action history.
     */
    fun getActionHistory(): List<Pair<UIAction, ActionResult>> = actionHistory.toList()

    /**
     * Clear action history.
     */
    fun clearHistory() {
        actionHistory.clear()
    }

    /**
     * Get last action result.
     */
    fun getLastResult(): ActionResult? = actionHistory.lastOrNull()?.second

    /**
     * Check if engine is ready.
     */
    fun isReady(): Boolean = true

    /**
     * Clean up resources.
     */
    fun destroy() {
        clearHistory()
        treeParser.clearCache()
    }
}
