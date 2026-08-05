package com.egyptian.agent.hybrid

import android.content.Context
import android.util.Log
import com.egyptian.agent.accessibility.ui.AccessibilityTreeParser
import com.egyptian.agent.ai.FunctionGemmaIntentEngine
import com.egyptian.agent.navigation.UINavigationEngine
import com.egyptian.agent.navigation.UIAction
import com.egyptian.agent.navigation.ActionResult
import com.egyptian.agent.nlp.IntentResult
import com.egyptian.agent.nlp.IntentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Hybrid Orchestrator for EgyptianAgent.
 *
 * Combines FunctionGemma intent-based system (fast path) with
 * DroidClaw-style UI navigation (slow path) for comprehensive
 * voice control capabilities.
 *
 * Features:
 * - Receives intent from FunctionGemma
 * - Decides: fast path (intent) vs slow path (UI navigation)
 * - Manages perception → reason → action loop for UI navigation
 * - Fallback mechanisms for stuck scenarios
 *
 * @author EgyptianAgent Team
 * @version 3.0.0
 */
class HybridOrchestrator private constructor(
    private val context: Context,
    private val functionGemmaEngine: FunctionGemmaIntentEngine,
    private val uiNavigationEngine: UINavigationEngine? = null
) {
    companion object {
        private const val TAG = "HybridOrchestrator"

        // Routing thresholds
        private const val FAST_PATH_CONFIDENCE_THRESHOLD = 0.85f
        private const val SLOW_PATH_CONFIDENCE_THRESHOLD = 0.70f

        // UI Navigation limits
        private const val MAX_NAVIGATION_ITERATIONS = 10
        private const val STUCK_THRESHOLD = 3  // Same screen for 3 iterations = stuck

        /**
         * Factory method for Java compatibility
         * Creates orchestrator with internally managed dependencies
         */
        @JvmStatic
        fun create(context: Context): HybridOrchestrator {
            return HybridOrchestrator(
                context,
                FunctionGemmaIntentEngine(context)
                // UINavigationEngine is optional - can be initialized later when AccessibilityService is available
            )
        }
    }

    // TreeParser is lazily initialized when accessibility service is available
    private var treeParser: AccessibilityTreeParser? = null
    private var isInitialized = false
    private var navigationHistory = mutableListOf<NavigationStep>()

    // Fast path intent types
    private val FAST_PATH_INTENTS = setOf(
        IntentType.CALL_CONTACT,
        IntentType.SEND_WHATSAPP,
        IntentType.SEND_VOICE_MESSAGE,
        IntentType.SEND_SMS,
        IntentType.SET_ALARM,
        IntentType.READ_TIME,
        IntentType.EMERGENCY,
        IntentType.OPEN_APP,
        IntentType.TOGGLE_WIFI,
        IntentType.TOGGLE_BLUETOOTH,
        IntentType.TOGGLE_FLASHLIGHT,
        IntentType.WEATHER_QUERY,
        IntentType.GREETING,
        IntentType.THANK_YOU,
        IntentType.GOODBYE
    )

    /**
     * Initialize the orchestrator.
     */
    fun initialize() {
        isInitialized = functionGemmaEngine.isReady() && (uiNavigationEngine?.isReady() ?: true)
        Log.i(TAG, "HybridOrchestrator initialized: $isInitialized")
    }

    /**
     * Process user command and route to appropriate path.
     * 
     * @param command Normalized Egyptian Arabic text
     * @return CommandResult with execution outcome
     */
    suspend fun processCommand(command: String): CommandResult = withContext(Dispatchers.Default) {
        Log.d(TAG, "Processing command: $command")
        
        if (!isInitialized) {
            Log.w(TAG, "Orchestrator not initialized, using fallback")
            return@withContext fallbackProcessing(command)
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            // Step 1: Classify intent with FunctionGemma
            val nluResult = functionGemmaEngine.classifyIntent(command)
            // Convert nlu.IntentResult to nlp.IntentResult
            val intentResult = convertToNlpIntentResult(nluResult)
            Log.d(TAG, "Intent classification: ${intentResult.intentType} (confidence: ${intentResult.confidence})")

            // Step 2: Make routing decision
            val routingDecision = makeRoutingDecision(intentResult, command)
            Log.d(TAG, "Routing decision: ${routingDecision.path} - ${routingDecision.reason}")

            // Step 3: Execute based on routing
            val result = when (routingDecision.path) {
                RoutingPath.FAST -> executeFastPath(intentResult, command)
                RoutingPath.SLOW -> executeSlowPath(command, routingDecision)
            }

            // Step 4: Record metrics
            val processingTime = System.currentTimeMillis() - startTime
            return@withContext result.copy(processingTimeMs = processingTime)

        } catch (e: Exception) {
            Log.e(TAG, "Error processing command", e)
            return@withContext CommandResult.failure("Processing failed: ${e.message}", processingTimeMs = System.currentTimeMillis() - startTime)
        }
    }

    /**
     * Convert nlu.IntentResult to nlp.IntentResult.
     */
    private fun convertToNlpIntentResult(nluResult: com.egyptian.agent.nlu.IntentResult): IntentResult {
        return IntentResult().apply {
            setIntentType(convertNluIntentType(nluResult.intentType))
            setConfidence(nluResult.confidence)
            nluResult.entities.forEach { (key, value) ->
                setEntity(key, value)
            }
        }
    }

    /**
     * Convert nlu.IntentType to nlp.IntentType.
     */
    private fun convertNluIntentType(nluType: com.egyptian.agent.nlu.IntentType): IntentType {
        return when (nluType) {
            com.egyptian.agent.nlu.IntentType.CALL_CONTACT -> IntentType.CALL_CONTACT
            com.egyptian.agent.nlu.IntentType.SEND_WHATSAPP -> IntentType.SEND_WHATSAPP
            com.egyptian.agent.nlu.IntentType.SEND_VOICE_MESSAGE -> IntentType.SEND_VOICE_MESSAGE
            com.egyptian.agent.nlu.IntentType.SEND_SMS -> IntentType.SEND_SMS
            com.egyptian.agent.nlu.IntentType.READ_MISSED_CALLS -> IntentType.READ_MISSED_CALLS
            com.egyptian.agent.nlu.IntentType.SET_ALARM -> IntentType.SET_ALARM
            com.egyptian.agent.nlu.IntentType.READ_TIME -> IntentType.READ_TIME
            com.egyptian.agent.nlu.IntentType.TOGGLE_WIFI -> IntentType.TOGGLE_WIFI
            com.egyptian.agent.nlu.IntentType.TOGGLE_BLUETOOTH -> IntentType.TOGGLE_BLUETOOTH
            com.egyptian.agent.nlu.IntentType.TOGGLE_FLASHLIGHT -> IntentType.TOGGLE_FLASHLIGHT
            com.egyptian.agent.nlu.IntentType.OPEN_APP -> IntentType.OPEN_APP
            com.egyptian.agent.nlu.IntentType.CLOSE_APP -> IntentType.CLOSE_APP
            com.egyptian.agent.nlu.IntentType.WEATHER_QUERY -> IntentType.WEATHER_QUERY
            com.egyptian.agent.nlu.IntentType.EMERGENCY -> IntentType.EMERGENCY
            com.egyptian.agent.nlu.IntentType.GREETING -> IntentType.GREETING
            com.egyptian.agent.nlu.IntentType.THANK_YOU -> IntentType.THANK_YOU
            com.egyptian.agent.nlu.IntentType.GOODBYE -> IntentType.GOODBYE
            else -> IntentType.UNKNOWN
        }
    }

    /**
     * Make routing decision based on intent and confidence.
     */
    private fun makeRoutingDecision(intentResult: IntentResult, command: String): RoutingDecision {
        val intentType = intentResult.intentType
        val confidence = intentResult.confidence
        
        // Rule 1: Clear intent with high confidence → FAST PATH
        if (FAST_PATH_INTENTS.contains(intentType) && confidence >= FAST_PATH_CONFIDENCE_THRESHOLD) {
            return RoutingDecision(
                path = RoutingPath.FAST,
                reason = "Clear intent (${intentType}) with high confidence ($confidence)",
                confidence = confidence,
                intentType = intentType,
                requiresUINavigation = false,
                uiContext = null
            )
        }
        
        // Rule 2: Low confidence → SLOW PATH
        if (confidence < SLOW_PATH_CONFIDENCE_THRESHOLD) {
            return RoutingDecision(
                path = RoutingPath.SLOW,
                reason = "Low confidence ($confidence < $SLOW_PATH_CONFIDENCE_THRESHOLD)",
                confidence = confidence,
                intentType = intentType,
                requiresUINavigation = true,
                uiContext = extractUIContext(command)
            )
        }
        
        // Rule 3: Unknown intent → SLOW PATH
        if (intentType == IntentType.UNKNOWN) {
            return RoutingDecision(
                path = RoutingPath.SLOW,
                reason = "Unknown intent, requires UI navigation",
                confidence = confidence,
                intentType = intentType,
                requiresUINavigation = true,
                uiContext = extractUIContext(command)
            )
        }
        
        // Rule 4: Intent requires UI interaction → SLOW PATH
        if (requiresUIInteraction(intentType, command)) {
            return RoutingDecision(
                path = RoutingPath.SLOW,
                reason = "Intent requires UI interaction",
                confidence = confidence,
                intentType = intentType,
                requiresUINavigation = true,
                uiContext = extractUIContext(command)
            )
        }
        
        // Default: FAST PATH
        return RoutingDecision(
            path = RoutingPath.FAST,
            reason = "Default fast path for ${intentType}",
            confidence = confidence,
            intentType = intentType,
            requiresUINavigation = false,
            uiContext = null
        )
    }

    /**
     * Check if intent type requires UI interaction.
     */
    private fun requiresUIInteraction(intentType: IntentType, command: String): Boolean {
        // Check for UI navigation keywords in Egyptian Arabic
        val uiKeywords = listOf(
            "شوف",      // check/see
            "افتح",     // open
            "اعمل",     // do/make
            "ابعت",     // send
            "اكتب",     // write
            "احجز",     // book
            "تصفح",     // browse
            "اقرا",     // read
            "الناس",    // people (social media)
            "الأخبار"   // news
        )
        
        return uiKeywords.any { command.contains(it, ignoreCase = true) }
    }

    /**
     * Extract UI context from command.
     */
    private fun extractUIContext(command: String): UIContext {
        // Extract target app from command
        val targetApp = extractTargetApp(command)
        
        // Extract expected UI elements
        val expectedElements = extractExpectedElements(command)
        
        // Determine if multi-step
        val isMultiStep = command.contains("و") ||  // "and" in Arabic
                         command.split(" ").size > 4 ||
                         command.contains("بعدين") ||  // "then"
                         command.contains("كمان")      // "also"
        
        return UIContext(
            targetApp = targetApp,
            expectedElements = expectedElements,
            isMultiStep = isMultiStep
        )
    }

    /**
     * Extract target app from command.
     */
    private fun extractTargetApp(command: String): String? {
        val appMappings = mapOf(
            "فيسبوك" to "com.facebook.katana",
            "واتساب" to "com.whatsapp",
            "يوتيوب" to "com.google.android.youtube",
            "انستجرام" to "com.instagram.android",
            "تويتر" to "com.twitter.android",
            "تيك توك" to "com.zhiliaoapp.musically",
            "أوبر" to "com.ubercab",
            "كريم" to "com.careem.acma",
            "خرائط" to "com.google.android.apps.maps",
            "جوجل" to "com.google.android.googlequicksearchbox"
        )
        
        for ((keyword, packageName) in appMappings) {
            if (command.contains(keyword, ignoreCase = true)) {
                return packageName
            }
        }
        
        return null
    }

    /**
     * Extract expected UI elements from command.
     */
    private fun extractExpectedElements(command: String): List<String> {
        val elementMappings = mapOf(
            "الأخبار" to listOf("news feed", "posts", "stories"),
            "رسالة" to listOf("message input", "send button", "contacts"),
            "بحث" to listOf("search bar", "search button"),
            "بروفايل" to listOf("profile", "settings", "edit"),
            "إشعارات" to listOf("notifications", "alerts")
        )
        
        for ((keyword, elements) in elementMappings) {
            if (command.contains(keyword, ignoreCase = true)) {
                return elements
            }
        }
        
        return emptyList()
    }

    /**
     * Execute fast path (intent-based).
     */
    private suspend fun executeFastPath(intentResult: IntentResult, command: String): CommandResult {
        Log.d(TAG, "Executing fast path for intent: ${intentResult.intentType}")
        
        // Use existing executor system
        // This would integrate with your existing CommandExecutor
        return CommandResult.success(
            message = "Fast path execution for ${intentResult.intentType}",
            intentType = intentResult.intentType,
            entities = intentResult.entities,
            routingPath = RoutingPath.FAST
        )
    }

    /**
     * Execute slow path (UI navigation).
     */
    private suspend fun executeSlowPath(command: String, routingDecision: RoutingDecision): CommandResult {
        Log.d(TAG, "Executing slow path (UI navigation) for: $command")
        
        navigationHistory.clear()
        var iterationCount = 0
        var stuckCount = 0
        var lastScreenHash = 0
        
        val goal = convertCommandToGoal(command)
        
        while (iterationCount < MAX_NAVIGATION_ITERATIONS) {
            iterationCount++

            // Perception: Get current screen state
            val currentTree = treeParser?.parseCurrentTree() ?: run {
                Log.w(TAG, "TreeParser not available, cannot perform UI navigation")
                return CommandResult.failure("Accessibility service not available", routingPath = RoutingPath.SLOW)
            }
            val currentScreenHash = currentTree.elements.hashCode()

            // Check if stuck (same screen for multiple iterations)
            if (currentScreenHash == lastScreenHash) {
                stuckCount++
                if (stuckCount >= STUCK_THRESHOLD) {
                    Log.w(TAG, "Stuck detected after $iterationCount iterations")
                    return handleStuckSituation(command, navigationHistory)
                }
            } else {
                stuckCount = 0
            }
            lastScreenHash = currentScreenHash

            // Reason: Determine next action based on goal and current state
            val nextAction = determineNextAction(goal, currentTree, navigationHistory)
            
            if (nextAction == null) {
                // Goal achieved or no valid action
                Log.d(TAG, "Goal achieved or no valid action at iteration $iterationCount")
                break
            }

            // Action: Execute the determined action
            val actionResult = uiNavigationEngine?.executeAction(nextAction) ?: run {
                Log.w(TAG, "UI Navigation Engine not available")
                return CommandResult.failure("UI navigation not available", routingPath = RoutingPath.SLOW)
            }

            // Record step in history
            navigationHistory.add(NavigationStep(iterationCount, nextAction, actionResult, currentTree))

            Log.d(TAG, "Iteration $iterationCount: ${nextAction.name} - ${actionResult.success}")

            // Check if action indicates completion
            if (nextAction is com.egyptian.agent.navigation.Done ||
                (actionResult.success && isGoalAchieved(goal, currentTree))) {
                Log.d(TAG, "Goal achieved at iteration $iterationCount")
                break
            }

            // Small delay for UI to update
            Thread.sleep(500)
        }
        
        if (iterationCount >= MAX_NAVIGATION_ITERATIONS) {
            Log.w(TAG, "Max iterations reached ($MAX_NAVIGATION_ITERATIONS)")
        }
        
        return CommandResult.success(
            message = "UI navigation completed in $iterationCount iterations",
            routingPath = RoutingPath.SLOW,
            iterations = iterationCount,
            navigationHistory = navigationHistory.toList()
        )
    }

    /**
     * Convert user command to navigation goal.
     */
    private fun convertCommandToGoal(command: String): String {
        // Simplified goal extraction
        // In production, this would use LLM to extract precise goal
        return command
    }

    /**
     * Determine next action based on goal and current state.
     */
    private fun determineNextAction(
        goal: String,
        currentTree: com.egyptian.agent.accessibility.ui.AccessibilityTree,
        history: List<NavigationStep>
    ): UIAction? {
        // Simplified action determination
        // In production, this would use LLM to reason about next action
        
        // Check if we need to launch an app first
        if (currentTree.packageName == "com.android.systemui" || 
            currentTree.packageName == "com.android.launcher3") {
            // We're on home screen, need to launch target app
            val targetApp = extractTargetApp(goal)
            if (targetApp != null) {
                return com.egyptian.agent.navigation.Launch(targetApp)
            }
        }
        
        // Look for elements matching goal keywords
        val goalKeywords = extractGoalKeywords(goal)
        for (keyword in goalKeywords) {
            val element = currentTree.getElementsByText(keyword).firstOrNull { it.isClickable }
            if (element != null) {
                return com.egyptian.agent.navigation.Tap(elementId = element.id)
            }
        }
        
        // Default: scroll to explore more content
        return com.egyptian.agent.navigation.Scroll(com.egyptian.agent.navigation.ScrollDirection.DOWN)
    }

    /**
     * Extract keywords from goal for matching.
     */
    private fun extractGoalKeywords(goal: String): List<String> {
        // Extract important keywords from Egyptian Arabic command
        val keywords = mutableListOf<String>()
        
        // Add common UI element keywords
        if (goal.contains("أخبار") || goal.contains("news")) keywords.add("news")
        if (goal.contains("بحث") || goal.contains("search")) keywords.add("search")
        if (goal.contains("رسالة") || goal.contains("message")) keywords.add("message")
        if (goal.contains("إرسال") || goal.contains("send")) keywords.add("send")
        
        return keywords.ifEmpty { listOf("home", "menu", "more") }
    }

    /**
     * Check if goal is achieved based on current screen.
     */
    private fun isGoalAchieved(goal: String, currentTree: com.egyptian.agent.accessibility.ui.AccessibilityTree): Boolean {
        // Simplified goal achievement check
        // In production, this would use LLM to verify goal completion
        
        val goalKeywords = extractGoalKeywords(goal)
        return currentTree.elements.any { element ->
            goalKeywords.any { keyword ->
                element.text?.contains(keyword, ignoreCase = true) == true ||
                element.contentDescription?.contains(keyword, ignoreCase = true) == true
            }
        }
    }

    /**
     * Handle stuck situation with recovery strategies.
     */
    private suspend fun handleStuckSituation(
        command: String,
        history: List<NavigationStep>
    ): CommandResult {
        Log.d(TAG, "Attempting recovery from stuck situation")

        // Recovery strategy 1: Go back and try different approach
        if (history.size > 2) {
            uiNavigationEngine?.executeAction(com.egyptian.agent.navigation.Back()) ?: run {
                return CommandResult.failure("UI navigation not available for recovery", routingPath = RoutingPath.SLOW)
            }
            Thread.sleep(500)

            // Retry with modified approach
            return executeSlowPath(command, RoutingDecision(
                path = RoutingPath.SLOW,
                reason = "Retry after stuck",
                confidence = 0.5f,
                intentType = null,
                requiresUINavigation = true,
                uiContext = extractUIContext(command)
            ))
        }

        // Recovery strategy 2: Go to home and restart
        uiNavigationEngine?.executeAction(com.egyptian.agent.navigation.Home())
        Thread.sleep(1000)

        return CommandResult.failure(
            "Could not complete task, returning to home screen",
            routingPath = RoutingPath.SLOW
        )
    }

    /**
     * Fallback processing when orchestrator is not initialized.
     */
    private suspend fun fallbackProcessing(command: String): CommandResult {
        Log.w(TAG, "Using fallback processing for: $command")

        // Use simple rule-based classification from nlu.EgyptianNormalizer
        val intentResult = com.egyptian.agent.nlu.EgyptianNormalizer.classifyBasicIntent(command)
        val nlpResult = IntentResult().apply {
            setIntentType(convertNluIntentType(intentResult.intentType))
            setConfidence(intentResult.confidence)
        }

        return CommandResult.success(
            message = "Fallback processing completed",
            intentType = nlpResult.intentType,
            entities = intentResult.entities,
            routingPath = RoutingPath.FAST
        )
    }

    /**
     * Java-compatible method to determine intent from command.
     * Wraps processCommand with callback for Java interop.
     *
     * @param command Normalized command text
     * @param callback Callback with IntentResult
     */
    @JvmOverloads
    fun determineIntent(command: String, callback: IntentResultCallback) {
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            try {
                val result = processCommand(command)
                val intentResult = IntentResult().apply {
                    setIntentType(result.intentType ?: IntentType.UNKNOWN)
                    setConfidence(if (result.success) 0.85f else 0.5f)
                    result.entities.forEach { (key, value) ->
                        setEntity(key, value)
                    }
                }
                callback.onResult(intentResult)
            } catch (e: Exception) {
                Log.e(TAG, "Error in determineIntent", e)
                callback.onResult(IntentResult().apply {
                    setIntentType(IntentType.UNKNOWN)
                    setConfidence(0.0f)
                })
            }
        }
    }

    /**
     * Java-compatible callback interface for intent results.
     */
    fun interface IntentResultCallback {
        fun onResult(result: IntentResult)
    }

    /**
     * Check if orchestrator is ready.
     */
    fun isReady(): Boolean = isInitialized

    /**
     * Get navigation history.
     */
    fun getNavigationHistory(): List<NavigationStep> = navigationHistory.toList()

    /**
     * Clear navigation history.
     */
    fun clearHistory() {
        navigationHistory.clear()
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        clearHistory()
        isInitialized = false
    }
}

