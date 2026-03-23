package com.egyptian.agent.accessibility.ui

import android.graphics.Rect

/**
 * Represents a parsed accessibility tree from Android AccessibilityService.
 * Adapted from DroidClaw's sanitizer.ts for EgyptianAgent hybrid architecture.
 *
 * @property timestamp Unix timestamp when tree was captured
 * @property packageName Package name of the current app
 * @property elements List of interactive UI elements
 * @property screenshotPath Optional screenshot path for vision fallback
 */
data class AccessibilityTree(
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val elements: List<UIElement>,
    val screenshotPath: String? = null,
    val isActiveWindow: Boolean = true
) {
    /**
     * Get all clickable elements
     */
    fun getClickableElements(): List<UIElement> = elements.filter { it.isClickable }

    /**
     * Get all focusable elements
     */
    fun getFocusableElements(): List<UIElement> = elements.filter { it.isFocusable }

    /**
     * Get elements by text content (supports Egyptian Arabic)
     */
    fun getElementsByText(text: String): List<UIElement> =
        elements.filter { it.text?.contains(text, ignoreCase = true) == true }

    /**
     * Get elements by content description
     */
    fun getElementsByDescription(description: String): List<UIElement> =
        elements.filter { it.contentDescription?.contains(description, ignoreCase = true) == true }

    /**
     * Check if tree is empty (requires vision fallback)
     */
    fun isEmpty(): Boolean = elements.isEmpty()

    /**
     * Get element count by depth
     */
    fun getElementCountByDepth(): Map<Int, Int> =
        elements.groupBy { it.depth }.mapValues { it.value.size }

    /**
     * Convert to LLM-readable XML format
     */
    fun toXMLFormat(maxElements: Int = 40): String {
        val sb = StringBuilder()
        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.appendLine("<accessibility-tree package=\"$packageName\" timestamp=\"$timestamp\">")

        val elementsToInclude = elements.take(maxElements)
        for (element in elementsToInclude) {
            sb.appendLine(element.toXML())
        }

        if (elements.size > maxElements) {
            sb.appendLine("<!-- ${elements.size - maxElements} more elements truncated -->")
        }

        sb.appendLine("</accessibility-tree>")
        return sb.toString()
    }

    /**
     * Convert to LLM-readable JSON format
     */
    fun toJSONFormat(maxElements: Int = 40): String {
        val elementsToInclude = elements.take(maxElements)
        val truncated = elements.size > maxElements

        return buildString {
            appendLine("{")
            appendLine("  \"package\": \"$packageName\",")
            appendLine("  \"timestamp\": $timestamp,")
            appendLine("  \"element_count\": ${elements.size},")
            if (truncated) {
                appendLine("  \"truncated\": true,")
                appendLine("  \"max_elements\": $maxElements,")
            }
            appendLine("  \"elements\": [")
            elementsToInclude.forEachIndexed { index, element ->
                appendLine(element.toJSON())
                if (index < elementsToInclude.lastIndex) appendLine(",")
            }
            appendLine("  ]")
            appendLine("}")
        }
    }

    companion object {
        const val DEFAULT_MAX_ELEMENTS = 40
    }
}

/**
 * Represents an interactive UI element from the accessibility tree.
 * Adapted from DroidClaw's element structure for EgyptianAgent.
 *
 * @property id Unique element identifier (view ID or generated)
 * @property className Android view class name
 * @property text Visible text content (supports Egyptian Arabic)
 * @property contentDescription Content description for accessibility
 * @property bounds Screen coordinates (bounding box)
 * @property isClickable Whether element can be clicked
 * @property isFocusable Whether element can receive focus
 * @property isEnabled Whether element is enabled
 * @property depth Depth in the accessibility tree hierarchy
 * @property index Index among siblings
 * @property children Child elements (nested)
 * @property isChecked For checkboxes/toggles
 * @property isSelected For selected items
 * @property isEditable For text input fields
 * @property progress Progress value for progress bars (0.0-1.0)
 */
