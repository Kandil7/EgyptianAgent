package com.egyptian.agent.accessibility.ui

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import com.google.common.truth.Truth.assertThat

/**
 * Comprehensive unit tests for AccessibilityTreeParser.
 * 
 * Target: >90% code coverage
 * 
 * @author EgyptianAgent Team
 * @version 1.0.0
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AccessibilityTreeParserTest {

    @Mock
    private lateinit var mockAccessibilityService: AccessibilityService

    @Mock
    private lateinit var mockRootNode: AccessibilityNodeInfo

    @Mock
    private lateinit var mockChildNode: AccessibilityNodeInfo

    private lateinit var parser: AccessibilityTreeParser

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        parser = AccessibilityTreeParser(mockAccessibilityService)
    }

    // ========================================================================
    // XML Parsing Tests
    // ========================================================================

    @Test
    fun `parseCurrentTree with valid root node returns non-empty tree`() {
        // Given
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockRootNode.packageName).thenReturn("com.whatsapp")
        `when`(mockRootNode.childCount).thenReturn(2)
        `when`(mockRootNode.getChild(0)).thenReturn(mockChildNode)
        `when`(mockRootNode.getChild(1)).thenReturn(null)
        `when`(mockChildNode.className).thenReturn("android.widget.Button")
        `when`(mockChildNode.text).thenReturn("Send Message")
        `when`(mockChildNode.isClickable).thenReturn(true)
        `when`(mockChildNode.isFocusable).thenReturn(true)
        `when`(mockChildNode.isEnabled).thenReturn(true)
        `when`(mockChildNode.childCount).thenReturn(0)

        // When
        val tree = parser.parseCurrentTree()

        // Then
        assertThat(tree.packageName).isEqualTo("com.whatsapp")
        assertThat(tree.elements).isNotEmpty()
        assertThat(tree.isActiveWindow).isTrue()
    }

    @Test
    fun `parseCurrentTree with null root node returns empty tree`() {
        // Given
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(null)

        // When
        val tree = parser.parseCurrentTree()

        // Then
        assertThat(tree.packageName).isEqualTo("unknown")
        assertThat(tree.elements).isEmpty()
        assertThat(tree.isActiveWindow).isFalse()
    }

    @Test
    fun `parseCurrentTree with empty child count returns empty tree`() {
        // Given
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockRootNode.packageName).thenReturn("com.android.settings")
        `when`(mockRootNode.childCount).thenReturn(0)

        // When
        val tree = parser.parseCurrentTree()

        // Then
        assertThat(tree.packageName).isEqualTo("com.android.settings")
        assertThat(tree.elements).isEmpty()
    }

    @Test
    fun `parseCurrentTree handles deep hierarchy up to MAX_DEPTH`() {
        // Given
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockRootNode.packageName).thenReturn("com.facebook.katana")
        `when`(mockRootNode.childCount).thenReturn(1)
        `when`(mockRootNode.getChild(0)).thenReturn(mockChildNode)
        `when`(mockChildNode.childCount).thenReturn(1)
        `when`(mockChildNode.className).thenReturn("android.widget.TextView")
        `when`(mockChildNode.text).thenReturn("Deep element")
        `when`(mockChildNode.isClickable).thenReturn(false)
        `when`(mockChildNode.isFocusable).thenReturn(false)

        // When
        val tree = parser.parseCurrentTree()

        // Then
        assertThat(tree.packageName).isEqualTo("com.facebook.katana")
    }

    // ========================================================================
    // UI Element Extraction Tests
    // ========================================================================

    @Test
    fun `parseCurrentTree extracts clickable elements`() {
        // Given
        val clickableNode = createMockNode(
            className = "android.widget.Button",
            text = "Click Me",
            isClickable = true,
            isFocusable = true
        )
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockRootNode.packageName).thenReturn("com.example.app")
        `when`(mockRootNode.childCount).thenReturn(1)
        `when`(mockRootNode.getChild(0)).thenReturn(clickableNode)
        `when`(clickableNode.childCount).thenReturn(0)

        // When
        val tree = parser.parseCurrentTree()

        // Then
        assertThat(tree.elements).hasSize(1)
        assertThat(tree.elements[0].isClickable).isTrue()
        assertThat(tree.elements[0].text).isEqualTo("Click Me")
    }

    @Test
    fun `parseCurrentTree extracts elements with content description`() {
        // Given
        val nodeWithDesc = createMockNode(
            className = "android.widget.ImageView",
            contentDescription = "Profile Picture",
            isClickable = true
        )
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockRootNode.packageName).thenReturn("com.instagram.android")
        `when`(mockRootNode.childCount).thenReturn(1)
        `when`(mockRootNode.getChild(0)).thenReturn(nodeWithDesc)
        `when`(nodeWithDesc.childCount).thenReturn(0)

        // When
        val tree = parser.parseCurrentTree()

        // Then
        assertThat(tree.elements).hasSize(1)
        assertThat(tree.elements[0].contentDescription).isEqualTo("Profile Picture")
    }

    @Test
    fun `parseCurrentTree extracts editable text fields`() {
        // Given
        val editTextNode = createMockNode(
            className = "android.widget.EditText",
            text = "Enter message",
            isEditable = true,
            isClickable = false
        )
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockRootNode.packageName).thenReturn("com.whatsapp")
        `when`(mockRootNode.childCount).thenReturn(1)
        `when`(mockRootNode.getChild(0)).thenReturn(editTextNode)
        `when`(editTextNode.childCount).thenReturn(0)

        // When
        val tree = parser.parseCurrentTree()

        // Then
        assertThat(tree.elements).hasSize(1)
        assertThat(tree.elements[0].isEditable).isTrue()
        assertThat(tree.elements[0].className).contains("EditText")
    }

    @Test
    fun `parseCurrentTree limits elements to MAX_ELEMENTS_PER_SCREEN`() {
        // Given
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockRootNode.packageName).thenReturn("com.facebook.katana")
        `when`(mockRootNode.childCount).thenReturn(150) // More than MAX_ELEMENTS_PER_SCREEN (100)

        // Mock 150 child nodes
        for (i in 0 until 150) {
            val childNode = createMockNode(
                className = "android.widget.TextView",
                text = "Element $i",
                isClickable = true
            )
            `when`(mockRootNode.getChild(i)).thenReturn(childNode)
            `when`(childNode.childCount).thenReturn(0)
        }

        // When
        val tree = parser.parseCurrentTree()

        // Then
        assertThat(tree.elements.size).isAtMost(100)
    }

    // ========================================================================
    // Diff Detection Tests
    // ========================================================================

    @Test
    fun `diffTrees detects added elements`() {
        // Given
        val oldElement = UIElement(
            id = "element_1",
            className = "Button",
            text = "Old Button",
            contentDescription = null,
            bounds = Rect(0, 0, 100, 50),
            isClickable = true,
            isFocusable = true,
            isEnabled = true,
            depth = 0
        )

        val newElement1 = oldElement
        val newElement2 = UIElement(
            id = "element_2",
            className = "Button",
            text = "New Button",
            contentDescription = null,
            bounds = Rect(0, 50, 100, 100),
            isClickable = true,
            isFocusable = true,
            isEnabled = true,
            depth = 0
        )

        val oldTree = AccessibilityTree(
            timestamp = 1000,
            packageName = "com.whatsapp",
            elements = listOf(oldElement)
        )

        val newTree = AccessibilityTree(
            timestamp = 2000,
            packageName = "com.whatsapp",
            elements = listOf(newElement1, newElement2)
        )

        // When
        val diff = parser.diffTrees(oldTree, newTree)

        // Then
        assertThat(diff.screenChanged).isTrue()
        assertThat(diff.addedElements).hasSize(1)
        assertThat(diff.addedElements[0].text).isEqualTo("New Button")
        assertThat(diff.removedElements).isEmpty()
    }

    @Test
    fun `diffTrees detects removed elements`() {
        // Given
        val element1 = UIElement(
            id = "element_1",
            className = "Button",
            text = "Keep This",
            contentDescription = null,
            bounds = Rect(0, 0, 100, 50),
            isClickable = true,
            isFocusable = true,
            isEnabled = true,
            depth = 0
        )

        val element2 = UIElement(
            id = "element_2",
            className = "Button",
            text = "Remove This",
            contentDescription = null,
            bounds = Rect(0, 50, 100, 100),
            isClickable = true,
            isFocusable = true,
            isEnabled = true,
            depth = 0
        )

        val oldTree = AccessibilityTree(
            timestamp = 1000,
            packageName = "com.whatsapp",
            elements = listOf(element1, element2)
        )

        val newTree = AccessibilityTree(
            timestamp = 2000,
            packageName = "com.whatsapp",
            elements = listOf(element1)
        )

        // When
        val diff = parser.diffTrees(oldTree, newTree)

        // Then
        assertThat(diff.screenChanged).isTrue()
        assertThat(diff.removedElements).hasSize(1)
        assertThat(diff.removedElements[0].text).isEqualTo("Remove This")
        assertThat(diff.addedElements).isEmpty()
    }

    @Test
    fun `diffTrees detects modified elements`() {
        // Given
        val oldElement = UIElement(
            id = "element_1",
            className = "Button",
            text = "Old Text",
            contentDescription = null,
            bounds = Rect(0, 0, 100, 50),
            isClickable = true,
            isFocusable = true,
            isEnabled = true,
            depth = 0
        )

        val newElement = UIElement(
            id = "element_1",
            className = "Button",
            text = "New Text",
            contentDescription = null,
            bounds = Rect(0, 0, 100, 50),
            isClickable = true,
            isFocusable = true,
            isEnabled = true,
            depth = 0
        )

        val oldTree = AccessibilityTree(
            timestamp = 1000,
            packageName = "com.whatsapp",
            elements = listOf(oldElement)
        )

        val newTree = AccessibilityTree(
            timestamp = 2000,
            packageName = "com.whatsapp",
            elements = listOf(newElement)
        )

        // When
        val diff = parser.diffTrees(oldTree, newTree)

        // Then
        assertThat(diff.screenChanged).isTrue()
        assertThat(diff.modifiedElements).hasSize(1)
        assertThat(diff.modifiedElements[0].changes).isNotEmpty()
    }

    @Test
    fun `diffTrees returns no changes for identical trees`() {
        // Given
        val element = UIElement(
            id = "element_1",
            className = "Button",
            text = "Same Text",
            contentDescription = null,
            bounds = Rect(0, 0, 100, 50),
            isClickable = true,
            isFocusable = true,
            isEnabled = true,
            depth = 0
        )

        val oldTree = AccessibilityTree(
            timestamp = 1000,
            packageName = "com.whatsapp",
            elements = listOf(element)
        )

        val newTree = AccessibilityTree(
            timestamp = 2000,
            packageName = "com.whatsapp",
            elements = listOf(element)
        )

        // When
        val diff = parser.diffTrees(oldTree, newTree)

        // Then
        assertThat(diff.screenChanged).isFalse()
        assertThat(diff.addedElements).isEmpty()
        assertThat(diff.removedElements).isEmpty()
        assertThat(diff.modifiedElements).isEmpty()
    }

    // ========================================================================
    // Edge Cases Tests
    // ========================================================================

    @Test
    fun `parseCurrentTree handles empty tree correctly`() {
        // Given
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockRootNode.packageName).thenReturn("com.example.empty")
        `when`(mockRootNode.childCount).thenReturn(0)

        // When
        val tree = parser.parseCurrentTree()

        // Then
        assertThat(tree.isEmpty()).isTrue()
        assertThat(tree.packageName).isEqualTo("com.example.empty")
    }

    @Test
    fun `parseCurrentTree handles null text and description`() {
        // Given
        val node = createMockNode(
            className = "android.widget.FrameLayout",
            text = null,
            contentDescription = null,
            isClickable = false,
            isFocusable = false
        )
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockRootNode.packageName).thenReturn("com.example.app")
        `when`(mockRootNode.childCount).thenReturn(1)
        `when`(mockRootNode.getChild(0)).thenReturn(node)
        `when`(node.childCount).thenReturn(0)

        // When
        val tree = parser.parseCurrentTree()

        // Then
        // Non-interactive elements without text/description should be filtered out
        assertThat(tree.elements).isEmpty()
    }

    @Test
    fun `parseCurrentTree truncates long text to 200 characters`() {
        // Given
        val longText = "A".repeat(300)
        val node = createMockNode(
            className = "android.widget.TextView",
            text = longText,
            isClickable = true
        )
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockRootNode.packageName).thenReturn("com.example.app")
        `when`(mockRootNode.childCount).thenReturn(1)
        `when`(mockRootNode.getChild(0)).thenReturn(node)
        `when`(node.childCount).thenReturn(0)

        // When
        val tree = parser.parseCurrentTree()

        // Then
        assertThat(tree.elements).hasSize(1)
        assertThat(tree.elements[0].text?.length).isAtMost(200)
    }

    @Test
    fun `isTreeEmpty returns true for empty tree`() {
        // Given
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(null)

        // When
        val isEmpty = parser.isTreeEmpty()

        // Then
        assertThat(isEmpty).isTrue()
    }

    @Test
    fun `isTreeEmpty returns false for non-empty tree`() {
        // Given
        val node = createMockNode(
            className = "Button",
            text = "Click",
            isClickable = true
        )
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockRootNode.packageName).thenReturn("com.example.app")
        `when`(mockRootNode.childCount).thenReturn(1)
        `when`(mockRootNode.getChild(0)).thenReturn(node)
        `when`(node.childCount).thenReturn(0)

        // When
        val isEmpty = parser.isTreeEmpty()

        // Then
        assertThat(isEmpty).isFalse()
    }

    // ========================================================================
    // Element Search Tests
    // ========================================================================

    @Test
    fun `findElementsByText finds matching elements`() {
        // Given
        val node1 = createMockNode(
            className = "Button",
            text = "Send Message",
            isClickable = true
        )
        val node2 = createMockNode(
            className = "Button",
            text = "Cancel",
            isClickable = true
        )
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockRootNode.packageName).thenReturn("com.whatsapp")
        `when`(mockRootNode.childCount).thenReturn(2)
        `when`(mockRootNode.getChild(0)).thenReturn(node1)
        `when`(mockRootNode.getChild(1)).thenReturn(node2)
        `when`(node1.childCount).thenReturn(0)
        `when`(node2.childCount).thenReturn(0)

        // When
        val elements = parser.findElementsByText("Send")

        // Then
        assertThat(elements).hasSize(1)
        assertThat(elements[0].text).isEqualTo("Send Message")
    }

    @Test
    fun `findElementsByText is case insensitive`() {
        // Given
        val node = createMockNode(
            className = "Button",
            text = "SEND MESSAGE",
            isClickable = true
        )
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockRootNode.packageName).thenReturn("com.whatsapp")
        `when`(mockRootNode.childCount).thenReturn(1)
        `when`(mockRootNode.getChild(0)).thenReturn(node)
        `when`(node.childCount).thenReturn(0)

        // When
        val elements = parser.findElementsByText("send")

        // Then
        assertThat(elements).hasSize(1)
    }

    @Test
    fun `findClickableElementByText returns clickable element`() {
        // Given
        val clickableNode = createMockNode(
            className = "Button",
            text = "Submit",
            isClickable = true
        )
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockRootNode.packageName).thenReturn("com.example.app")
        `when`(mockRootNode.childCount).thenReturn(1)
        `when`(mockRootNode.getChild(0)).thenReturn(clickableNode)
        `when`(clickableNode.childCount).thenReturn(0)

        // When
        val element = parser.findClickableElementByText("Submit")

        // Then
        assertThat(element).isNotNull()
        assertThat(element?.isClickable).isTrue()
    }

    @Test
    fun `findClickableElementByText returns null for non-clickable element`() {
        // Given
        val nonClickableNode = createMockNode(
            className = "TextView",
            text = "Label",
            isClickable = false
        )
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockRootNode.packageName).thenReturn("com.example.app")
        `when`(mockRootNode.childCount).thenReturn(1)
        `when`(mockRootNode.getChild(0)).thenReturn(nonClickableNode)
        `when`(nonClickableNode.childCount).thenReturn(0)

        // When
        val element = parser.findClickableElementByText("Label")

        // Then
        assertThat(element).isNull()
    }

    // ========================================================================
    // Tree Format Conversion Tests
    // ========================================================================

    @Test
    fun `toLLMXMLFormat returns valid XML string`() {
        // Given
        val node = createMockNode(
            className = "Button",
            text = "Click Me",
            isClickable = true
        )
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockRootNode.packageName).thenReturn("com.example.app")
        `when`(mockRootNode.childCount).thenReturn(1)
        `when`(mockRootNode.getChild(0)).thenReturn(node)
        `when`(node.childCount).thenReturn(0)

        // When
        val xml = parser.toLLMXMLFormat()

        // Then
        assertThat(xml).contains("<?xml version=\"1.0\"")
        assertThat(xml).contains("<accessibility-tree")
        assertThat(xml).contains("package=\"com.example.app\"")
        assertThat(xml).contains("</accessibility-tree>")
    }

    @Test
    fun `toLLMJSONFormat returns valid JSON structure`() {
        // Given
        val node = createMockNode(
            className = "Button",
            text = "Click Me",
            isClickable = true
        )
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockRootNode.packageName).thenReturn("com.example.app")
        `when`(mockRootNode.childCount).thenReturn(1)
        `when`(mockRootNode.getChild(0)).thenReturn(node)
        `when`(node.childCount).thenReturn(0)

        // When
        val json = parser.toLLMJSONFormat()

        // Then
        assertThat(json).contains("\"package\": \"com.example.app\"")
        assertThat(json).contains("\"elements\":")
    }

    @Test
    fun `clearCache resets lastTree`() {
        // Given
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockRootNode.packageName).thenReturn("com.example.app")
        `when`(mockRootNode.childCount).thenReturn(0)

        // When
        parser.parseCurrentTree()
        parser.clearCache()
        val lastTree = parser.getLastTree()

        // Then
        assertThat(lastTree).isNull()
    }

    @Test
    fun `getLastTree returns previously parsed tree`() {
        // Given
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockRootNode.packageName).thenReturn("com.whatsapp")
        `when`(mockRootNode.childCount).thenReturn(0)

        // When
        val firstTree = parser.parseCurrentTree()
        val lastTree = parser.getLastTree()

        // Then
        assertThat(lastTree).isNotNull()
        assertThat(lastTree?.packageName).isEqualTo("com.whatsapp")
        assertThat(lastTree?.timestamp).isEqualTo(firstTree.timestamp)
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private fun createMockNode(
        className: String,
        text: String? = null,
        contentDescription: String? = null,
        isClickable: Boolean,
        isFocusable: Boolean = false,
        isEnabled: Boolean = true,
        isEditable: Boolean = false
    ): AccessibilityNodeInfo {
        val node = org.mockito.Mockito.mock(AccessibilityNodeInfo::class.java)
        `when`(node.className).thenReturn(className)
        `when`(node.text).thenReturn(text)
        `when`(node.contentDescription).thenReturn(contentDescription)
        `when`(node.isClickable).thenReturn(isClickable)
        `when`(node.isFocusable).thenReturn(isFocusable)
        `when`(node.isEnabled).thenReturn(isEnabled)
        `when`(node.isEditable).thenReturn(isEditable)
        `when`(node.childCount).thenReturn(0)
        org.mockito.Mockito.doAnswer { invocation ->
            val rect = invocation.arguments[0] as Rect
            rect.set(0, 0, 100, 50)
            null
        }.`when`(node).getBoundsInScreen(org.mockito.ArgumentMatchers.any())
        return node
    }
}
