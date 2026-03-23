package com.egyptian.agent.navigation

/**
 * UI Actions for EgyptianAgent Navigation Engine.
 * Adapted from DroidClaw's actions.ts (28 actions).
 *
 * Categories:
 * - Basic Interactions (8): tap, type, enter, longPress, clear, paste, swipe, scroll
 * - Navigation (7): home, back, launch, switchApp, openUrl, openSettings, notifications
 * - Clipboard (2): clipboardGet, clipboardSet
 * - Multi-step Skills (6): readScreen, submitMessage, copyVisibleText, waitForContent, findAndTap, composeEmail
 * - System (5): screenshot, shell, keyEvent, pullFile, pushFile, wait, done
 *
 * @author EgyptianAgent Team
 * @version 1.0.0
 */

/**
 * Base class for all UI actions.
 */
sealed class UIAction {
    abstract val name: String
    abstract val description: String
}

// ============================================================================
// Basic Interactions (8 actions)
// ============================================================================

/**
 * Tap on an element at specified coordinates or by element ID.
 */
data class Tap(
    val elementId: String? = null,
    val x: Int? = null,
    val y: Int? = null,
    val descriptionText: String = "Tap on element"
) : UIAction() {
    override val name = "tap"
    override val description = descriptionText

    init {
        require(elementId != null || (x != null && y != null)) {
            "Either elementId or (x, y) coordinates must be provided"
        }
    }
}

/**
 * Type text into an input field.
 */
data class Type(
    val text: String,
    val targetId: String? = null,
    val clearFirst: Boolean = true,
    val descriptionText: String = "Type text"
) : UIAction() {
    override val name = "type"
    override val description = descriptionText
}

/**
 * Press Enter/Return key.
 */
data class Enter(
    val targetId: String? = null,
    val descriptionText: String = "Press Enter"
) : UIAction() {
    override val name = "enter"
    override val description = descriptionText
}

/**
 * Long press on an element.
 */
data class LongPress(
    val elementId: String? = null,
    val x: Int? = null,
    val y: Int? = null,
    val duration: Long = 1000L,
    val descriptionText: String = "Long press"
) : UIAction() {
    override val name = "longPress"
    override val description = descriptionText
}

/**
 * Clear text from an input field.
 */
data class Clear(
    val targetId: String? = null,
    val descriptionText: String = "Clear text field"
) : UIAction() {
    override val name = "clear"
    override val description = descriptionText
}

/**
 * Paste content from clipboard.
 */
data class Paste(
    val targetId: String? = null,
    val descriptionText: String = "Paste from clipboard"
) : UIAction() {
    override val name = "paste"
    override val description = descriptionText
}

/**
 * Swipe gesture from one point to another.
 */
data class Swipe(
    val startX: Int,
    val startY: Int,
    val endX: Int,
    val endY: Int,
    val duration: Long = 300L,
    val descriptionText: String = "Swipe gesture"
) : UIAction() {
    override val name = "swipe"
    override val description = descriptionText
}

/**
 * Scroll in a specific direction.
 */
data class Scroll(
    val direction: ScrollDirection,
    val targetId: String? = null,
    val descriptionText: String = "Scroll"
) : UIAction() {
    override val name = "scroll"
    override val description = "$descriptionText ${direction.name.lowercase()}"
}

enum class ScrollDirection {
    UP, DOWN, LEFT, RIGHT
}

// ============================================================================
// Navigation (7 actions)
// ============================================================================

/**
 * Go to home screen.
 */
data class Home(
    val descriptionText: String = "Go to home screen"
) : UIAction() {
    override val name = "home"
    override val description = descriptionText
}

/**
 * Go back (simulate back button).
 */
data class Back(
    val descriptionText: String = "Go back"
) : UIAction() {
    override val name = "back"
    override val description = descriptionText
}

/**
 * Launch an application by package name.
 */
data class Launch(
    val packageName: String,
    val activityName: String? = null,
    val descriptionText: String = "Launch app"
) : UIAction() {
    override val name = "launch"
    override val description = "$descriptionText: $packageName"
}