data class UIElement(
    val id: String,
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val bounds: Rect,
    val isClickable: Boolean,
    val isFocusable: Boolean,
    val isEnabled: Boolean,
    val depth: Int,
    val index: Int = 0,
    val children: List<UIElement> = emptyList(),
    val isChecked: Boolean = false,
    val isSelected: Boolean = false,
    val isEditable: Boolean = false,
    val progress: Float = -1f
) {
    /**
     * Get center coordinates for tap action
     */
    fun getCenterPoint(): Pair<Int, Int> {
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        return Pair(centerX, centerY)
    }

    /**
     * Get human-readable description for LLM
     */
    fun getDescription(): String {
        val descriptions = mutableListOf<String>()

        if (!text.isNullOrBlank()) {
            descriptions.add("text=\"$text\"")
        }
        if (!contentDescription.isNullOrBlank()) {
            descriptions.add("description=\"$contentDescription\"")
        }
        if (className.contains("Button", ignoreCase = true)) {
            descriptions.add("type=button")
        } else if (className.contains("EditText", ignoreCase = true)) {
            descriptions.add("type=textfield")
        } else if (className.contains("ImageView", ignoreCase = true)) {
            descriptions.add("type=image")
        } else if (className.contains("TextView", ignoreCase = true)) {
            descriptions.add("type=text")
        }

        if (isChecked) descriptions.add("checked=true")
        if (isSelected) descriptions.add("selected=true")
        if (isEditable) descriptions.add("editable=true")
        if (progress >= 0f) descriptions.add("progress=${(progress * 100).toInt()}%")

        descriptions.add("bounds=[${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}]")
        descriptions.add("clickable=$isClickable")

        return descriptions.joinToString(", ")
    }

    /**
     * Check if element matches search text (Egyptian Arabic support)
     */
    fun matchesSearch(text: String): Boolean {
        val searchText = text.lowercase().trim()
        return this.text?.lowercase()?.contains(searchText) == true ||
                this.contentDescription?.lowercase()?.contains(searchText) == true
    }

    /**
     * Convert to XML format for LLM
     */
    fun toXML(indent: String = "    "): String {
        val sb = StringBuilder()
        sb.appendLine("$indent<element")
        sb.appendLine("$indent  id=\"$id\"")
        sb.appendLine("$indent  class=\"$className\"")
        if (!text.isNullOrBlank()) {
            sb.appendLine("$indent  text=\"${escapeXML(text)}\"")
        }
        if (!contentDescription.isNullOrBlank()) {
            sb.appendLine("$indent  description=\"${escapeXML(contentDescription)}\"")
        }
        sb.appendLine("$indent  bounds=\"${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}\"")
        sb.appendLine("$indent  clickable=\"$isClickable\"")
        sb.appendLine("$indent  focusable=\"$isFocusable\"")
        sb.appendLine("$indent  enabled=\"$isEnabled\"")
        sb.appendLine("$indent  depth=\"$depth\"")

        if (children.isNotEmpty()) {
            sb.appendLine("$indent  >")
            children.forEach { child ->
                sb.appendLine(child.toXML("$indent  "))
            }
            sb.appendLine("$indent</element>")
        } else {
            sb.appendLine("$indent/>")
        }

        return sb.toString()
    }

    /**
     * Convert to JSON format for LLM
     */
    fun toJSON(indent: Int = 2): String {
        val indentStr = " ".repeat(indent)
        return buildString {
            appendLine("$indentStr{")
            appendLine("$indentStr  \"id\": \"$id\",")
            appendLine("$indentStr  \"class\": \"$className\",")
            if (!text.isNullOrBlank()) {
                appendLine("$indentStr  \"text\": \"${escapeJSON(text)}\",")
            }
            if (!contentDescription.isNullOrBlank()) {
                appendLine("$indentStr  \"description\": \"${escapeJSON(contentDescription)}\",")
            }
            appendLine("$indentStr  \"bounds\": {")
            appendLine("$indentStr    \"left\": ${bounds.left},")
            appendLine("$indentStr    \"top\": ${bounds.top},")
            appendLine("$indentStr    \"right\": ${bounds.right},")
            appendLine("$indentStr    \"bottom\": ${bounds.bottom}")
            appendLine("$indentStr  },")
            appendLine("$indentStr  \"clickable\": $isClickable,")
            appendLine("$indentStr  \"focusable\": $isFocusable,")
            appendLine("$indentStr  \"enabled\": $isEnabled,")
            appendLine("$indentStr  \"depth\": $depth,")
            appendLine("$indentStr  \"center\": {")
            appendLine("$indentStr    \"x\": ${getCenterPoint().first},")
            appendLine("$indentStr    \"y\": ${getCenterPoint().second}")
            appendLine("$indentStr  }")
            appendLine("$indentStr}")
        }
    }

    private fun escapeXML(text: String): String =
        text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    private fun escapeJSON(text: String): String =
        text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
}

/**
 * Result of comparing two accessibility trees
 */
data class TreeDiff(
    val addedElements: List<UIElement>,
    val removedElements: List<UIElement>,
    val modifiedElements: List<ModifiedElement>,
    val screenChanged: Boolean,
    val changeSummary: String
)

/**
 * Represents a modified element between tree versions
 */
data class ModifiedElement(
    val oldElement: UIElement,
    val newElement: UIElement,
    val changes: List<String>
)

/**
 * Vision fallback data for when accessibility tree is empty
 * (Flutter, WebView, games)
 */
data class VisionFallback(
    val screenshotPath: String,
    val boundingBoxes: List<BoundingBox>,
    val detectedElements: List<VisionElement>,
    val confidence: Float
)

/**
 * Bounding box detected by vision model
 */
data class BoundingBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val label: String,
    val confidence: Float,
    val elementType: ElementType = ElementType.UNKNOWN
)

/**
 * Element type detected by vision
 */
enum class ElementType {
    BUTTON,
    TEXT_FIELD,
    TEXT_LABEL,
    IMAGE,
    ICON,
    LIST_ITEM,
    CARD,
    NAVIGATION_BAR,
    UNKNOWN
}

/**
 * Element detected by vision model
 */
data class VisionElement(
    val id: String,
    val label: String,
    val bounds: Rect,
    val confidence: Float,
    val isInteractive: Boolean,
    val elementType: ElementType
) {
    fun getCenterPoint(): Pair<Int, Int> {
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        return Pair(centerX, centerY)
    }
}
