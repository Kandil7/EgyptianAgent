package com.egyptian.agent.workflow

import android.content.Context
import android.util.Log
import com.egyptian.agent.navigation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import java.io.File
import java.io.FileReader

/**
 * Workflow Engine for EgyptianAgent.
 * 
 * Executes YAML-based deterministic workflows (inspired by DroidClaw Flows).
 * Supports pre-built workflows and user-creatable workflows via voice.
 *
 * Features:
 * - YAML workflow parsing
 * - Deterministic execution (no LLM)
 * - Pre-built workflows (morning routine, coming home, bedtime)
 * - Variable substitution
 * - Error handling and recovery
 *
 * @author EgyptianAgent Team
 * @version 1.0.0
 */
class WorkflowEngine(
    private val context: Context,
    private val uiNavigationEngine: UINavigationEngine
) {
    companion object {
        private const val TAG = "WorkflowEngine"
        private const val WORKFLOWS_DIR = "workflows"
    }

    private val yaml = Yaml()
    private val workflowsDir = File(context.filesDir, WORKFLOWS_DIR)

    init {
        // Create workflows directory if not exists
        if (!workflowsDir.exists()) {
            workflowsDir.mkdirs()
        }
    }

    /**
     * Load workflow from YAML string.
     */
    fun loadWorkflow(yamlContent: String): Workflow {
        try {
            val workflowMap = yaml.load<Map<String, Any>>(yamlContent)
            
            val appId = workflowMap["appId"] as? String
            val name = workflowMap["name"] as? String ?: "Unnamed Workflow"
            val description = workflowMap["description"] as? String
            val stepsRaw = workflowMap["steps"] as? List<Any>
            
            val steps = stepsRaw?.map { step -> parseStep(step) } ?: emptyList()
            
            return Workflow(
                id = generateWorkflowId(name),
                appId = appId,
                name = name,
                description = description,
                steps = steps,
                isPrebuilt = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing workflow YAML", e)
            throw WorkflowException("Failed to parse workflow: ${e.message}", e)
        }
    }

    /**
     * Load workflow from file.
     */
    fun loadWorkflowFromFile(filename: String): Workflow {
        val file = File(workflowsDir, filename)
        if (!file.exists()) {
            throw WorkflowException("Workflow file not found: $filename")
        }
        
        val yamlContent = FileReader(file).readText()
        return loadWorkflow(yamlContent)
    }

    /**
     * Execute workflow.
     */
    suspend fun executeWorkflow(
        workflow: Workflow,
        variables: Map<String, String> = emptyMap()
    ): WorkflowResult = withContext(Dispatchers.Default) {
        Log.d(TAG, "Executing workflow: ${workflow.name}")
        
        val stepResults = mutableListOf<StepResult>()
        var success = true
        var errorMessage: String? = null
        
        try {
            // Launch app if specified
            if (workflow.appId != null) {
                val launchResult = uiNavigationEngine.executeAction(Launch(workflow.appId))
                if (!launchResult.success) {
                    return@withContext WorkflowResult.failure(
                        workflow.id,
                        "Failed to launch app: ${workflow.appId}",
                        stepResults
                    )
                }
                Thread.sleep(1000) // Wait for app to launch
            }
            
            // Execute each step
            for ((index, step) in workflow.steps.withIndex()) {
                Log.d(TAG, "Executing step ${index + 1}/${workflow.steps.size}: ${step.action}")
                
                val substitutedStep = substituteVariables(step, variables)
                val result = executeStep(substitutedStep)
                
                stepResults.add(result)
                
                if (!result.success) {
                    success = false
                    errorMessage = "Step ${index + 1} failed: ${result.message}"
                    
                    // Check if step is optional
                    if (!step.isOptional) {
                        break
                    }
                }
                
                // Wait between steps
                if (index < workflow.steps.lastIndex) {
                    Thread.sleep(step.delay ?: 500)
                }
            }
            
            WorkflowResult(
                workflowId = workflow.id,
                workflowName = workflow.name,
                success = success,
                message = if (success) "Workflow completed successfully" else (errorMessage ?: "Unknown error"),
                stepResults = stepResults,
                executionTimeMs = 0, // Will be set by caller
                variables = variables
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error executing workflow", e)
            WorkflowResult.failure(
                workflow.id,
                "Workflow execution failed: ${e.message}",
                stepResults
            )
        }
    }

    /**
     * Parse a step from YAML representation.
     */
    private fun parseStep(stepRaw: Any): WorkflowStep {
        return when (stepRaw) {
            is String -> parseSimpleStep(stepRaw)
            is Map<*, *> -> parseComplexStep(stepRaw)
            else -> throw WorkflowException("Invalid step format: $stepRaw")
        }
    }

    /**
     * Parse simple step (action only).
     */
    private fun parseSimpleStep(action: String): WorkflowStep {
        return WorkflowStep(
            action = action,
            parameters = emptyMap(),
            isOptional = false,
            delay = null,
            retryCount = 0
        )
    }

    /**
     * Parse complex step (action + parameters).
     */
    private fun parseComplexStep(stepMap: Map<*, *>): WorkflowStep {
        val action = stepMap.entries.firstOrNull()?.key as? String 
            ?: throw WorkflowException("Step must have an action")
        
        val value = stepMap.values.first()
        
        // Check for additional parameters
        val parameters = mutableMapOf<String, String>()
        var isOptional = false
        var delay: Long? = null
        var retryCount = 0
        
        if (value is Map<*, *>) {
            // Complex step with parameters
            for ((key, paramValue) in value) {
                when (key) {
                    "text", "message", "target", "search" -> {
                        parameters[key as String] = paramValue.toString()
                    }
                    "optional" -> {
                        isOptional = paramValue as? Boolean ?: false
                    }
                    "delay" -> {
                        delay = (paramValue as? Number)?.toLong()
                    }
                    "retry" -> {
                        retryCount = (paramValue as? Number)?.toInt() ?: 0
                    }
                    else -> {
                        parameters[key as String] = paramValue.toString()
                    }
                }
            }
        } else if (value != null) {
            // Simple step with value
            parameters["value"] = value.toString()
        }
        
        return WorkflowStep(
            action = action,
            parameters = parameters,
            isOptional = isOptional,
            delay = delay,
            retryCount = retryCount
        )
    }

    /**
     * Execute a single workflow step.
     */
    private suspend fun executeStep(step: WorkflowStep): StepResult {
        var lastError: String? = null
        
        // Retry loop
        for (attempt in 0..step.retryCount) {
            try {
                val action = createActionFromStep(step)
                val result = uiNavigationEngine.executeAction(action)
                
                return StepResult(
                    step = step,
                    success = result.success,
                    message = result.message,
                    actionResult = result
                )
            } catch (e: Exception) {
                lastError = e.message
                if (attempt < step.retryCount) {
                    Thread.sleep(1000) // Wait before retry
                }
            }
        }
        
        return StepResult(
            step = step,
            success = false,
            message = lastError ?: "Unknown error",
            actionResult = ActionResult.failure(lastError ?: "Unknown error")
        )
    }

    /**
     * Create UI action from workflow step.
     */
    private fun createActionFromStep(step: WorkflowStep): UIAction {
        val action = step.action.lowercase()
        val value = step.parameters["value"]
        val text = step.parameters["text"]
        val message = step.parameters["message"]
        val search = step.parameters["search"]
        val target = step.parameters["target"]
        
        return when (action) {
            "launchapp", "launch" -> Launch(value ?: "")
            "wait" -> Wait((value?.toLongOrNull() ?: 1000))
            "tap" -> Tap(elementId = value, descriptionText = "Tap $value")
            "type" -> Type(text = text ?: value ?: "", targetId = target)
            "swipe" -> createSwipeFromValue(value)
            "scroll" -> Scroll(parseScrollDirection(value))
            "back" -> Back()
            "home" -> Home()
            "findandtap", "find_and_tap" -> FindAndTap(searchText = search ?: value ?: "")
            "submitmessage", "submit_message" -> SubmitMessage(message = message ?: text ?: "")
            "readscreen", "read_screen" -> ReadScreen()
            "done" -> Done(message = value ?: "Workflow completed")
            "clear" -> Clear(targetId = target)
            "enter" -> Enter(targetId = target)
            "longpress" -> LongPress(elementId = value)
            "paste" -> Paste(targetId = target)
            "clipboardset", "clipboard_set" -> ClipboardSet(text = text ?: value ?: "")
            "openurl", "open_url" -> OpenUrl(url = value ?: "")
            "notifications" -> Notifications()
            "screenshot" -> Screenshot()
            "keyevent", "key_event" -> KeyEvent(parseKeyCode(value))
            else -> throw WorkflowException("Unknown action: $action")
        }
    }

    /**
     * Create swipe action from value.
     */
    private fun createSwipeFromValue(value: String?): Swipe {
        return when (value?.lowercase()) {
            "up" -> Swipe(
                startX = 500, startY = 700, endX = 500, endY = 300
            )
            "down" -> Swipe(
                startX = 500, startY = 300, endX = 500, endY = 700
            )
            "left" -> Swipe(
                startX = 700, startY = 500, endX = 300, endY = 500
            )
            "right" -> Swipe(
                startX = 300, startY = 500, endX = 700, endY = 500
            )
            else -> Swipe(
                startX = 500, startY = 700, endX = 500, endY = 300
            ) // Default: scroll down
        }
    }

    /**
     * Parse scroll direction from string.
     */
    private fun parseScrollDirection(value: String?): ScrollDirection {
        return when (value?.lowercase()) {
            "up" -> ScrollDirection.UP
            "down" -> ScrollDirection.DOWN
            "left" -> ScrollDirection.LEFT
            "right" -> ScrollDirection.RIGHT
            else -> ScrollDirection.DOWN
        }
    }

    /**
     * Parse key code from string.
     */
    private fun parseKeyCode(value: String?): Int {
        return when (value?.lowercase()) {
            "home" -> UIActions.KeyCodes.KEYCODE_HOME
            "back" -> UIActions.KeyCodes.KEYCODE_BACK
            "enter" -> UIActions.KeyCodes.KEYCODE_ENTER
            "menu" -> UIActions.KeyCodes.KEYCODE_MENU
            "power" -> UIActions.KeyCodes.KEYCODE_POWER
            "volume_up" -> UIActions.KeyCodes.KEYCODE_VOLUME_UP
            "volume_down" -> UIActions.KeyCodes.KEYCODE_VOLUME_DOWN
            else -> UIActions.KeyCodes.KEYCODE_ENTER
        }
    }

    /**
     * Substitute variables in step parameters.
     */
    private fun substituteVariables(step: WorkflowStep, variables: Map<String, String>): WorkflowStep {
        val substitutedParams = step.parameters.mapValues { (_, value) ->
            var substituted = value
            for ((key, replacement) in variables) {
                substituted = substituted.replace("\${$key}", replacement)
                    .replace("{{$key}}", replacement)
            }
            substituted
        }
        
        return step.copy(parameters = substitutedParams)
    }

    /**
     * Generate workflow ID from name.
     */
    private fun generateWorkflowId(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9]"), "_")
            .take(50)
    }

    /**
     * Save workflow to file.
     */
    fun saveWorkflow(workflow: Workflow): File {
        val filename = "${workflow.id}.yaml"
        val file = File(workflowsDir, filename)
        
        val yamlContent = buildString {
            appendLine("appId: ${workflow.appId ?: "null"}")
            appendLine("name: ${workflow.name}")
            if (workflow.description != null) {
                appendLine("description: ${workflow.description}")
            }
            appendLine("---")
            for (step in workflow.steps) {
                appendLine("- ${step.action}: ${formatStepValue(step)}")
            }
        }
        
        file.writeText(yamlContent)
        return file
    }

    /**
     * Format step value for YAML output.
     */
    private fun formatStepValue(step: WorkflowStep): String {
        if (step.parameters.isEmpty()) {
            return step.action
        }
        
        val value = step.parameters["value"] ?: step.parameters["text"] ?: ""
        return if (step.parameters.size == 1) {
            value
        } else {
            step.parameters.entries.joinToString(", ") { "${it.key}=${it.value}" }
        }
    }

    /**
     * List all saved workflows.
     */
    fun listWorkflows(): List<WorkflowInfo> {
        return workflowsDir.listFiles { file -> file.extension == "yaml" }
            ?.map { file ->
                val workflow = loadWorkflowFromFile(file.name)
                WorkflowInfo(
                    id = workflow.id,
                    name = workflow.name,
                    description = workflow.description,
                    isPrebuilt = workflow.isPrebuilt,
                    stepCount = workflow.steps.size
                )
            } ?: emptyList()
    }

    /**
     * Delete workflow.
     */
    fun deleteWorkflow(workflowId: String): Boolean {
        val file = File(workflowsDir, "$workflowId.yaml")
        return file.delete()
    }

    /**
     * Get pre-built workflow by name.
     */
    fun getPrebuiltWorkflow(name: String): Workflow? {
        return when (name.lowercase()) {
            "morning_routine", "روتين_الصباح" -> createMorningRoutineWorkflow()
            "coming_home", "رجوع_البيت" -> createComingHomeWorkflow()
            "bedtime", "وقت_النوم" -> createBedtimeWorkflow()
            else -> null
        }
    }

    // ========================================================================
    // Pre-built Workflows
    // ========================================================================

    /**
     * Morning Routine Workflow (روتين الصباح).
     * Checks weather, news, and sends morning message.
     */
    private fun createMorningRoutineWorkflow(): Workflow {
        return Workflow(
            id = "morning_routine",
            appId = "com.google.android.googlequicksearchbox",
            name = "Morning Routine",
            nameAr = "روتين الصباح",
            description = "Check weather, news, and send morning message",
            descriptionAr = "فحص الطقس والأخبار وإرسال رسالة صباحية",
            steps = listOf(
                WorkflowStep("launchApp", mapOf("value" to "com.google.android.googlequicksearchbox")),
                WorkflowStep("wait", mapOf("value" to "2000")),
                WorkflowStep("type", mapOf("text" to "أخبار مصر اليوم")),
                WorkflowStep("enter", emptyMap()),
                WorkflowStep("wait", mapOf("value" to "3000")),
                WorkflowStep("readScreen", emptyMap()),
                WorkflowStep("back", emptyMap()),
                WorkflowStep("launchApp", mapOf("value" to "com.whatsapp")),
                WorkflowStep("wait", mapOf("value" to "2000")),
                WorkflowStep("findAndTap", mapOf("search" to "ماما")),
                WorkflowStep("wait", mapOf("value" to "1000")),
                WorkflowStep("submitMessage", mapOf("message" to "صباح الخير يا حبيبتي")),
                WorkflowStep("done", mapOf("message" to "تم روتين الصباح"))
            ),
            isPrebuilt = true
        )
    }

    /**
     * Coming Home Workflow (رجوع البيت).
     * Turns on WiFi, AC, lights.
     */
    private fun createComingHomeWorkflow(): Workflow {
        return Workflow(
            id = "coming_home",
            appId = null,
            name = "Coming Home",
            nameAr = "رجوع البيت",
            description = "Turn on WiFi, AC, and lights when arriving home",
            descriptionAr = "تشغيل الواي فاي والتكييف والإضاءة عند الوصول للبيت",
            steps = listOf(
                WorkflowStep("shell", mapOf("value" to "svc wifi enable"), isOptional = true),
                WorkflowStep("wait", mapOf("value" to "1000")),
                WorkflowStep("launchApp", mapOf("value" to "com.google.android.apps.home")),
                WorkflowStep("wait", mapOf("value" to "2000")),
                WorkflowStep("findAndTap", mapOf("search" to "AC")),
                WorkflowStep("wait", mapOf("value" to "500")),
                WorkflowStep("findAndTap", mapOf("search" to "on")),
                WorkflowStep("wait", mapOf("value" to "1000")),
                WorkflowStep("findAndTap", mapOf("search" to "Lights")),
                WorkflowStep("wait", mapOf("value" to "500")),
                WorkflowStep("findAndTap", mapOf("search" to "on")),
                WorkflowStep("done", mapOf("message" to "أهلاً بك في البيت"))
            ),
            isPrebuilt = true
        )
    }

    /**
     * Bedtime Workflow (وقت النوم).
     * Sets alarms, turns off lights, enables DND.
     */
    private fun createBedtimeWorkflow(): Workflow {
        return Workflow(
            id = "bedtime",
            appId = "com.google.android.deskclock",
            name = "Bedtime",
            nameAr = "وقت النوم",
            description = "Set alarms, turn off lights, enable Do Not Disturb",
            descriptionAr = "ضبط المنبهات وإطفاء الإضاءة وتفعيل وضع عدم الإزعاج",
            steps = listOf(
                WorkflowStep("launchApp", mapOf("value" to "com.google.android.deskclock")),
                WorkflowStep("wait", mapOf("value" to "1000")),
                WorkflowStep("findAndTap", mapOf("search" to "Add alarm")),
                WorkflowStep("wait", mapOf("value" to "500")),
                WorkflowStep("type", mapOf("text" to "07:00")),
                WorkflowStep("tap", mapOf("value" to "Save")),
                WorkflowStep("wait", mapOf("value" to "500")),
                WorkflowStep("home", emptyMap()),
                WorkflowStep("shell", mapOf("value" to "cmd media_control set_dnd_mode priority"), isOptional = true),
                WorkflowStep("done", mapOf("message" to "تصبح على خير"))
            ),
            isPrebuilt = true
        )
    }
}

/**
 * Workflow data class.
 */
data class Workflow(
    val id: String,
    val appId: String?,
    val name: String,
    val nameAr: String? = null,
    val description: String? = null,
    val descriptionAr: String? = null,
    val steps: List<WorkflowStep>,
    val isPrebuilt: Boolean = false
)

/**
 * Workflow step data class.
 */
data class WorkflowStep(
    val action: String,
    val parameters: Map<String, String>,
    val isOptional: Boolean = false,
    val delay: Long? = null,
    val retryCount: Int = 0
)

/**
 * Workflow execution result.
 */
data class WorkflowResult(
    val workflowId: String,
    val workflowName: String,
    val success: Boolean,
    val message: String,
    val stepResults: List<StepResult>,
    val executionTimeMs: Long,
    val variables: Map<String, String> = emptyMap()
) {
    companion object {
        fun failure(workflowId: String, message: String, stepResults: List<StepResult>): WorkflowResult {
            return WorkflowResult(
                workflowId = workflowId,
                workflowName = "",
                success = false,
                message = message,
                stepResults = stepResults,
                executionTimeMs = 0
            )
        }
    }
}

/**
 * Step execution result.
 */
data class StepResult(
    val step: WorkflowStep,
    val success: Boolean,
    val message: String,
    val actionResult: ActionResult
)

/**
 * Workflow info for listing.
 */
data class WorkflowInfo(
    val id: String,
    val name: String,
    val description: String?,
    val isPrebuilt: Boolean,
    val stepCount: Int
)

/**
 * Workflow exception.
 */
class WorkflowException(message: String, cause: Throwable? = null) : Exception(message, cause)
