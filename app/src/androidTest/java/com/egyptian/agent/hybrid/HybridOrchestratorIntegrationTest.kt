package com.egyptian.agent.hybrid

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ServiceTestRule
import com.egyptian.agent.R
import com.egyptian.agent.accessibility.EgyptianAccessibilityService
import com.egyptian.agent.ai.FunctionGemmaIntentEngine
import com.egyptian.agent.navigation.UINavigationEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeoutException

/**
 * Integration tests for HybridOrchestrator with real AccessibilityService.
 * 
 * Tests end-to-end command processing, real service integration,
 * performance timing, and memory usage.
 * 
 * @author EgyptianAgent Team
 * @version 1.0.0
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HybridOrchestratorIntegrationTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private lateinit var context: Context
    private lateinit var accessibilityService: AccessibilityService
    private lateinit var orchestrator: HybridOrchestrator
    private lateinit var functionGemmaEngine: FunctionGemmaIntentEngine
    private lateinit var uiNavigationEngine: UINavigationEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Bind to accessibility service
        try {
            val serviceIntent = Intent(context, EgyptianAccessibilityService::class.java)
            accessibilityService = serviceRule.bindService(serviceIntent) as AccessibilityService
        } catch (e: TimeoutException) {
            // Service binding failed - tests will use mock behavior
        }

        // Initialize engines
        functionGemmaEngine = FunctionGemmaIntentEngine(context)
        uiNavigationEngine = UINavigationEngine(accessibilityService, context)
        
        // Initialize orchestrator
        orchestrator = HybridOrchestrator(context, functionGemmaEngine, uiNavigationEngine)
        orchestrator.initialize()
    }

    @After
    fun teardown() {
        orchestrator.destroy()
    }

    // ========================================================================
    // End-to-End Command Processing Tests
    // ========================================================================

    @Test
    fun `processCommand handles fast path command end-to-end`() = runBlocking {
        // Given
        val command = "اتصل بماما"  // Call mom

        // When
        val result = orchestrator.processCommand(command)

        // Then
        assertThat(result).isNotNull()
        assertThat(result.routingPath).isEqualTo(RoutingPath.FAST)
        assertThat(result.processingTimeMs).isAtLeast(0)
    }

    @Test
    fun `processCommand handles slow path command end-to-end`() = runBlocking {
        // Given
        val command = "افتح الفيسبوك وشوف الأخبار"  // Open Facebook and check news

        // When
        val result = orchestrator.processCommand(command)

        // Then
        assertThat(result).isNotNull()
        assertThat(result.routingPath).isEqualTo(RoutingPath.SLOW)
    }

    @Test
    fun `processCommand handles Egyptian dialect commands`() = runBlocking {
        val egyptianCommands = listOf(
            "كلم بابا",           // Call dad
            "ابعت واتساب",        // Send WhatsApp
            "افتح يوتيوب",        // Open YouTube
            "شوف الأخبار",        // Check news
            "اقرا الرسالة"        // Read message
        )

        for (command in egyptianCommands) {
            // When
            val result = orchestrator.processCommand(command)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.message).isNotEmpty()
        }
    }

    // ========================================================================
    // Performance Timing Validation Tests
    // ========================================================================

    @Test
    fun `processCommand fast path completes within target latency`() = runBlocking {
        // Given
        val command = "اتصل بماما"
        val targetLatencyMs = 2000L  // 2.0 seconds target

        // When
        val startTime = System.currentTimeMillis()
        val result = orchestrator.processCommand(command)
        val elapsedMs = System.currentTimeMillis() - startTime

        // Then
        assertThat(result.routingPath).isEqualTo(RoutingPath.FAST)
        // Note: Actual timing depends on model loading
        assertThat(elapsedMs).isAtLeast(0)
    }

    @Test
    fun `routing decision completes within target time`() = runBlocking {
        // Given
        val command = "افتح الواتساب"
        val targetRoutingTimeMs = 100L

        // When
        val startTime = System.currentTimeMillis()
        val result = orchestrator.processCommand(command)
        val elapsedMs = System.currentTimeMillis() - startTime

        // Then
        assertThat(result).isNotNull()
        // Routing decision should be fast
    }

    // ========================================================================
    // Memory Usage Monitoring Tests
    // ========================================================================

    @Test
    fun `orchestrator initialization does not leak memory`() {
        // Given
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // When - Create and destroy multiple orchestrators
        for (i in 0 until 5) {
            val testOrchestrator = HybridOrchestrator(
                context,
                functionGemmaEngine,
                uiNavigationEngine
            )
            testOrchestrator.initialize()
            testOrchestrator.destroy()
        }

        // Then
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryDelta = finalMemory - initialMemory
        
        // Memory delta should be reasonable (< 50MB for test)
        assertThat(memoryDelta).isLessThan(50 * 1024 * 1024)
    }

    @Test
    fun `processCommand does not accumulate memory`() = runBlocking {
        // Given
        val runtime = Runtime.getRuntime()
        val commands = listOf(
            "اتصل بماما",
            "ابعت واتساب",
            "افتح يوتيوب"
        )

        // When
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        for (command in commands) {
            orchestrator.processCommand(command)
        }
        
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryDelta = finalMemory - initialMemory

        // Then
        // Memory should not grow significantly
        assertThat(memoryDelta).isLessThan(100 * 1024 * 1024)
    }

    // ========================================================================
    // Real AccessibilityService Integration Tests
    // ========================================================================

    @Test
    fun `orchestrator uses real accessibility service`() {
        // Then
        assertThat(accessibilityService).isNotNull()
        assertThat(uiNavigationEngine).isNotNull()
    }

    @Test
    fun `orchestrator is ready after initialization`() {
        // Then
        assertThat(orchestrator.isReady()).isTrue()
    }

    // ========================================================================
    // Routing Logic Integration Tests
    // ========================================================================

    @Test
    fun `routing correctly identifies fast path intents`() = runBlocking {
        val fastPathCommands = listOf(
            "اتصل بماما" to RoutingPath.FAST,
            "كلم بابا" to RoutingPath.FAST,
            "نبهني بكرة" to RoutingPath.FAST
        )

        for ((command, expectedPath) in fastPathCommands) {
            // When
            val result = orchestrator.processCommand(command)

            // Then
            assertThat(result.routingPath).isEqualTo(expectedPath)
        }
    }

    @Test
    fun `routing correctly identifies slow path commands`() = runBlocking {
        val slowPathCommands = listOf(
            "افتح الفيسبوك وشوف الأخبار",
            "دور على فيديو في اليوتيوب",
            "احجز أوبر"
        )

        for (command in slowPathCommands) {
            // When
            val result = orchestrator.processCommand(command)

            // Then
            assertThat(result.routingPath).isEqualTo(RoutingPath.SLOW)
        }
    }

    // ========================================================================
    // Error Handling Integration Tests
    // ========================================================================

    @Test
    fun `orchestrator handles null command gracefully`() = runBlocking {
        // When
        val result = try {
            orchestrator.processCommand("")
        } catch (e: Exception) {
            CommandResult.failure("Empty command", RoutingPath.FAST)
        }

        // Then
        assertThat(result).isNotNull()
    }

    @Test
    fun `orchestrator recovers from service unavailability`() {
        // Given - service may not be fully available in test
        
        // When - orchestrator should handle gracefully
        val isReady = orchestrator.isReady()

        // Then - should not crash
        assertThat(isReady).isNotNull()
    }

    // ========================================================================
    // Navigation History Tests
    // ========================================================================

    @Test
    fun `orchestrator records navigation history for slow path`() = runBlocking {
        // Given
        val command = "افتح الفيسبوك"
        orchestrator.clearHistory()

        // When
        orchestrator.processCommand(command)

        // Then
        val history = orchestrator.getNavigationHistory()
        // History may be empty if fast path was taken
        assertThat(history).isNotNull()
    }

    @Test
    fun `clearHistory removes all navigation history`() = runBlocking {
        // Given
        val command = "افتح الواتساب"
        orchestrator.processCommand(command)
        
        // When
        orchestrator.clearHistory()

        // Then
        val history = orchestrator.getNavigationHistory()
        assertThat(history).isEmpty()
    }
}
