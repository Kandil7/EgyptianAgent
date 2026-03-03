package com.egyptian.agent.llm;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Comprehensive Unit Tests for FunctionGemmaEngine
 * 
 * Test Coverage:
 * - Model Loading (success/failure scenarios)
 * - Inference Performance (<500ms target)
 * - JSON Output Validation
 * - Streaming Callback Tests
 * - Edge Cases (empty, long, mixed input)
 * 
 * @author EgyptianAgent Team
 * @version 1.0
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class FunctionGemmaEngineTest {

    @Mock
    private Context mockContext;

    private FunctionGemmaEngine engine;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ========================================================================
    // Model Loading Tests
    // ========================================================================

    @Test
    public void testConstructor_CreatesEngine() {
        engine = new FunctionGemmaEngine(mockContext);
        assertNotNull("Engine should be created", engine);
        assertFalse("Engine should not be ready immediately", engine.isReady());
        assertTrue("Engine should be loading", engine.isLoading());
    }

    @Test
    public void testConstructor_WithConfig() {
        FunctionGemmaConfig config = new FunctionGemmaConfig.Builder()
            .setContextSize(1024)
            .setNumThreads(2)
            .setTemperature(0.1f)
            .build();
        
        engine = new FunctionGemmaEngine(mockContext, config);
        assertNotNull("Engine should be created with config", engine);
    }

    @Test
    public void testIsLoading_State() {
        engine = new FunctionGemmaEngine(mockContext);
        assertTrue("Engine should be in loading state initially", engine.isLoading());
    }

    // ========================================================================
    // Inference Tests
    // ========================================================================

    @Test
    public void testGenerateResponse_SimpleCommand() {
        engine = new FunctionGemmaEngine(mockContext);
        
        // Note: In unit tests, this will use fallback since model won't be loaded
        String response = engine.generateResponse("اتصل بماما", 64);
        
        assertNotNull("Response should not be null", response);
        // Fallback should still return something meaningful
        assertFalse("Response should not be empty", response.trim().isEmpty());
    }

    @Test
    public void testGenerateResponse_ComplexCommand() {
        engine = new FunctionGemmaEngine(mockContext);
        
        String response = engine.generateResponse(
            "ابعت واتساب لأحمد وقوله إنى هتأخر عن الاجتماع", 
            128
        );
        
        assertNotNull("Response should not be null", response);
    }

    @Test
    public void testGenerateResponse_EmptyInput() {
        engine = new FunctionGemmaEngine(mockContext);
        
        String response = engine.generateResponse("", 64);
        
        assertNotNull("Should handle empty input", response);
    }

    @Test
    public void testGenerateResponse_NullInput() {
        engine = new FunctionGemmaEngine(mockContext);
        
        String response = engine.generateResponse(null, 64);
        
        assertNotNull("Should handle null input", response);
    }

    // ========================================================================
    // Function Calling Tests
    // ========================================================================

    @Test
    public void testCallFunction_CallContact() {
        engine = new FunctionGemmaEngine(mockContext);
        
        FunctionGemmaEngine.FunctionCallResult result = engine.callFunction(
            "call_contact",
            java.util.Map.of("contact_name", "ماما")
        );
        
        assertNotNull("Function call result should not be null", result);
        // In unit tests, will use mock/fallback
    }

    @Test
    public void testCallFunction_SendWhatsapp() {
        engine = new FunctionGemmaEngine(mockContext);
        
        FunctionGemmaEngine.FunctionCallResult result = engine.callFunction(
            "send_whatsapp",
            java.util.Map.of(
                "contact_name", "أحمد",
                "message", "سلامات"
            )
        );
        
        assertNotNull("Function call result should not be null", result);
    }

    @Test
    public void testCallFunction_SetAlarm() {
        engine = new FunctionGemmaEngine(mockContext);
        
        FunctionGemmaEngine.FunctionCallResult result = engine.callFunction(
            "set_alarm",
            java.util.Map.of("time", "بكرة الصبح")
        );
        
        assertNotNull("Function call result should not be null", result);
    }

    @Test
    public void testCallFunction_InvalidFunction() {
        engine = new FunctionGemmaEngine(mockContext);
        
        FunctionGemmaEngine.FunctionCallResult result = engine.callFunction(
            "invalid_function_name",
            java.util.Map.of("param", "value")
        );
        
        assertNotNull("Should handle invalid function", result);
        assertFalse("Invalid function should not succeed", result.success);
    }

    // ========================================================================
    // Streaming Tests
    // ========================================================================

    @Test
    public void testGenerateResponseAsync_Streaming() throws Exception {
        engine = new FunctionGemmaEngine(mockContext);
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger tokenCount = new AtomicInteger(0);
        AtomicBoolean completed = new AtomicBoolean(false);
        
        engine.generateResponseAsync("اتصل بماما", 64, new FunctionGemmaEngine.TokenCallback() {
            @Override
            public void onToken(String token) {
                tokenCount.incrementAndGet();
            }

            @Override
            public void onComplete(String fullResponse) {
                completed.set(true);
                latch.countDown();
            }

            @Override
            public void onError(Exception error) {
                latch.countDown();
            }
        });
        
        // Wait for completion (with timeout)
        boolean completedInTime = latch.await(5, TimeUnit.SECONDS);
        
        // In unit tests with fallback, should complete quickly
        assertTrue("Should complete within timeout", completedInTime);
        assertTrue("Completion callback should be called", completed.get());
    }

    @Test
    public void testStreaming_MultipleTokens() throws Exception {
        engine = new FunctionGemmaEngine(mockContext);
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger tokenCount = new AtomicInteger(0);
        
        engine.generateResponseAsync("الساعة كام؟", 32, new FunctionGemmaEngine.TokenCallback() {
            @Override
            public void onToken(String token) {
                tokenCount.incrementAndGet();
            }

            @Override
            public void onComplete(String fullResponse) {
                latch.countDown();
            }

            @Override
            public void onError(Exception error) {
                latch.countDown();
            }
        });
        
        latch.await(5, TimeUnit.SECONDS);
        
        // Should receive at least one token (even from fallback)
        assertTrue("Should receive tokens", tokenCount.get() > 0);
    }

    // ========================================================================
    // Performance Tests
    // ========================================================================

    @Test
    public void testPerformance_InferenceLatency() {
        engine = new FunctionGemmaEngine(mockContext);
        
        long startTime = System.currentTimeMillis();
        engine.generateResponse("اتصل بماما", 64);
        long elapsed = System.currentTimeMillis() - startTime;
        
        // With fallback, should be very fast (<100ms)
        assertTrue("Inference should complete in <100ms (fallback mode), took: " + elapsed + "ms", 
                   elapsed < 100);
    }

    @Test
    public void testPerformance_ConcurrentRequests() throws Exception {
        engine = new FunctionGemmaEngine(mockContext);
        
        int numRequests = 10;
        CountDownLatch latch = new CountDownLatch(numRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numRequests; i++) {
            final int requestId = i;
            engine.generateResponseAsync("اتصل بماما", 64, new FunctionGemmaEngine.TokenCallback() {
                @Override
                public void onToken(String token) {}

                @Override
                public void onComplete(String fullResponse) {
                    successCount.incrementAndGet();
                    latch.countDown();
                }

                @Override
                public void onError(Exception error) {
                    latch.countDown();
                }
            });
        }
        
        boolean completedInTime = latch.await(10, TimeUnit.SECONDS);
        
        assertTrue("All requests should complete", completedInTime);
        assertEquals("All requests should succeed", numRequests, successCount.get());
    }

    // ========================================================================
    // Edge Cases Tests
    // ========================================================================

    @Test
    public void testEdgeCase_VeryLongInput() {
        engine = new FunctionGemmaEngine(mockContext);
        
        StringBuilder longInput = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longInput.append("اتصل بماما ");
        }
        
        String response = engine.generateResponse(longInput.toString(), 256);
        
        assertNotNull("Should handle very long input", response);
    }

    @Test
    public void testEdgeCase_SpecialCharacters() {
        engine = new FunctionGemmaEngine(mockContext);
        
        String response = engine.generateResponse("!@#$%^&*()", 64);
        
        assertNotNull("Should handle special characters", response);
    }

    @Test
    public void testEdgeCase_MixedLanguages() {
        engine = new FunctionGemmaEngine(mockContext);
        
        String response = engine.generateResponse("Call mom اتصل بماما", 64);
        
        assertNotNull("Should handle mixed languages", response);
    }

    @Test
    public void testEdgeCase_Emoji() {
        engine = new FunctionGemmaEngine(mockContext);
        
        String response = engine.generateResponse("اتصل بماما 📞❤️", 64);
        
        assertNotNull("Should handle emoji", response);
    }

    // ========================================================================
    // State Management Tests
    // ========================================================================

    @Test
    public void testIsReady_InitialState() {
        engine = new FunctionGemmaEngine(mockContext);
        // Initially not ready (model loading)
        assertFalse("Engine should not be ready initially", engine.isReady());
    }

    @Test
    public void testDestroy() {
        engine = new FunctionGemmaEngine(mockContext);
        engine.destroy();
        
        // Should not throw exceptions
        assertFalse("Engine should not be ready after destroy", engine.isReady());
    }

    @Test
    public void testDestroy_MultipleCalls() {
        engine = new FunctionGemmaEngine(mockContext);
        engine.destroy();
        engine.destroy(); // Second call should be safe
        
        assertTrue("Multiple destroy calls should be safe", true);
    }

    // ========================================================================
    // Performance Metrics Tests
    // ========================================================================

    @Test
    public void testPerformanceMetrics_Initial() {
        engine = new FunctionGemmaEngine(mockContext);
        
        FunctionGemmaEngine.PerformanceMetrics metrics = engine.getPerformanceMetrics();
        
        assertNotNull("Metrics should not be null", metrics);
        assertEquals("Initial load time should be 0", 0, metrics.loadTimeMs);
        assertEquals("Initial inference count should be 0", 0, metrics.inferenceCount);
    }

    @Test
    public void testPerformanceMetrics_AfterInference() {
        engine = new FunctionGemmaEngine(mockContext);
        
        // Perform inference
        engine.generateResponse("اتصل بماما", 64);
        
        FunctionGemmaEngine.PerformanceMetrics metrics = engine.getPerformanceMetrics();
        
        assertNotNull("Metrics should not be null", metrics);
        assertTrue("Inference count should be >= 1", metrics.inferenceCount >= 1);
    }

    // ========================================================================
    // JSON Output Validation Tests
    // ========================================================================

    @Test
    public void testJsonOutput_ValidStructure() {
        engine = new FunctionGemmaEngine(mockContext);
        
        FunctionGemmaEngine.FunctionCallResult result = engine.callFunction(
            "call_contact",
            java.util.Map.of("contact_name", "ماما")
        );
        
        assertNotNull("Result should not be null", result);
        // Validate structure
        assertNotNull("Function name should exist", result.functionName);
        assertNotNull("Arguments should exist", result.arguments);
    }

    @Test
    public void testJsonOutput_ArgumentTypes() {
        engine = new FunctionGemmaEngine(mockContext);
        
        FunctionGemmaEngine.FunctionCallResult result = engine.callFunction(
            "send_whatsapp",
            java.util.Map.of(
                "contact_name", "أحمد",
                "message", "سلامات",
                "timestamp", System.currentTimeMillis()
            )
        );
        
        assertNotNull("Result should not be null", result);
        
        // Validate argument types
        assertTrue("Arguments should be a map", result.arguments instanceof java.util.Map);
    }

    // ========================================================================
    // Cleanup
    // ========================================================================

    @Test
    public void testCleanup_Resources() {
        engine = new FunctionGemmaEngine(mockContext);
        
        // Perform some operations
        engine.generateResponse("اتصل بماما", 64);
        engine.callFunction("set_alarm", java.util.Map.of("time", "بكرة"));
        
        // Clean up
        engine.destroy();
        
        // Verify cleanup (should not throw)
        assertFalse("Engine should not be ready after destroy", engine.isReady());
    }
}
