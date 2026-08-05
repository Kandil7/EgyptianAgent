package com.egyptian.agent.workflow

import android.content.Context
import com.egyptian.agent.navigation.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import com.google.common.truth.Truth.assertThat
import java.io.File

/**
 * Comprehensive unit tests for WorkflowEngine.
 * 
 * Target: >90% code coverage
 * 
 * @author EgyptianAgent Team
 * @version 1.0.0
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WorkflowEngineTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockUINavigationEngine: UINavigationEngine

    private val mockFilesDir = File("/tmp/test/files")

    private lateinit var workflowEngine: WorkflowEngine
    private lateinit var workflowsDir: File

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(mockContext.filesDir).thenReturn(mockFilesDir)
        
        // Create temp workflows directory
        workflowsDir = File(mockFilesDir, "workflows")
        workflowsDir.mkdirs()
        
        workflowEngine = WorkflowEngine(mockContext, mockUINavigationEngine)
    }

    // ========================================================================
    // YAML Parsing Tests
    // ========================================================================

    @Test
    fun `loadWorkflow parses simple workflow YAML`() {
        // Given
        val yamlContent = """
            appId: com.whatsapp
            name: Send Message
            description: Send a WhatsApp message
            ---
            - launchApp: com.whatsapp
            - wait: 2000
            - type: Hello
            - done: Completed
        """.trimIndent()

        // When
        val workflow = workflowEngine.loadWorkflow(yamlContent)

        // Then
        assertThat(workflow.id).isEqualTo("send_message")
        assertThat(workflow.appId).isEqualTo("com.whatsapp")
        assertThat(workflow.name).isEqualTo("Send Message")
        assertThat(workflow.steps).hasSize(4)
        assertThat(workflow.isPrebuilt).isFalse()
    }

    @Test
    fun `loadWorkflow parses workflow with parameters`() {
        // Given
        val yamlContent = """
            appId: com.google.android.youtube
            name: YouTube Search
            ---
            - launchApp: com.google.android.youtube
            - wait: 2000
            - findAndTap:
                search: Search
            - type:
                text: Egyptian news
            - done: Done
        """.trimIndent()

        // When
        val workflow = workflowEngine.loadWorkflow(yamlContent)

        // Then
        assertThat(workflow.name).isEqualTo("YouTube Search")
        assertThat(workflow.steps).hasSize(5)
    }

    @Test
    fun `loadWorkflow handles workflow without appId`() {
        // Given
        val yamlContent = """
            name: System Toggle
            description: Toggle system settings
            ---
            - shell: svc wifi enable
            - wait: 1000
            - done: WiFi enabled
        """.trimIndent()

        // When
        val workflow = workflowEngine.loadWorkflow(yamlContent)

        // Then
        assertThat(workflow.appId).isNull()
        assertThat(workflow.name).isEqualTo("System Toggle")
    }

    @Test(expected = WorkflowException::class)
    fun `loadWorkflow throws exception for invalid YAML`() {
        // Given
        val invalidYaml = "invalid: yaml: content: ["

        // When
        workflowEngine.loadWorkflow(invalidYaml)
    }

    // ========================================================================
    // Workflow Execution Tests
    // ========================================================================

    @Test
    fun `executeWorkflow runs all steps successfully`() = runTest {
        // Given
        val workflow = Workflow(
            id = "test_workflow",
            appId = null,
            name = "Test Workflow",
            steps = listOf(
                WorkflowStep("wait", mapOf("value" to "100")),
                WorkflowStep("done", mapOf("message" to "Completed"))
            ),
            isPrebuilt = false
        )

        `when`(mockUINavigationEngine.executeAction(org.mockito.ArgumentMatchers.any()))
            .thenReturn(ActionResult.success("Success"))

        // When
        val result = workflowEngine.executeWorkflow(workflow)

        // Then
        assertThat(result.workflowId).isEqualTo("test_workflow")
        assertThat(result.success).isTrue()
    }

    @Test
    fun `executeWorkflow with app launch launches app first`() = runTest {
        // Given
        val workflow = Workflow(
            id = "app_workflow",
            appId = "com.whatsapp",
            name = "WhatsApp Workflow",
            steps = listOf(
                WorkflowStep("wait", mapOf("value" to "1000")),
                WorkflowStep("done", mapOf("message" to "Done"))
            ),
            isPrebuilt = false
        )

        `when`(mockUINavigationEngine.executeAction(org.mockito.ArgumentMatchers.any()))
            .thenReturn(ActionResult.success("Success"))

        // When
        val result = workflowEngine.executeWorkflow(workflow)

        // Then
        assertThat(result.workflowId).isEqualTo("app_workflow")
    }

    @Test
    fun `executeWorkflow handles step failure`() = runTest {
        // Given
        val workflow = Workflow(
            id = "failing_workflow",
            appId = null,
            name = "Failing Workflow",
            steps = listOf(
                WorkflowStep("wait", mapOf("value" to "100")),
                WorkflowStep("tap", mapOf("value" to "button"))
            ),
            isPrebuilt = false
        )

        `when`(mockUINavigationEngine.executeAction(org.mockito.ArgumentMatchers.any()))
            .thenReturn(ActionResult.failure("Failed"))

        // When
        val result = workflowEngine.executeWorkflow(workflow)

        // Then
        assertThat(result.success).isFalse()
    }

    @Test
    fun `executeWorkflow with optional step continues on failure`() = runTest {
        // Given
        val workflow = Workflow(
            id = "optional_workflow",
            appId = null,
            name = "Optional Step Workflow",
            steps = listOf(
                WorkflowStep("wait", mapOf("value" to "100"), isOptional = true),
                WorkflowStep("done", mapOf("message" to "Completed"))
            ),
            isPrebuilt = false
        )

        `when`(mockUINavigationEngine.executeAction(org.mockito.ArgumentMatchers.any()))
            .thenReturn(ActionResult.failure("Failed"))

        // When
        val result = workflowEngine.executeWorkflow(workflow)

        // Then - should continue despite failure due to optional step
        assertThat(result.stepResults).hasSize(2)
    }

    // ========================================================================
    // Pre-built Workflows Tests
    // ========================================================================

    @Test
    fun `getPrebuiltWorkflow returns morning routine workflow`() {
        // When
        val workflow = workflowEngine.getPrebuiltWorkflow("morning_routine")

        // Then
        assertThat(workflow).isNotNull()
        assertThat(workflow?.id).isEqualTo("morning_routine")
        assertThat(workflow?.name).isEqualTo("Morning Routine")
        assertThat(workflow?.isPrebuilt).isTrue()
        assertThat(workflow?.steps).isNotEmpty()
    }

    @Test
    fun `getPrebuiltWorkflow returns morning routine by Arabic name`() {
        // When
        val workflow = workflowEngine.getPrebuiltWorkflow("روتين_الصباح")

        // Then
        assertThat(workflow).isNotNull()
        assertThat(workflow?.id).isEqualTo("morning_routine")
    }

    @Test
    fun `getPrebuiltWorkflow returns coming home workflow`() {
        // When
        val workflow = workflowEngine.getPrebuiltWorkflow("coming_home")

        // Then
        assertThat(workflow).isNotNull()
        assertThat(workflow?.id).isEqualTo("coming_home")
        assertThat(workflow?.name).isEqualTo("Coming Home")
    }

    @Test
    fun `getPrebuiltWorkflow returns bedtime workflow`() {
        // When
        val workflow = workflowEngine.getPrebuiltWorkflow("bedtime")

        // Then
        assertThat(workflow).isNotNull()
        assertThat(workflow?.id).isEqualTo("bedtime")
        assertThat(workflow?.name).isEqualTo("Bedtime")
    }

    @Test
    fun `getPrebuiltWorkflow returns null for unknown workflow`() {
        // When
        val workflow = workflowEngine.getPrebuiltWorkflow("unknown_workflow")

        // Then
        assertThat(workflow).isNull()
    }

    // ========================================================================
    // Variable Substitution Tests
    // ========================================================================

    @Test
    fun `executeWorkflow substitutes variables in steps`() = runTest {
        // Given
        val workflow = Workflow(
            id = "variable_workflow",
            appId = null,
            name = "Variable Workflow",
            steps = listOf(
                WorkflowStep("type", mapOf("text" to "\${message}")),
                WorkflowStep("done", mapOf("message" to "Sent \${message}"))
            ),
            isPrebuilt = false
        )

        val variables = mapOf("message" to "Hello, World!")

        `when`(mockUINavigationEngine.executeAction(org.mockito.ArgumentMatchers.any()))
            .thenReturn(ActionResult.success("Success"))

        // When
        val result = workflowEngine.executeWorkflow(workflow, variables)

        // Then
        assertThat(result.success).isTrue()
    }

    // ========================================================================
    // Workflow Save and Load Tests
    // ========================================================================

    @Test
    fun `saveWorkflow saves workflow to file`() {
        // Given
        val workflow = Workflow(
            id = "saved_workflow",
            appId = "com.test.app",
            name = "Saved Workflow",
            description = "Test workflow",
            steps = listOf(
                WorkflowStep("launchApp", mapOf("value" to "com.test.app")),
                WorkflowStep("done", mapOf("message" to "Done"))
            ),
            isPrebuilt = false
        )

        // When
        val file = workflowEngine.saveWorkflow(workflow)

        // Then
        assertThat(file.exists()).isTrue()
        assertThat(file.name).isEqualTo("saved_workflow.yaml")
    }

    @Test
    fun `loadWorkflowFromFile loads saved workflow`() {
        // Given
        val workflow = Workflow(
            id = "file_workflow",
            appId = "com.test.app",
            name = "File Workflow",
            steps = listOf(
                WorkflowStep("wait", mapOf("value" to "1000"))
            ),
            isPrebuilt = false
        )

        val file = workflowEngine.saveWorkflow(workflow)

        // When
        val loadedWorkflow = workflowEngine.loadWorkflowFromFile(file.name)

        // Then
        assertThat(loadedWorkflow.id).isEqualTo("file_workflow")
        assertThat(loadedWorkflow.name).isEqualTo("File Workflow")
    }

    @Test(expected = WorkflowException::class)
    fun `loadWorkflowFromFile throws exception for missing file`() {
        // When
        workflowEngine.loadWorkflowFromFile("nonexistent.yaml")
    }

    // ========================================================================
    // Workflow List and Delete Tests
    // ========================================================================

    @Test
    fun `listWorkflows returns saved workflows`() {
        // Given
        val workflow1 = Workflow(
            id = "list_workflow_1",
            appId = null,
            name = "List Workflow 1",
            steps = listOf(WorkflowStep("wait", mapOf("value" to "100"))),
            isPrebuilt = false
        )

        val workflow2 = Workflow(
            id = "list_workflow_2",
            appId = null,
            name = "List Workflow 2",
            steps = listOf(WorkflowStep("wait", mapOf("value" to "100"))),
            isPrebuilt = false
        )

        workflowEngine.saveWorkflow(workflow1)
        workflowEngine.saveWorkflow(workflow2)

        // When
        val workflows = workflowEngine.listWorkflows()

        // Then
        assertThat(workflows).hasSize(2)
        assertThat(workflows.map { it.id }).containsExactly("list_workflow_1", "list_workflow_2")
    }

    @Test
    fun `deleteWorkflow removes workflow file`() {
        // Given
        val workflow = Workflow(
            id = "delete_workflow",
            appId = null,
            name = "Delete Workflow",
            steps = listOf(WorkflowStep("wait", mapOf("value" to "100"))),
            isPrebuilt = false
        )

        workflowEngine.saveWorkflow(workflow)

        // When
        val deleted = workflowEngine.deleteWorkflow("delete_workflow")

        // Then
        assertThat(deleted).isTrue()
    }

    @Test
    fun `deleteWorkflow returns false for non-existent workflow`() {
        // When
        val deleted = workflowEngine.deleteWorkflow("nonexistent")

        // Then
        assertThat(deleted).isFalse()
    }

    // ========================================================================
    // Step Creation Tests
    // ========================================================================

    @Test
    fun `WorkflowStep with empty parameters creates valid step`() {
        // When
        val step = WorkflowStep("wait", emptyMap())

        // Then
        assertThat(step.action).isEqualTo("wait")
        assertThat(step.parameters).isEmpty()
        assertThat(step.isOptional).isFalse()
        assertThat(step.retryCount).isEqualTo(0)
    }

    @Test
    fun `WorkflowStep with delay creates valid step`() {
        // When
        val step = WorkflowStep("tap", mapOf("value" to "button"), delay = 1000L)

        // Then
        assertThat(step.action).isEqualTo("tap")
        assertThat(step.delay).isEqualTo(1000L)
    }

    @Test
    fun `WorkflowStep with retry creates valid step`() {
        // When
        val step = WorkflowStep("findAndTap", mapOf("search" to "Submit"), retryCount = 3)

        // Then
        assertThat(step.action).isEqualTo("findAndTap")
        assertThat(step.retryCount).isEqualTo(3)
    }

    // ========================================================================
    // Workflow Result Tests
    // ========================================================================

    @Test
    fun `WorkflowResult success creates valid result`() {
        // Given
        val stepResult = StepResult(
            step = WorkflowStep("wait", mapOf("value" to "100")),
            success = true,
            message = "Waited",
            actionResult = ActionResult.success("Success")
        )

        // When
        val result = WorkflowResult(
            workflowId = "test",
            workflowName = "Test Workflow",
            success = true,
            message = "Completed",
            stepResults = listOf(stepResult),
            executionTimeMs = 1000L,
            variables = emptyMap()
        )

        // Then
        assertThat(result.success).isTrue()
        assertThat(result.message).isEqualTo("Completed")
        assertThat(result.stepResults).hasSize(1)
    }

    @Test
    fun `WorkflowResult failure factory creates valid result`() {
        // When
        val result = WorkflowResult.failure(
            workflowId = "failed",
            message = "Failed to execute",
            stepResults = emptyList()
        )

        // Then
        assertThat(result.success).isFalse()
        assertThat(result.message).isEqualTo("Failed to execute")
    }

    @Test
    fun `StepResult creates valid result`() {
        // Given
        val step = WorkflowStep("tap", mapOf("value" to "button"))
        val actionResult = ActionResult.success("Tapped")

        // When
        val result = StepResult(
            step = step,
            success = true,
            message = "Tapped successfully",
            actionResult = actionResult
        )

        // Then
        assertThat(result.step).isEqualTo(step)
        assertThat(result.success).isTrue()
        assertThat(result.actionResult).isEqualTo(actionResult)
    }

    @Test
    fun `WorkflowInfo creates valid info object`() {
        // When
        val info = WorkflowInfo(
            id = "info_workflow",
            name = "Info Workflow",
            description = "Test description",
            isPrebuilt = false,
            stepCount = 5
        )

        // Then
        assertThat(info.id).isEqualTo("info_workflow")
        assertThat(info.name).isEqualTo("Info Workflow")
        assertThat(info.stepCount).isEqualTo(5)
    }

    @Test
    fun `WorkflowException creates valid exception`() {
        // When
        val exception = WorkflowException("Test error")

        // Then
        assertThat(exception.message).isEqualTo("Test error")
    }

    @Test
    fun `WorkflowException with cause creates valid exception`() {
        // Given
        val cause = RuntimeException("Cause")

        // When
        val exception = WorkflowException("Test error", cause)

        // Then
        assertThat(exception.message).isEqualTo("Test error")
        assertThat(exception.cause).isEqualTo(cause)
    }
}