/**
 * Switch to a different app.
 */
data class SwitchApp(
    val packageName: String,
    val descriptionText: String = "Switch to app"
) : UIAction() {
    override val name = "switchApp"
    override val description = "$descriptionText: $packageName"
}

/**
 * Open a URL in browser.
 */
data class OpenUrl(
    val url: String,
    val browserPackage: String? = null,
    val descriptionText: String = "Open URL"
) : UIAction() {
    override val name = "openUrl"
    override val description = "$descriptionText: $url"
}

/**
 * Open device settings.
 */
data class OpenSettings(
    val settingsPage: String? = null,
    val descriptionText: String = "Open settings"
) : UIAction() {
    override val name = "openSettings"
    override val description = descriptionText
}

/**
 * Open notifications panel.
 */
data class Notifications(
    val descriptionText: String = "Open notifications"
) : UIAction() {
    override val name = "notifications"
    override val description = descriptionText
}

// ============================================================================
// Clipboard (2 actions)
// ============================================================================

/**
 * Get clipboard content.
 */
data class ClipboardGet(
    val descriptionText: String = "Get clipboard content"
) : UIAction() {
    override val name = "clipboardGet"
    override val description = descriptionText
}

/**
 * Set clipboard content.
 */
data class ClipboardSet(
    val text: String,
    val descriptionText: String = "Set clipboard content"
) : UIAction() {
    override val name = "clipboardSet"
    override val description = descriptionText
}

// ============================================================================
// Multi-step Skills (6 actions)
// ============================================================================

/**
 * Read all visible text on screen (auto-scrolls if needed).
 */
data class ReadScreen(
    val maxScrolls: Int = 5,
    val descriptionText: String = "Read entire screen content"
) : UIAction() {
    override val name = "readScreen"
    override val description = descriptionText
}

/**
 * Submit a message (finds input field, types, and taps send).
 */
data class SubmitMessage(
    val message: String,
    val inputId: String? = null,
    val sendButtonId: String? = null,
    val descriptionText: String = "Submit message"
) : UIAction() {
    override val name = "submitMessage"
    override val description = descriptionText
}

/**
 * Copy all visible text to clipboard.
 */
data class CopyVisibleText(
    val descriptionText: String = "Copy visible text to clipboard"
) : UIAction() {
    override val name = "copyVisibleText"
    override val description = descriptionText
}

/**
 * Wait for specific content to appear.
 */
data class WaitForContent(
    val searchText: String,
    val timeoutMs: Long = 10000L,
    val pollIntervalMs: Long = 500L,
    val descriptionText: String = "Wait for"
) : UIAction() {
    override val name = "waitForContent"
    override val description = "$descriptionText: $searchText"
}

/**
 * Find an element by text and tap it.
 */
data class FindAndTap(
    val searchText: String,
    val timeoutMs: Long = 5000L,
    val descriptionText: String = "Find and tap"
) : UIAction() {
    override val name = "findAndTap"
    override val description = "$descriptionText: $searchText"
}

/**
 * Compose an email (fills To, Subject, Body fields).
 */
data class ComposeEmail(
    val to: String,
    val subject: String,
    val body: String,
    val descriptionText: String = "Compose email"
) : UIAction() {
    override val name = "composeEmail"
    override val description = descriptionText
}

// ============================================================================
// System (5 actions)
// ============================================================================

/**
 * Take a screenshot.
 */
data class Screenshot(
    val outputPath: String? = null,
    val descriptionText: String = "Take screenshot"
) : UIAction() {
    override val name = "screenshot"
    override val description = descriptionText
}

/**
 * Execute a shell command (requires root/ADB).
 */
data class Shell(
    val command: String,
    val descriptionText: String = "Execute shell command"
) : UIAction() {
    override val name = "shell"
    override val description = descriptionText
}

/**
 * Send a key event.
 */
data class KeyEvent(
    val keyCode: Int,
    val descriptionText: String = "Send key event"
) : UIAction() {
    override val name = "keyEvent"
    override val description = "$descriptionText: $keyCode"
}

/**
 * Pull a file from device.
 */
