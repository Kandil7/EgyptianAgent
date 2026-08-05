package com.egyptian.agent.hybrid

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ServiceTestRule
import com.egyptian.agent.accessibility.EgyptianAccessibilityService
import com.egyptian.agent.navigation.UINavigationEngine
import com.egyptian.agent.workflow.Workflow
import com.egyptian.agent.workflow.WorkflowEngine
import com.egyptian.agent.workflow.WorkflowStep
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.concurrent.TimeoutException

/**
 * Integration tests for Workflow Engine on real device.
 * 
 * Tests pre-built workflow execution, custom workflow creation,
 * and multi-step automation validation.
 * 
 * @author EgyptianAgent Team
 * @version 1.0.0
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class WorkflowIntegrationTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private lateinit var context: Context
    private lateinit var accessibilityService: AccessibilityService
    private lateinit var workflowEngine: WorkflowEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        try {
            val serviceIntent = Intent(context, EgyptianAccessibilityService::class.java)
            accessibilityService = serviceRule.bindService(serviceIntent) as AccessibilityService
        } catch (e: TimeoutException) {
            // Service binding may fail in test environment
        }

        val uiNavigationEngine = UINavigationEngine(accessibilityService, context)
        workflowEngine = WorkflowEngine(context, uiNavigationEngine)
    }

    @After
    fun teardown() {
        // Cleanup temporary workflow files
    }

    // ========================================================================
    // Pre-built Workflow Execution Tests
    // ========================================================================

    @Test
    fun `execute morning routine pre-built workflow`() = runBlocking {
        // Given
        val workflow = workflowEngine.getPrebuiltWorkflow("morning_routine")
        assertThat(workflow).isNotNull()

        // When
        val result = workflowEngine.executeWorkflow(workflow!!)

        // Then
        assertThat(result.workflowId).isEqualTo("morning_routine")
        assertThat(result.workflowName).isEqualTo("Morning Routine")
    }

    @Test
    fun `execute coming home pre-built workflow`() = runBlocking {
        // Given
        val workflow = workflowEngine.getPrebuiltWorkflow("coming_home")
        assertThat(workflow).isNotNull()

        // When
        val result = workflowEngine.executeWorkflow(workflow!!)

        // Then
        assertThat(result.workflowId).isEqualTo("coming_home")
    }

    @Test
    fun `execute bedtime pre-built workflow`() = runBlocking {
        // Given
        val workflow = workflowEngine.getPrebuiltWorkflow("bedtime")
        assertThat(workflow).isNotNull()

        // When
        val result = workflowEngine.executeWorkflow(workflow!!)

        // Then
        assertThat(result.workflowId).isEqualTo("bedtime")
        assertThat(result.workflowName).isEqualTo("Bedtime")
    }

    // ========================================================================
    // Custom Workflow Creation and Execution Tests
    // ========================================================================

    @Test
    fun `create and execute custom workflow`() = runBlocking {
        // Given
        val customWorkflow = Workflow(
            id = "custom_test_workflow",
            appId = "com.android.settings",
            name = "Custom Test Workflow",
            description = "Test custom workflow creation",
            steps = listOf(
                WorkflowStep("launchApp", mapOf("value" to "com.android.settings")),
                WorkflowStep("wait", mapOf("value" to "500")),
                WorkflowStep("home", emptyMap()),
                WorkflowStep("done", mapOf("message" to "Custom workflow completed"))
            ),
            isPrebuilt = false
        )

        // When
        val result = workflowEngine.executeWorkflow(customWorkflow)

        // Then
        assertThat(result.workflowId).isEqualTo("custom_test_workflow")
        assertThat(result.success).isTrue()
    }

    @Test
    fun `create and execute simple wait workflow`() = runBlocking {
        // Given
        val workflow = Workflow(
            id = "simple_wait",
            appId = null,
            name = "Simple Wait",
            steps = listOf(
                WorkflowStep("wait", mapOf("value" to "100")),
                WorkflowStep("done", mapOf("message" to "Wait completed"))
            ),
            isPrebuilt = false
        )

        // When
        val result = workflowEngine.executeWorkflow(workflow)

        // Then
        assertThat(result.workflowId).isEqualTo("simple_wait")
    }

    // ========================================================================
    // Multi-Step Automation Validation Tests
    // ========================================================================

    @Test
    fun `execute multi-step workflow with app launch`() = runBlocking {
        // Given
        val workflow = Workflow(
            id = "multi_step_app",
            appId = "com.google.android.youtube",
            name = "Multi-step App Workflow",
            steps = listOf(
                WorkflowStep("wait", mapOf("value" to "1000")),
                WorkflowStep("home", emptyMap()),
                WorkflowStep("wait", mapOf("value" to "500")),
                WorkflowStep("done", mapOf("message" to "Multi-step completed"))
            ),
            isPrebuilt = false
        )

        // When
        val result = workflowEngine.executeWorkflow(workflow)

        // Then
        assertThat(result.workflowId).isEqualTo("multi_step_app")
        assertThat(result.stepResults).hasSize(4)
    }

    @Test
    fun `execute workflow with navigation actions`() = runBlocking {
        // Given
        val workflow = Workflow(
            id = "navigation_workflow",
            appId = null,
            name = "Navigation Workflow",
            steps = listOf(
                WorkflowStep("home", emptyMap()),
                WorkflowStep("wait", mapOf("value" to "200")),
                WorkflowStep("back", emptyMap()),
                WorkflowStep("wait", mapOf("value" to "200")),
                WorkflowStep("done", mapOf("message" to "Navigation done"))
            ),
            isPrebuilt = false
        )

        // When
        val result = workflowEngine.executeWorkflow(workflow)

        // Then
        assertThat(result.workflowId).isEqualTo("navigation_workflow")
    }

    // ========================================================================
    // Workflow with Variables Tests
    // ========================================================================

    @Test
    fun `execute workflow with variable substitution`() = runBlocking {
        // Given
        val workflow = Workflow(
            id = "variable_workflow",
            appId = null,
            name = "Variable Workflow",
            steps = listOf(
                WorkflowStep("wait", mapOf("value" to "100")),
                WorkflowStep("done", mapOf("message" to "Completed with \${user}"))
            ),
            isPrebuilt = false
        )

        val variables = mapOf("user" to "TestUser")

        // When
        val result = workflowEngine.executeWorkflow(workflow, variables)

        // Then
        assertThat(result.workflowId).isEqualTo("variable_workflow")
        assertThat(result.variables).isEqualTo(variables)
    }

    // ========================================================================
    // Workflow Error Handling Tests
    // ========================================================================

    @Test
    fun `workflow handles step failure gracefully`() = runBlocking {
        // Given
        val workflow = Workflow(
            id = "error_handling_workflow",
            appId = "com.nonexistent.app",
            name = "Error Handling Workflow",
            steps = listOf(
                WorkflowStep("launchApp", mapOf("value" to "com.nonexistent.app")),
                WorkflowStep("done", mapOf("message" to "Should not reach"))
            ),
            isPrebuilt = false
        )

        // When
        val result = workflowEngine.executeWorkflow(workflow)

        // Then - workflow should handle failure
        assertThat(result).isNotNull()
    }

    @Test
    fun `workflow with optional step continues on failure`() = runBlocking {
        // Given
        val workflow = Workflow(
            id = "optional_step_workflow",
            appId = null,
            name = "Optional Step Workflow",
            steps = listOf(
                WorkflowStep("shell", mapOf("value" to "invalid_command"), isOptional = true),
                WorkflowStep("wait", mapOf("value" to "100")),
                WorkflowStep("done", mapOf("message" to "Completed"))
            ),
            isPrebuilt = false
        )

        // When
        val result = workflowEngine.executeWorkflow(workflow)

        // Then - should continue despite optional step failure
        assertThat(result.workflowId).isEqualTo("optional_step_workflow")
    }

    // ========================================================================
    // Workflow Timing Tests
    // ========================================================================

    @Test
    fun `measure workflow execution time`() = runBlocking {
        // Given
        val workflow = Workflow(
            id = "timing_workflow",
            appId = null,
            name = "Timing Workflow",
            steps = listOf(
                WorkflowStep("wait", mapOf("value" to "100")),
                WorkflowStep("wait", mapOf("value" to "100")),
                WorkflowStep("done", mapOf("message" to "Done"))
            ),
            isPrebuilt = false
        )

        // When
        val startTime = System.currentTimeMillis()
        val result = workflowEngine.executeWorkflow(workflow)
        val elapsedMs = System.currentTimeMillis() - startTime

        // Then
        assertThat(result.workflowId).isEqualTo("timing_workflow")
        assertThat(elapsedMs).isAtLeast(200L)
    }

    // ========================================================================
    // Workflow Save and Load Tests
    // ========================================================================

    @Test
    fun `save and load workflow from file`() {
        // Given
        val workflow = Workflow(
            id = "save_load_test",
            appId = null,
            name = "Save Load Test",
            steps = listOf(
                WorkflowStep("wait", mapOf("value" to "100"))
            ),
            isPrebuilt = false
        )

        // Save workflow
        val file = workflowEngine.saveWorkflow(workflow)

        // When - Load workflow
        val loadedWorkflow = workflowEngine.loadWorkflowFromFile(file.name)

        // Then
        assertThat(loadedWorkflow.id).isEqualTo("save_load_test")
        assertThat(loadedWorkflow.name).isEqualTo("Save Load Test")
    }

    @Test
    fun `list saved workflows`() {
        // Given
        val workflow1 = Workflow(
            id = "list_test_1",
            appId = null,
            name = "List Test 1",
            steps = listOf(WorkflowStep("wait", mapOf("value" to "100"))),
            isPrebuilt = false
        )
        val workflow2 = Workflow(
            id = "list_test_2",
            appId = null,
            name = "List Test 2",
            steps = listOf(WorkflowStep("wait", mapOf("value" to "100"))),
            isPrebuilt = false
        )

        workflowEngine.saveWorkflow(workflow1)
        workflowEngine.saveWorkflow(workflow2)

        // When
        val workflows = workflowEngine.listWorkflows()

        // Then
        assertThat(workflows).isNotEmpty()
    }

    @Test
    fun `delete workflow from file system`() {
        // Given
        val workflow = Workflow(
            id = "delete_test",
            appId = null,
            name = "Delete Test",
            steps = listOf(WorkflowStep("wait", mapOf("value" to "100"))),
            isPrebuilt = false
        )

        workflowEngine.saveWorkflow(workflow)

        // When
        val deleted = workflowEngine.deleteWorkflow("delete_test")

        // Then
        assertThat(deleted).isTrue()
    }

    // ========================================================================
    // Workflow YAML Parsing Tests
    // ========================================================================

    @Test
    fun `load workflow from YAML string`() {
        // Given
        val yamlContent = """
            appId: com.android.settings
            name: YAML Test Workflow
            description: Test YAML parsing
            ---
            - launchApp: com.android.settings
            - wait: 500
            - done: YAML workflow completed
        """.trimIndent()

        // When
        val workflow = workflowEngine.loadWorkflow(yamlContent)

        // Then
        assertThat(workflow.id).isEqualTo("yaml_test_workflow")
        assertThat(workflow.name).isEqualTo("YAML Test Workflow")
        assertThat(workflow.steps).hasSize(3)
    }

    // ========================================================================
    // Workflow Step Result Tests
    // ========================================================================

    @Test
    fun `workflow returns step results`() = runBlocking {
        // Given
        val workflow = Workflow(
            id = "step_results_test",
            appId = null,
            name = "Step Results Test",
            steps = listOf(
                WorkflowStep("wait", mapOf("value" to "50")),
                WorkflowStep("wait", mapOf("value" to "50")),
                WorkflowStep("done", mapOf("message" to "Done"))
            ),
            isPrebuilt = false
        )

        // When
        val result = workflowEngine.executeWorkflow(workflow)

        // Then
        assertThat(result.stepResults).hasSize(3)
        assertThat(result.stepResults[0].step.action).isEqualTo("wait")
    }
}
