package com.egyptian.agent.navigation

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import com.google.common.truth.Truth.assertThat

/**
 * Comprehensive unit tests for UIActions (28 actions).
 * 
 * Target: >85% code coverage
 * 
 * @author EgyptianAgent Team
 * @version 1.0.0
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UIActionsTest {

    // ========================================================================
    // Basic Interactions Tests (8 actions)
    // ========================================================================

    @Test
    fun `Tap action with elementId creates valid action`() {
        // Given
        val elementId = "send_button"

        // When
        val action = Tap(elementId = elementId, descriptionText = "Tap send button")

        // Then
        assertThat(action.name).isEqualTo("tap")
        assertThat(action.elementId).isEqualTo(elementId)
        assertThat(action.x).isNull()
        assertThat(action.y).isNull()
        assertThat(action.description).contains("send")
    }

    @Test
    fun `Tap action with coordinates creates valid action`() {
        // Given
        val x = 500
        val y = 300

        // When
        val action = Tap(x = x, y = y, descriptionText = "Tap at coordinates")

        // Then
        assertThat(action.name).isEqualTo("tap")
        assertThat(action.x).isEqualTo(x)
        assertThat(action.y).isEqualTo(y)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Tap action throws exception when no elementId or coordinates provided`() {
        // When
        Tap(descriptionText = "Invalid tap")
    }

    @Test
    fun `Type action creates valid action`() {
        // Given
        val text = "Hello, World!"
        val targetId = "message_input"

        // When
        val action = Type(text = text, targetId = targetId, clearFirst = true)

        // Then
        assertThat(action.name).isEqualTo("type")
        assertThat(action.text).isEqualTo(text)
        assertThat(action.targetId).isEqualTo(targetId)
        assertThat(action.clearFirst).isTrue()
    }

    @Test
    fun `Type action with default clearFirst`() {
        // When
        val action = Type(text = "Test")

        // Then
        assertThat(action.clearFirst).isTrue()
    }

    @Test
    fun `Enter action creates valid action`() {
        // Given
        val targetId = "search_input"

        // When
        val action = Enter(targetId = targetId)

        // Then
        assertThat(action.name).isEqualTo("enter")
        assertThat(action.targetId).isEqualTo(targetId)
    }

    @Test
    fun `LongPress action with elementId creates valid action`() {
        // Given
        val elementId = "menu_item"
        val duration = 2000L

        // When
        val action = LongPress(elementId = elementId, duration = duration)

        // Then
        assertThat(action.name).isEqualTo("longPress")
        assertThat(action.elementId).isEqualTo(elementId)
        assertThat(action.duration).isEqualTo(duration)
    }

    @Test
    fun `LongPress action with coordinates creates valid action`() {
        // Given
        val x = 100
        val y = 200

        // When
        val action = LongPress(x = x, y = y, duration = 1500L)

        // Then
        assertThat(action.name).isEqualTo("longPress")
        assertThat(action.x).isEqualTo(x)
        assertThat(action.y).isEqualTo(y)
    }

    @Test
    fun `Clear action creates valid action`() {
        // Given
        val targetId = "input_field"

        // When
        val action = Clear(targetId = targetId)

        // Then
        assertThat(action.name).isEqualTo("clear")
        assertThat(action.targetId).isEqualTo(targetId)
    }

    @Test
    fun `Paste action creates valid action`() {
        // Given
        val targetId = "message_box"

        // When
        val action = Paste(targetId = targetId)

        // Then
        assertThat(action.name).isEqualTo("paste")
        assertThat(action.targetId).isEqualTo(targetId)
    }

    @Test
    fun `Swipe action creates valid action`() {
        // Given
        val startX = 500
        val startY = 700
        val endX = 500
        val endY = 300

        // When
        val action = Swipe(startX = startX, startY = startY, endX = endX, endY = endY)

        // Then
        assertThat(action.name).isEqualTo("swipe")
        assertThat(action.startX).isEqualTo(startX)
        assertThat(action.startY).isEqualTo(startY)
        assertThat(action.endX).isEqualTo(endX)
        assertThat(action.endY).isEqualTo(endY)
        assertThat(action.duration).isEqualTo(300L)
    }

    @Test
    fun `Scroll action with DOWN direction creates valid action`() {
        // When
        val action = Scroll(direction = ScrollDirection.DOWN)

        // Then
        assertThat(action.name).isEqualTo("scroll")
        assertThat(action.direction).isEqualTo(ScrollDirection.DOWN)
        assertThat(action.description).contains("down")
    }

    @Test
    fun `Scroll action with all directions`() {
        // Test all scroll directions
        val directions = ScrollDirection.values()

        for (direction in directions) {
            val action = Scroll(direction = direction)
            assertThat(action.name).isEqualTo("scroll")
            assertThat(action.direction).isEqualTo(direction)
        }
    }

    // ========================================================================
    // Navigation Actions Tests (7 actions)
    // ========================================================================

    @Test
    fun `Home action creates valid action`() {
        // When
        val action = Home()

        // Then
        assertThat(action.name).isEqualTo("home")
        assertThat(action.description).contains("home")
    }

    @Test
    fun `Back action creates valid action`() {
        // When
        val action = Back()

        // Then
        assertThat(action.name).isEqualTo("back")
        assertThat(action.description).contains("back")
    }

    @Test
    fun `Launch action with package name creates valid action`() {
        // Given
        val packageName = "com.whatsapp"

        // When
        val action = Launch(packageName = packageName)

        // Then
        assertThat(action.name).isEqualTo("launch")
        assertThat(action.packageName).isEqualTo(packageName)
        assertThat(action.description).contains(packageName)
    }

    @Test
    fun `Launch action with activity name creates valid action`() {
        // Given
        val packageName = "com.example.app"
        val activityName = ".MainActivity"

        // When
        val action = Launch(packageName = packageName, activityName = activityName)

        // Then
        assertThat(action.name).isEqualTo("launch")
        assertThat(action.packageName).isEqualTo(packageName)
        assertThat(action.activityName).isEqualTo(activityName)
    }

    @Test
    fun `SwitchApp action creates valid action`() {
        // Given
        val packageName = "com.facebook.katana"

        // When
        val action = SwitchApp(packageName = packageName)

        // Then
        assertThat(action.name).isEqualTo("switchApp")
        assertThat(action.packageName).isEqualTo(packageName)
    }

    @Test
    fun `OpenUrl action creates valid action`() {
        // Given
        val url = "https://www.google.com"

        // When
        val action = OpenUrl(url = url)

        // Then
        assertThat(action.name).isEqualTo("openUrl")
        assertThat(action.url).isEqualTo(url)
        assertThat(action.description).contains(url)
    }

    @Test
    fun `OpenUrl action with browser package creates valid action`() {
        // Given
        val url = "https://www.example.com"
        val browserPackage = "com.android.chrome"

        // When
        val action = OpenUrl(url = url, browserPackage = browserPackage)

        // Then
        assertThat(action.name).isEqualTo("openUrl")
        assertThat(action.url).isEqualTo(url)
        assertThat(action.browserPackage).isEqualTo(browserPackage)
    }

    @Test
    fun `OpenSettings action creates valid action`() {
        // When
        val action = OpenSettings()

        // Then
        assertThat(action.name).isEqualTo("openSettings")
    }

    @Test
    fun `OpenSettings action with specific page creates valid action`() {
        // Given
        val settingsPage = "wifi_settings"

        // When
        val action = OpenSettings(settingsPage = settingsPage)

        // Then
        assertThat(action.name).isEqualTo("openSettings")
        assertThat(action.settingsPage).isEqualTo(settingsPage)
    }

    @Test
    fun `Notifications action creates valid action`() {
        // When
        val action = Notifications()

        // Then
        assertThat(action.name).isEqualTo("notifications")
    }

    // ========================================================================
    // Clipboard Actions Tests (2 actions)
    // ========================================================================

    @Test
    fun `ClipboardGet action creates valid action`() {
        // When
        val action = ClipboardGet()

        // Then
        assertThat(action.name).isEqualTo("clipboardGet")
    }

    @Test
    fun `ClipboardSet action creates valid action`() {
        // Given
        val text = "Hello, Clipboard!"

        // When
        val action = ClipboardSet(text = text)

        // Then
        assertThat(action.name).isEqualTo("clipboardSet")
        assertThat(action.text).isEqualTo(text)
    }

    // ========================================================================
    // Multi-step Skills Tests (6 actions)
    // ========================================================================

    @Test
    fun `ReadScreen action creates valid action`() {
        // When
        val action = ReadScreen()

        // Then
        assertThat(action.name).isEqualTo("readScreen")
        assertThat(action.maxScrolls).isEqualTo(5)
    }

    @Test
    fun `ReadScreen action with custom maxScrolls creates valid action`() {
        // Given
        val maxScrolls = 10

        // When
        val action = ReadScreen(maxScrolls = maxScrolls)

        // Then
        assertThat(action.name).isEqualTo("readScreen")
        assertThat(action.maxScrolls).isEqualTo(maxScrolls)
    }

    @Test
    fun `SubmitMessage action creates valid action`() {
        // Given
        val message = "Hello, World!"

        // When
        val action = SubmitMessage(message = message)

        // Then
        assertThat(action.name).isEqualTo("submitMessage")
        assertThat(action.message).isEqualTo(message)
    }

    @Test
    fun `SubmitMessage action with custom input and send button IDs creates valid action`() {
        // Given
        val message = "Test message"
        val inputId = "message_input"
        val sendButtonId = "send_button"

        // When
        val action = SubmitMessage(message = message, inputId = inputId, sendButtonId = sendButtonId)

        // Then
        assertThat(action.name).isEqualTo("submitMessage")
        assertThat(action.message).isEqualTo(message)
        assertThat(action.inputId).isEqualTo(inputId)
        assertThat(action.sendButtonId).isEqualTo(sendButtonId)
    }

    @Test
    fun `CopyVisibleText action creates valid action`() {
        // When
        val action = CopyVisibleText()

        // Then
        assertThat(action.name).isEqualTo("copyVisibleText")
    }

    @Test
    fun `WaitForContent action creates valid action`() {
        // Given
        val searchText = "Loading complete"

        // When
        val action = WaitForContent(searchText = searchText)

        // Then
        assertThat(action.name).isEqualTo("waitForContent")
        assertThat(action.searchText).isEqualTo(searchText)
        assertThat(action.timeoutMs).isEqualTo(10000L)
        assertThat(action.pollIntervalMs).isEqualTo(500L)
    }

    @Test
    fun `WaitForContent action with custom timeout creates valid action`() {
        // Given
        val searchText = "Content loaded"
        val timeoutMs = 20000L
        val pollIntervalMs = 1000L

        // When
        val action = WaitForContent(searchText = searchText, timeoutMs = timeoutMs, pollIntervalMs = pollIntervalMs)

        // Then
        assertThat(action.name).isEqualTo("waitForContent")
        assertThat(action.searchText).isEqualTo(searchText)
        assertThat(action.timeoutMs).isEqualTo(timeoutMs)
        assertThat(action.pollIntervalMs).isEqualTo(pollIntervalMs)
    }

    @Test
    fun `FindAndTap action creates valid action`() {
        // Given
        val searchText = "Submit"

        // When
        val action = FindAndTap(searchText = searchText)

        // Then
        assertThat(action.name).isEqualTo("findAndTap")
        assertThat(action.searchText).isEqualTo(searchText)
        assertThat(action.timeoutMs).isEqualTo(5000L)
    }

    @Test
    fun `FindAndTap action with custom timeout creates valid action`() {
        // Given
        val searchText = "Next"
        val timeoutMs = 10000L

        // When
        val action = FindAndTap(searchText = searchText, timeoutMs = timeoutMs)

        // Then
        assertThat(action.name).isEqualTo("findAndTap")
        assertThat(action.searchText).isEqualTo(searchText)
        assertThat(action.timeoutMs).isEqualTo(timeoutMs)
    }

    @Test
    fun `ComposeEmail action creates valid action`() {
        // Given
        val to = "test@example.com"
        val subject = "Test Subject"
        val body = "Test Body"

        // When
        val action = ComposeEmail(to = to, subject = subject, body = body)

        // Then
        assertThat(action.name).isEqualTo("composeEmail")
        assertThat(action.to).isEqualTo(to)
        assertThat(action.subject).isEqualTo(subject)
        assertThat(action.body).isEqualTo(body)
    }

    // ========================================================================
    // System Actions Tests (5 actions)
    // ========================================================================

    @Test
    fun `Screenshot action creates valid action`() {
        // When
        val action = Screenshot()

        // Then
        assertThat(action.name).isEqualTo("screenshot")
        assertThat(action.outputPath).isNull()
    }

    @Test
    fun `Screenshot action with custom path creates valid action`() {
        // Given
        val outputPath = "/sdcard/screenshots/test.png"

        // When
        val action = Screenshot(outputPath = outputPath)

        // Then
        assertThat(action.name).isEqualTo("screenshot")
        assertThat(action.outputPath).isEqualTo(outputPath)
    }

    @Test
    fun `Shell action creates valid action`() {
        // Given
        val command = "ls -la"

        // When
        val action = Shell(command = command)

        // Then
        assertThat(action.name).isEqualTo("shell")
        assertThat(action.command).isEqualTo(command)
    }

    @Test
    fun `KeyEvent action creates valid action`() {
        // Given
        val keyCode = UIActions.KeyCodes.KEYCODE_HOME

        // When
        val action = KeyEvent(keyCode = keyCode)

        // Then
        assertThat(action.name).isEqualTo("keyEvent")
        assertThat(action.keyCode).isEqualTo(keyCode)
    }

    @Test
    fun `PullFile action creates valid action`() {
        // Given
        val devicePath = "/sdcard/file.txt"
        val localPath = "/tmp/file.txt"

        // When
        val action = PullFile(devicePath = devicePath, localPath = localPath)

        // Then
        assertThat(action.name).isEqualTo("pullFile")
        assertThat(action.devicePath).isEqualTo(devicePath)
        assertThat(action.localPath).isEqualTo(localPath)
    }

    @Test
    fun `PushFile action creates valid action`() {
        // Given
        val localPath = "/tmp/file.txt"
        val devicePath = "/sdcard/file.txt"

        // When
        val action = PushFile(localPath = localPath, devicePath = devicePath)

        // Then
        assertThat(action.name).isEqualTo("pushFile")
        assertThat(action.localPath).isEqualTo(localPath)
        assertThat(action.devicePath).isEqualTo(devicePath)
    }

    @Test
    fun `Wait action creates valid action`() {
        // Given
        val durationMs = 2000L

        // When
        val action = Wait(durationMs = durationMs)

        // Then
        assertThat(action.name).isEqualTo("wait")
        assertThat(action.durationMs).isEqualTo(durationMs)
        assertThat(action.description).contains("2000")
    }

    @Test
    fun `Done action with success creates valid action`() {
        // Given
        val message = "Task completed successfully"

        // When
        val action = Done(message = message, success = true)

        // Then
        assertThat(action.name).isEqualTo("done")
        assertThat(action.message).isEqualTo(message)
        assertThat(action.success).isTrue()
    }

    @Test
    fun `Done action with failure creates valid action`() {
        // Given
        val message = "Task failed"

        // When
        val action = Done(message = message, success = false)

        // Then
        assertThat(action.name).isEqualTo("done")
        assertThat(action.message).isEqualTo(message)
        assertThat(action.success).isFalse()
    }

    // ========================================================================
    // ActionResult Tests
    // ========================================================================

    @Test
    fun `ActionResult success factory creates valid result`() {
        // Given
        val message = "Action completed successfully"

        // When
        val result = ActionResult.success(message)

        // Then
        assertThat(result.success).isTrue()
        assertThat(result.message).isEqualTo(message)
        assertThat(result.screenChanged).isFalse()
        assertThat(result.error).isNull()
    }

    @Test
    fun `ActionResult success with screen changed creates valid result`() {
        // Given
        val message = "Screen changed"

        // When
        val result = ActionResult.success(message, screenChanged = true)

        // Then
        assertThat(result.success).isTrue()
        assertThat(result.message).isEqualTo(message)
        assertThat(result.screenChanged).isTrue()
    }

    @Test
    fun `ActionResult success with data creates valid result`() {
        // Given
        val message = "Data retrieved"
        val data = mapOf("key" to "value")

        // When
        val result = ActionResult.success(message, data = data)

        // Then
        assertThat(result.success).isTrue()
        assertThat(result.message).isEqualTo(message)
        assertThat(result.data).isEqualTo(data)
    }

    @Test
    fun `ActionResult failure factory creates valid result`() {
        // Given
        val message = "Action failed"
        val error = RuntimeException("Test error")

        // When
        val result = ActionResult.failure(message, error)

        // Then
        assertThat(result.success).isFalse()
        assertThat(result.message).isEqualTo(message)
        assertThat(result.error).isEqualTo(error)
    }

    @Test
    fun `ActionResult failure without error creates valid result`() {
        // Given
        val message = "Action failed"

        // When
        val result = ActionResult.failure(message)

        // Then
        assertThat(result.success).isFalse()
        assertThat(result.message).isEqualTo(message)
        assertThat(result.error).isNull()
    }

    // ========================================================================
    // UIActions Companion Object Tests
    // ========================================================================

    @Test
    fun `UIActions ALL_ACTIONS contains all 28 actions`() {
        // When
        val allActions = UIActions.ALL_ACTIONS

        // Then
        assertThat(allActions).hasSize(28)

        // Basic Interactions (8)
        assertThat(allActions).contains("tap")
        assertThat(allActions).contains("type")
        assertThat(allActions).contains("enter")
        assertThat(allActions).contains("longPress")
        assertThat(allActions).contains("clear")
        assertThat(allActions).contains("paste")
        assertThat(allActions).contains("swipe")
        assertThat(allActions).contains("scroll")

        // Navigation (7)
        assertThat(allActions).contains("home")
        assertThat(allActions).contains("back")
        assertThat(allActions).contains("launch")
        assertThat(allActions).contains("switchApp")
        assertThat(allActions).contains("openUrl")
        assertThat(allActions).contains("openSettings")
        assertThat(allActions).contains("notifications")

        // Clipboard (2)
        assertThat(allActions).contains("clipboardGet")
        assertThat(allActions).contains("clipboardSet")

        // Multi-step Skills (6)
        assertThat(allActions).contains("readScreen")
        assertThat(allActions).contains("submitMessage")
        assertThat(allActions).contains("copyVisibleText")
        assertThat(allActions).contains("waitForContent")
        assertThat(allActions).contains("findAndTap")
        assertThat(allActions).contains("composeEmail")

        // System (5)
        assertThat(allActions).contains("screenshot")
        assertThat(allActions).contains("shell")
        assertThat(allActions).contains("keyEvent")
        assertThat(allActions).contains("pullFile")
        assertThat(allActions).contains("pushFile")
        assertThat(allActions).contains("wait")
        assertThat(allActions).contains("done")
    }

    @Test
    fun `UIActions KeyCodes contains common key codes`() {
        // Then
        assertThat(UIActions.KeyCodes.KEYCODE_HOME).isEqualTo(3)
        assertThat(UIActions.KeyCodes.KEYCODE_BACK).isEqualTo(4)
        assertThat(UIActions.KeyCodes.KEYCODE_ENTER).isEqualTo(66)
        assertThat(UIActions.KeyCodes.KEYCODE_POWER).isEqualTo(26)
        assertThat(UIActions.KeyCodes.KEYCODE_VOLUME_UP).isEqualTo(24)
        assertThat(UIActions.KeyCodes.KEYCODE_VOLUME_DOWN).isEqualTo(25)
    }

    @Test
    fun `ScrollDirection enum has all four directions`() {
        // When
        val directions = ScrollDirection.values().toList()

        // Then
        assertThat(directions).hasSize(4)
        assertThat(directions).contains(ScrollDirection.UP)
        assertThat(directions).contains(ScrollDirection.DOWN)
        assertThat(directions).contains(ScrollDirection.LEFT)
        assertThat(directions).contains(ScrollDirection.RIGHT)
    }
}
