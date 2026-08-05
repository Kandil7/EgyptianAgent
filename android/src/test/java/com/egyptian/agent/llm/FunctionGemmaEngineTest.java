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
 * Unit Tests for FunctionGemmaEngine
 * 
 * Test Coverage:
 * - Engine construction (default and custom config)
 * - Fallback inference (model not loaded in unit tests)
 * - Fallback function calling via parseTextFunctionCall
 * - Streaming callback tests
 * - Performance (fallback latency, metrics getters)
 * - Edge cases (empty, null, long, mixed input)
 * - State management (isReady, destroy)
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
    }

    @Test
    public void testConstructor_WithConfig() {
        FunctionGemmaConfig config = new FunctionGemmaConfig.Builder()
            .contextSize(1024)
            .numThreads(2)
            .temperature(0.1f)
            .build();
        
        engine = new FunctionGemmaEngine(mockContext, config);
        assertNotNull("Engine should be created with config", engine);
        assertFalse("Engine should not be ready with config", engine.isReady());
    }

    // ========================================================================
    // Inference Tests (fallback mode - model never loads in unit tests)
    // ========================================================================

    @Test
    public void testGenerateResponse_SimpleCommand() {
        engine = new FunctionGemmaEngine(mockContext);
        
        // Note: In unit tests, this will use fallback since model won't be loaded
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("اتصل بماما", 64);
        
        assertNotNull("Result should not be null", result);
        assertNotNull("Function name should not be null", result.getFunctionName());
        // Fallback should classify call commands
        assertEquals("call_contact", result.getFunctionName());
    }

    @Test
    public void testGenerateResponse_ComplexCommand() {
        engine = new FunctionGemmaEngine(mockContext);
        
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand(
            "ابعت واتساب لأحمد وقوله إنى هتأخر عن الاجتماع", 
            128
        );
        
        assertNotNull("Result should not be null", result);
        assertEquals("send_whatsapp", result.getFunctionName());
    }

    @Test
    public void testGenerateResponse_EmptyInput() {
        engine = new FunctionGemmaEngine(mockContext);
        
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("", 64);
        
        assertNotNull("Should handle empty input", result);
    }

    @Test
    public void testGenerateResponse_NullInput() {
        engine = new FunctionGemmaEngine(mockContext);
        
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand(null, 64);
        
        assertNotNull("Should handle null input", result);
        assertEquals("unknown", result.getFunctionName());
    }

    // ========================================================================
    // Function Calling Tests (fallback mode)
    // ========================================================================

    @Test
    public void testCallFunction_CallContact() {
        engine = new FunctionGemmaEngine(mockContext);
        
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("اتصل بماما", 64);
        
        assertNotNull("Function call result should not be null", result);
        assertEquals("call_contact", result.getFunctionName());
        assertEquals("ماما", result.getArgument("contact_name"));
    }

    @Test
    public void testCallFunction_SendWhatsapp() {
        engine = new FunctionGemmaEngine(mockContext);
        
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("ابعت واتساب لأحمد", 64);
        
        assertNotNull("Function call result should not be null", result);
        assertEquals("send_whatsapp", result.getFunctionName());
        assertEquals("أحمد", result.getArgument("contact_name"));
    }

    @Test
    public void testCallFunction_SetAlarm() {
        engine = new FunctionGemmaEngine(mockContext);
        
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("نبهني بكرة الصبح", 64);
        
        assertNotNull("Function call result should not be null", result);
        assertEquals("set_alarm", result.getFunctionName());
        assertEquals("بكرة", result.getArgument("time"));
    }

    @Test
    public void testCallFunction_InvalidFunction() {
        engine = new FunctionGemmaEngine(mockContext);
        
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("invalid_function_name", 64);
        
        assertNotNull("Should handle invalid function", result);
        assertFalse("Invalid function should not be valid", result.isValid());
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
        
        engine.processCommandAsync("اتصل بماما", 64, new FunctionGemmaEngine.TokenCallback() {
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
    public void testStreaming_CompletesInFallbackMode() throws Exception {
        engine = new FunctionGemmaEngine(mockContext);
        
        CountDownLatch latch = new CountDownLatch(1);
        
        engine.processCommandAsync("الساعة كام؟", 32, new FunctionGemmaEngine.TokenCallback() {
            @Override
            public void onToken(String token) {
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
        
        // Fallback mode delivers the full response without streaming tokens
        assertTrue("Should complete within timeout", latch.await(5, TimeUnit.SECONDS));
    }

    // ========================================================================
    // Performance Tests
    // ========================================================================

    @Test
    public void testPerformance_InferenceLatency() {
        engine = new FunctionGemmaEngine(mockContext);
        
        long startTime = System.currentTimeMillis();
        engine.processCommand("اتصل بماما", 64);
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
            engine.processCommandAsync("اتصل بماما", 64, new FunctionGemmaEngine.TokenCallback() {
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
        
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand(longInput.toString(), 256);
        
        assertNotNull("Should handle very long input", result);
    }

    @Test
    public void testEdgeCase_SpecialCharacters() {
        engine = new FunctionGemmaEngine(mockContext);
        
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("!@#$%^&*()", 64);
        
        assertNotNull("Should handle special characters", result);
    }

    @Test
    public void testEdgeCase_MixedLanguages() {
        engine = new FunctionGemmaEngine(mockContext);
        
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("Call mom اتصل بماما", 64);
        
        assertNotNull("Should handle mixed languages", result);
    }

    @Test
    public void testEdgeCase_Emoji() {
        engine = new FunctionGemmaEngine(mockContext);
        
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("اتصل بماما 📞❤️", 64);
        
        assertNotNull("Should handle emoji", result);
    }

    // ========================================================================
    // State Management Tests
    // ========================================================================

    @Test
    public void testIsReady_InitialState() {
        engine = new FunctionGemmaEngine(mockContext);
        // Initially not ready (model not loaded in unit tests)
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
        
        assertEquals("Initial load time should be 0", 0, engine.getModelLoadTimeMs());
        assertEquals("Initial inference count should be 0", 0, engine.getTotalInferences());
        assertEquals("Initial success rate should be 0", 0.0f, engine.getSuccessRate(), 0.001f);
    }

    @Test
    public void testFallback_ServesInferenceWithoutModel() {
        engine = new FunctionGemmaEngine(mockContext);
        
        // Perform inference via fallback
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("اتصل بماما", 64);
        
        assertNotNull("Result should not be null", result);
        assertEquals("call_contact", result.getFunctionName());
        // Model still not loaded - fallback path was used
        assertFalse("Engine should not be ready (fallback used)", engine.isReady());
    }

    // ========================================================================
    // JSON Output Validation Tests
    // ========================================================================

    @Test
    public void testJsonOutput_ValidStructure() {
        engine = new FunctionGemmaEngine(mockContext);
        
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("اتصل بماما", 64);
        
        assertNotNull("Result should not be null", result);
        // Validate structure
        assertNotNull("Function name should exist", result.getFunctionName());
        assertNotNull("Arguments should exist", result.getArguments());
    }

    @Test
    public void testJsonOutput_ArgumentTypes() {
        engine = new FunctionGemmaEngine(mockContext);
        
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("ابعت واتساب لأحمد", 64);
        
        assertNotNull("Result should not be null", result);
        
        // Validate argument types
        assertTrue("Arguments should be a map", result.getArguments() instanceof java.util.Map);
        assertNotNull("contact_name argument should exist", result.getArgument("contact_name"));
    }

    // ========================================================================
    // Cleanup
    // ========================================================================

    @Test
    public void testCleanup_Resources() {
        engine = new FunctionGemmaEngine(mockContext);
        
        // Perform some operations
        engine.processCommand("اتصل بماما", 64);
        engine.processCommand("نبهني بكرة", 64);
        
        // Clean up
        engine.destroy();
        
        // Verify cleanup (should not throw)
        assertFalse("Engine should not be ready after destroy", engine.isReady());
    }
}