data class PullFile(
    val devicePath: String,
    val localPath: String,
    val descriptionText: String = "Pull file from device"
) : UIAction() {
    override val name = "pullFile"
    override val description = descriptionText
}

/**
 * Push a file to device.
 */
data class PushFile(
    val localPath: String,
    val devicePath: String,
    val descriptionText: String = "Push file to device"
) : UIAction() {
    override val name = "pushFile"
    override val description = descriptionText
}

/**
 * Wait for a specified duration.
 */
data class Wait(
    val durationMs: Long,
    val descriptionText: String = "Wait"
) : UIAction() {
    override val name = "wait"
    override val description = "$descriptionText for ${durationMs}ms"
}

/**
 * Mark task as done with result message.
 */
data class Done(
    val message: String,
    val success: Boolean = true,
    val descriptionText: String = "Task completed"
) : UIAction() {
    override val name = "done"
    override val description = descriptionText
}

// ============================================================================
// Action Result
// ============================================================================

/**
 * Result of executing a UI action.
 */
data class ActionResult(
    val success: Boolean,
    val message: String,
    val screenChanged: Boolean = false,
    val data: Map<String, Any?> = emptyMap(),
    val error: Throwable? = null,
    val executionTimeMs: Long = 0L
) {
    companion object {
        fun success(message: String, screenChanged: Boolean = false, data: Map<String, Any?> = emptyMap()): ActionResult {
            return ActionResult(success = true, message = message, screenChanged = screenChanged, data = data)
        }

        fun failure(message: String, error: Throwable? = null): ActionResult {
            return ActionResult(success = false, message = message, error = error)
        }
    }
}

/**
 * Companion object with action registry and utilities.
 */
object UIActions {
    /**
     * All available action types.
     */
    val ALL_ACTIONS = listOf(
        // Basic Interactions
        "tap", "type", "enter", "longPress", "clear", "paste", "swipe", "scroll",
        // Navigation
        "home", "back", "launch", "switchApp", "openUrl", "openSettings", "notifications",
        // Clipboard
        "clipboardGet", "clipboardSet",
        // Multi-step Skills
        "readScreen", "submitMessage", "copyVisibleText", "waitForContent", "findAndTap", "composeEmail",
        // System
        "screenshot", "shell", "keyEvent", "pullFile", "pushFile", "wait", "done"
    )

    /**
     * Common Android key codes.
     */
    object KeyCodes {
        const val KEYCODE_HOME = 3
        const val KEYCODE_BACK = 4
        const val KEYCODE_ENTER = 66
        const val KEYCODE_TAB = 61
        const val KEYCODE_SPACE = 62
        const val KEYCODE_DEL = 67
        const val KEYCODE_FORWARD_DEL = 112
        const val KEYCODE_MOVE_HOME = 122
        const val KEYCODE_MOVE_END = 123
        const val KEYCODE_DPAD_UP = 19
        const val KEYCODE_DPAD_DOWN = 20
        const val KEYCODE_DPAD_LEFT = 21
        const val KEYCODE_DPAD_RIGHT = 22
        const val KEYCODE_DPAD_CENTER = 23
        const val KEYCODE_VOLUME_UP = 24
        const val KEYCODE_VOLUME_DOWN = 25
        const val KEYCODE_POWER = 26
        const val KEYCODE_MENU = 82
    }

    /**
     * Common swipe coordinates for different screen sizes.
     */
    object SwipePresets {
        // Scroll down (middle of screen)
        fun scrollDown(screenWidth: Int, screenHeight: Int): Swipe = Swipe(
            startX = screenWidth / 2,
            startY = (screenHeight * 0.7).toInt(),
            endX = screenWidth / 2,
            endY = (screenHeight * 0.3).toInt()
        )

        // Scroll up (middle of screen)
        fun scrollUp(screenWidth: Int, screenHeight: Int): Swipe = Swipe(
            startX = screenWidth / 2,
            startY = (screenHeight * 0.3).toInt(),
            endX = screenWidth / 2,
            endY = (screenHeight * 0.7).toInt()
        )
    }
}
