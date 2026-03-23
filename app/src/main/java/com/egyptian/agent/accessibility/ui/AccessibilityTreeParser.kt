package com.egyptian.agent.accessibility.ui

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.graphics.Rect
import java.util.UUID

/**
 * Accessibility Tree Parser for EgyptianAgent Hybrid Architecture.
 * 
 * Parses Android accessibility tree into LLM-readable format.
 * Adapted from DroidClaw's sanitizer.ts for EgyptianAgent.
 *
 * Features:
 * - Dump accessibility tree via AccessibilityService
 * - Parse XML to structured UI elements
 * - Diff detection for screen changes
 * - Handle Flutter/webview edge cases
 * - Vision fallback for empty trees
 *
 * @author EgyptianAgent Team
 * @version 1.0.0
 */
class AccessibilityTreeParser(
    private val accessibilityService: AccessibilityService
) {
    companion object {
        private const val TAG = "AccessibilityTreeParser"
        private const val MAX_DEPTH = 30
        private const val MAX_ELEMENTS_PER_SCREEN = 100
    }

    private var lastTree: AccessibilityTree? = null

    /**
     * Parse current accessibility tree from active window.
     * 
     * @return AccessibilityTree with parsed UI elements
     */
    fun parseCurrentTree(): AccessibilityTree {
        val startTime = System.currentTimeMillis()
        
        val rootNode = accessibilityService.rootInActiveWindow
        val packageName = rootNode?.packageName?.toString() ?: "unknown"
        
        Log.d(TAG, "Parsing accessibility tree for package: $packageName")
        
        val elements = if (rootNode != null) {
            parseNode(rootNode, depth = 0)
        } else {
            Log.w(TAG, "Root node is null, returning empty tree")
            emptyList()
        }
        
        // Apply element ranking and limit
        val rankedElements = rankAndLimitElements(elements, MAX_ELEMENTS_PER_SCREEN)
        
        val tree = AccessibilityTree(
            timestamp = System.currentTimeMillis(),
            packageName = packageName,
            elements = rankedElements,
            isActiveWindow = rootNode != null
        )
        
        // Check if vision fallback is needed
        if (tree.isEmpty()) {
            Log.w(TAG, "Accessibility tree is empty, vision fallback may be needed")
        }
        
        lastTree = tree
        
        val parseTime = System.currentTimeMillis() - startTime
        Log.d(TAG, "Parsed ${tree.elements.size} elements in ${parseTime}ms")
        
        return tree
    }

    /**
     * Recursively parse an AccessibilityNodeInfo and its children.
     */
    private fun parseNode(node: AccessibilityNodeInfo, depth: Int): List<UIElement> {
        if (depth > MAX_DEPTH) {
            return emptyList()
        }

        val elements = mutableListOf<UIElement>()
        
        // Generate unique ID for this element
        val elementId = generateElementId(node, depth)
        
        // Extract element properties
        val element = UIElement(
            id = elementId,
            className = node.className?.toString() ?: "Unknown",
            text = node.text?.toString()?.take(200), // Limit text length
            contentDescription = node.contentDescription?.toString()?.take(200),
            bounds = Rect().also { node.getBoundsInScreen(it) },
            isClickable = node.isClickable,
            isFocusable = node.isFocusable,
            isEnabled = node.isEnabled,
            depth = depth,
            index = getElementIndex(node),
            isChecked = node.isChecked,
            isSelected = node.isSelected,
            isEditable = node.isEditable,
            progress = if (node.rangeInfo != null) node.rangeInfo.current else -1f
        )
        
        // Only include interactive or meaningful elements
        if (shouldIncludeElement(element, node)) {
            elements.add(element)
        }
        
        // Parse children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                try {
                    val childElements = parseNode(child, depth + 1)
                    elements.addAll(childElements)
                } finally {
                    child.recycle()
                }
            }
        }
        
        return elements
    }

    /**
     * Generate unique element ID.
     */
    private fun generateElementId(node: AccessibilityNodeInfo, depth: Int): String {
        val viewId = node.viewIdResourceName ?: ""
        val className = node.className?.toString() ?: "Unknown"
        val text = node.text?.toString()?.take(20) ?: ""
        val contentDesc = node.contentDescription?.toString()?.take(20) ?: ""
        
        // Create readable ID
        val baseId = if (viewId.isNotEmpty()) {
            viewId.substringAfterLast('/').take(50)
        } else if (contentDesc.isNotEmpty()) {
            "desc_${contentDesc.replace(Regex("[^a-zA-Z0-9]"), "_").take(30)}"
        } else if (text.isNotEmpty()) {
            "text_${text.replace(Regex("[^a-zA-Z0-9]"), "_").take(30)}"
        } else {
            "${className.substringAfterLast('.')}_${depth}_${System.nanoTime()}"
        }
        
        return baseId.replace(Regex("[^a-zA-Z0-9_]"), "_").take(100)
    }

    /**
     * Get index of node among its siblings.
     */
    private fun getElementIndex(node: AccessibilityNodeInfo): Int {
        val parent = node.parent ?: return 0
        for (i in 0 until parent.childCount) {
            if (parent.getChild(i) == node) {
                return i
            }
        }
        return 0
    }

    /**
     * Determine if element should be included in the tree.
     */
    private fun shouldIncludeElement(element: UIElement, node: AccessibilityNodeInfo): Boolean {
        // Always include clickable elements
        if (element.isClickable) return true
        
        // Include focusable elements
        if (element.isFocusable) return true
        
        // Include elements with text or content description
        if (!element.text.isNullOrBlank()) return true
        if (!element.contentDescription.isNullOrBlank()) return true
        
        // Include editable fields
        if (element.isEditable) return true
        
        // Include important view types
        val className = element.className.lowercase()
        if (className.contains("button") ||
            className.contains("edittext") ||
            className.contains("imageview") ||
            className.contains("textview") ||
            className.contains("webview") ||
            className.contains("listview") ||
            className.contains("recyclerview")) {
            return true
        }
        
        // Include elements with progress
        if (element.progress >= 0f) return true
        
        return false
    }

    /**
     * Rank elements by importance and limit count.
     */
    private fun rankAndLimitElements(elements: List<UIElement>, maxCount: Int): List<UIElement> {
        // Score elements by importance
        val scoredElements = elements.map { element ->
            val score = calculateElementScore(element)
            Pair(element, score)
        }
        
        // Sort by score (descending)
        val sorted = scoredElements.sortedByDescending { pair -> pair.second }

        // Take top elements
        return sorted.take(maxCount).map { pair -> pair.first }
    }

    /**
     * Calculate importance score for an element.
     */
    private fun calculateElementScore(element: UIElement): Int {
        var score = 0
        
        // Interactive elements get higher score
        if (element.isClickable) score += 10
        if (element.isFocusable) score += 5
        if (element.isEditable) score += 8
        
        // Elements with text/description are more important
        if (!element.text.isNullOrBlank()) score += 5
        if (!element.contentDescription.isNullOrBlank()) score += 5
        
        // Certain element types are more important
        val className = element.className.lowercase()
        when {
            className.contains("button") -> score += 8
            className.contains("edittext") -> score += 7
            className.contains("imagebutton") -> score += 6
            className.contains("checkbox") -> score += 5
            className.contains("seekbar") -> score += 4
        }
        
        // Elements at shallower depth are usually more important
        score += kotlin.math.max(0, 10 - element.depth)
        
        // Larger elements are often more important
        val area = element.bounds.width() * element.bounds.height()
        if (area > 10000) score += 3
        else if (area > 5000) score += 2
        else if (area > 1000) score += 1
        
        return score
    }

    /**
     * Detect changes between two accessibility trees.
     * 
     * @param oldTree Previous tree
     * @param newTree Current tree
     * @return TreeDiff with changes
     */
    fun diffTrees(oldTree: AccessibilityTree, newTree: AccessibilityTree): TreeDiff {
        val addedElements = mutableListOf<UIElement>()
        val removedElements = mutableListOf<UIElement>()
        val modifiedElements = mutableListOf<ModifiedElement>()

        val oldElementIds = oldTree.elements.associateBy { element -> element.id }
        val newElementIds = newTree.elements.associateBy { element -> element.id }

        // Find added elements
        newTree.elements.forEach { newElement ->
            if (!oldElementIds.containsKey(newElement.id)) {
                addedElements.add(newElement)
            }
        }

        // Find removed elements
        oldTree.elements.forEach { oldElement ->
            if (!newElementIds.containsKey(oldElement.id)) {
                removedElements.add(oldElement)
            }
        }
        
        // Find modified elements
        newTree.elements.forEach { newElement ->
            val oldElement = oldElementIds[newElement.id]
            if (oldElement != null && oldElement != newElement) {
                val changes = detectElementChanges(oldElement, newElement)
                if (changes.isNotEmpty()) {
                    modifiedElements.add(ModifiedElement(oldElement, newElement, changes))
                }
            }
        }
        
        val screenChanged = addedElements.isNotEmpty() || 
                           removedElements.isNotEmpty() || 
                           modifiedElements.isNotEmpty()
        
        val changeSummary = buildString {
            append("Screen changed: $screenChanged. ")
            append("Added: ${addedElements.size}, ")
            append("Removed: ${removedElements.size}, ")
            append("Modified: ${modifiedElements.size}")
        }
        
        Log.d(TAG, "Tree diff: $changeSummary")
        
        return TreeDiff(
            addedElements = addedElements,
            removedElements = removedElements,
            modifiedElements = modifiedElements,
            screenChanged = screenChanged,
            changeSummary = changeSummary
        )
    }

    /**
     * Detect changes between two versions of the same element.
     */
    private fun detectElementChanges(oldElement: UIElement, newElement: UIElement): List<String> {
        val changes = mutableListOf<String>()
        
        if (oldElement.text != newElement.text) {
            changes.add("text changed: \"${oldElement.text}\" → \"${newElement.text}\"")
        }
        
        if (oldElement.contentDescription != newElement.contentDescription) {
            changes.add("description changed")
        }
        
        if (oldElement.isChecked != newElement.isChecked) {
            changes.add("checked state changed: ${oldElement.isChecked} → ${newElement.isChecked}")
        }
        
        if (oldElement.isSelected != newElement.isSelected) {
            changes.add("selected state changed")
        }
        
        if (oldElement.isEnabled != newElement.isEnabled) {
            changes.add("enabled state changed")
        }
        
        if (oldElement.bounds != newElement.bounds) {
            changes.add("bounds changed")
        }
        
        return changes
    }

    /**
     * Convert current tree to LLM-readable XML format.
     */
    fun toLLMXMLFormat(maxElements: Int = 40): String {
        val tree = parseCurrentTree()
        return tree.toXMLFormat(maxElements)
    }

    /**
     * Convert current tree to LLM-readable JSON format.
     */
    fun toLLMJSONFormat(maxElements: Int = 40): String {
        val tree = parseCurrentTree()
        return tree.toJSONFormat(maxElements)
    }

    /**
     * Check if current tree is empty (requires vision fallback).
     */
    fun isTreeEmpty(): Boolean {
        val tree = parseCurrentTree()
        return tree.isEmpty()
    }

    /**
     * Find elements matching search text (supports Egyptian Arabic).
     */
    fun findElementsByText(searchText: String): List<UIElement> {
        val tree = parseCurrentTree()
        return tree.getElementsByText(searchText)
    }

    /**
     * Find clickable elements matching search text.
     */
    fun findClickableElementByText(searchText: String): UIElement? {
        val elements = findElementsByText(searchText)
        return elements.firstOrNull { element -> element.isClickable }
    }

    /**
     * Get last parsed tree.
     */
    fun getLastTree(): AccessibilityTree? = lastTree

    /**
     * Clear cached tree.
     */
    fun clearCache() {
        lastTree = null
    }
}