/**
 * Routing path decision.
 */
sealed class RoutingPath {
    object FAST : RoutingPath()  // Intent-based (FunctionGemma)
    object SLOW : RoutingPath()  // UI Navigation (DroidClaw-style)
}

/**
 * Routing decision with reasoning.
 */
data class RoutingDecision(
    val path: RoutingPath,
    val reason: String,
    val confidence: Float,
    val intentType: IntentType?,
    val requiresUINavigation: Boolean,
    val uiContext: UIContext?
)

/**
 * UI context for navigation.
 */
data class UIContext(
    val targetApp: String?,
    val expectedElements: List<String>,
    val isMultiStep: Boolean
)

/**
 * Navigation step in history.
 */
data class NavigationStep(
    val iteration: Int,
    val action: UIAction,
    val result: ActionResult,
    val screenState: com.egyptian.agent.accessibility.ui.AccessibilityTree
)

/**
 * Command execution result.
 */
data class CommandResult(
    val success: Boolean,
    val message: String,
    val intentType: IntentType? = null,
    val entities: Map<String, String> = emptyMap(),
    val routingPath: RoutingPath,
    val processingTimeMs: Long = 0L,
    val iterations: Int = 0,
    val navigationHistory: List<NavigationStep> = emptyList(),
    val error: Throwable? = null
) {
    companion object {
        fun success(
            message: String,
            intentType: IntentType? = null,
            entities: Map<String, String> = emptyMap(),
            routingPath: RoutingPath = RoutingPath.FAST,
            iterations: Int = 0,
            navigationHistory: List<NavigationStep> = emptyList()
        ): CommandResult {
            return CommandResult(
                success = true,
                message = message,
                intentType = intentType,
                entities = entities,
                routingPath = routingPath,
                iterations = iterations,
                navigationHistory = navigationHistory
            )
        }

        fun failure(
            message: String,
            routingPath: RoutingPath = RoutingPath.FAST,
            error: Throwable? = null,
            processingTimeMs: Long = 0L
        ): CommandResult {
            return CommandResult(
                success = false,
                message = message,
                routingPath = routingPath,
                error = error,
                processingTimeMs = processingTimeMs
            )
        }
    }
}

// Extension property to get accessibility service from context
val Context.accessibilityService: android.accessibilityservice.AccessibilityService
    get() {
        // This would be properly injected in production
        throw IllegalStateException("AccessibilityService must be injected")
    }
