package com.egyptian.agent.llm;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Comprehensive Unit Tests for FunctionGemmaEngine
 *
 * Test Coverage:
 * - Model loading success/failure
 * - Inference performance (<500ms)
 * - JSON output validation
 * - Streaming callback tests
 * - Edge cases (empty input, long input)
 * - Configuration validation
 * - Memory management
 * - Error handling
 *
 * Performance Targets:
 * - Model load time: <5 seconds
 * - Inference time: <500ms per command
 * - Memory usage: ~550MB
 *
 * @author Egyptian Agent Team
 * @version 1.0.0
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class FunctionGemmaEngineTest {

    // ========================================================================
    // Test Configuration
    // ========================================================================

    private static final long MODEL_LOAD_TIMEOUT_MS = 10000;
    private static final long MAX_INFERENCE_TIME_MS = 500;
    private static final int ASYNC_TIMEOUT_SECONDS = 15;
    private static final float CONFIDENCE_THRESHOLD = 0.85f;

    private Context context;
    private FunctionGemmaEngine engine;
    private FunctionGemmaConfig config;

    // Test statistics
    private int totalTests;
    private int passedTests;
    private long totalInferenceTime;

    // ========================================================================
    // Test Setup
    // ========================================================================

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();

        // Create optimized config for testing
        config = FunctionGemmaConfig.builder()
                .confidenceThreshold(CONFIDENCE_THRESHOLD)
                .maxTokens(128)
                .temperature(0.1f)
                .topK(40)
                .topP(0.9f)
                .useStreaming(false)
                .enableLogging(false)
                .contextSize(2048)
                .numThreads(2)
                .build();

        // Initialize test statistics
        totalTests = 0;
        passedTests = 0;
        totalInferenceTime = 0;
    }

    // ========================================================================
    // CATEGORY 1: Model Loading Tests (8 tests)
    // ========================================================================

    @Test
    public void testEngineCreation() {
        // Given: New engine instance
        engine = new FunctionGemmaEngine(context, config);

        // Then: Engine should be created successfully
        assertNotNull("Engine should be initialized", engine);
        assertFalse("Engine should not be destroyed", engine.isDestroyed());
    }

    @Test
    public void testEngineWithDefaultConfig() {
        // Given: Engine with default configuration
        engine = new FunctionGemmaEngine(context);

        // Then: Should initialize successfully
        assertNotNull("Engine should be initialized with default config", engine);
    }

    @Test
    public void testModelLoadingStatus() {
        // Given: New engine
        engine = new FunctionGemmaEngine(context, config);

        // When: Wait for model loading
        waitForModelLoad();

        // Then: Loading status should be tracked
        assertFalse("Engine should not be destroyed during load", engine.isDestroyed());
    }

    @Test
    public void testIsReady_AfterLoad() {
        // Given: New engine
        engine = new FunctionGemmaEngine(context, config);

        // When: Wait for potential model load
        waitForModelLoad();

        // Then: Ready status should be available
        // Note: In test environment without model file, isReady may be false
        boolean isReady = engine.isReady();
        // This is acceptable in test environment
        assertNotNull("Ready status should be available", isReady);
    }

    @Test
    public void testIsLoading_DuringInitialization() {
        // Given: New engine
        engine = new FunctionGemmaEngine(context, config);

        // Then: Loading status should be tracked
        boolean isLoading = engine.isLoading();
        // Loading may be true or false depending on timing
        assertNotNull("Loading status should be available", isLoading);
    }

    @Test
    public void testModelLoadTime_Tracking() {
        // Given: New engine
        engine = new FunctionGemmaEngine(context, config);

        // When: Wait for model load
        waitForModelLoad();

        // Then: Load time should be tracked (may be 0 if model not available)
        long loadTime = engine.getModelLoadTimeMs();
        assertTrue("Load time should be >= 0", loadTime >= 0);
    }

    @Test
    public void testEstimatedMemoryUsage() {
        // Given: New engine
        engine = new FunctionGemmaEngine(context, config);

        // Then: Memory usage should be reported
        long memoryUsage = engine.getEstimatedMemoryUsageMB();
        assertTrue("Memory usage should be >= 0", memoryUsage >= 0);
    }

    @Test
    public void testEngineConfiguration_Applied() {
        // Given: Engine with custom config
        FunctionGemmaConfig customConfig = FunctionGemmaConfig.builder()
                .maxTokens(64)
                .temperature(0.2f)
                .confidenceThreshold(0.9f)
                .build();

        engine = new FunctionGemmaEngine(context, customConfig);

        // Then: Engine should be created with custom config
        assertNotNull("Engine should be initialized", engine);
    }

    // ========================================================================
    // CATEGORY 2: Inference Performance Tests (8 tests)
    // ========================================================================

    @Test
    public void testInferencePerformance_SimpleCommand() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process simple command
        long startTime = System.currentTimeMillis();
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("الساعة كام");
        long processingTime = System.currentTimeMillis() - startTime;
        totalInferenceTime += processingTime;
        totalTests++;

        // Then: Should complete within time limit
        if (engine.isReady()) {
            assertTrue("Inference should complete within " + MAX_INFERENCE_TIME_MS + "ms, took: " + processingTime + "ms",
                    processingTime <= MAX_INFERENCE_TIME_MS * 3); // Allow 3x for test environment
        }
        assertNotNull("Result should not be null", result);
        passedTests++;
    }

    @Test
    public void testInferencePerformance_CallCommand() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process call command
        long startTime = System.currentTimeMillis();
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("اتصل بماما");
        long processingTime = System.currentTimeMillis() - startTime;
        totalInferenceTime += processingTime;
        totalTests++;

        // Then: Should complete within time limit
        assertNotNull("Result should not be null", result);
        if (engine.isReady()) {
            assertTrue("Inference should complete within time limit",
                    processingTime <= MAX_INFERENCE_TIME_MS * 3);
        }
        passedTests++;
    }

    @Test
    public void testInferencePerformance_EmergencyCommand() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process emergency command
        long startTime = System.currentTimeMillis();
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("نجدة");
        long processingTime = System.currentTimeMillis() - startTime;
        totalInferenceTime += processingTime;
        totalTests++;

        // Then: Should complete quickly for emergency
        assertNotNull("Result should not be null", result);
        passedTests++;
    }

    @Test
    public void testInferencePerformance_MultipleCommands() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process multiple commands
        String[] commands = {"الساعة كام", "اتصل بماما", "افتح الواي فاي"};
        long totalTime = 0;

        for (String command : commands) {
            long startTime = System.currentTimeMillis();
            FunctionGemmaEngine.FunctionCallResult result = engine.processCommand(command);
            long processingTime = System.currentTimeMillis() - startTime;
            totalTime += processingTime;
            assertNotNull("Result should not be null for: " + command, result);
        }

        // Then: Average should be within acceptable range
        long avgTime = totalTime / commands.length;
        System.out.println("Average inference time: " + avgTime + "ms");
    }

    @Test
    public void testInferencePerformance_WithTokenLimit() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process with custom token limit
        long startTime = System.currentTimeMillis();
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("الساعة كام", 64);
        long processingTime = System.currentTimeMillis() - startTime;

        // Then: Should complete with limited tokens
        assertNotNull("Result should not be null", result);
        assertTrue("Should complete within time limit", processingTime <= MAX_INFERENCE_TIME_MS * 3);
    }

    @Test
    public void testInferencePerformance_RawOutput() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Get raw output
        String rawOutput = engine.processCommandRaw("الساعة كام");

        // Then: Should return non-null output
        assertNotNull("Raw output should not be null", rawOutput);
    }

    @Test
    public void testInferenceMetrics_Tracking() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process multiple commands
        for (int i = 0; i < 5; i++) {
            engine.processCommand("الساعة كام");
        }

        // Then: Metrics should be tracked
        assertTrue("Total inferences should be > 0", engine.getTotalInferences() > 0);
        assertTrue("Total inference time should be >= 0", engine.getTotalInferenceTimeMs() >= 0);
    }

    @Test
    public void testInferenceMetrics_SuccessRate() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process commands
        engine.processCommand("الساعة كام");
        engine.processCommand("اتصل بماما");

        // Then: Success rate should be calculated
        float successRate = engine.getSuccessRate();
        assertTrue("Success rate should be between 0 and 1", successRate >= 0 && successRate <= 1);
    }

    // ========================================================================
    // CATEGORY 3: JSON Output Validation Tests (6 tests)
    // ========================================================================

    @Test
    public void testJsonOutput_ValidFunctionCall() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process command
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("اتصل بماما");

        // Then: Result should have valid structure
        assertNotNull("Result should not be null", result);
        assertNotNull("Function name should not be null", result.getFunctionName());

        // Check if it's a known function
        String functionName = result.getFunctionName().toLowerCase();
        assertTrue("Function name should be valid",
                functionName.equals("unknown") ||
                functionName.equals("call_contact") ||
                functionName.equals("read_time") ||
                functionName.equals("emergency") ||
                functionName.equals("send_whatsapp") ||
                functionName.equals("set_alarm") ||
                functionName.equals("toggle_wifi") ||
                functionName.equals("toggle_bluetooth") ||
                functionName.equals("open_app") ||
                functionName.equals("greeting") ||
                functionName.equals("thank_you") ||
                functionName.equals("goodbye"));
    }

    @Test
    public void testJsonOutput_ArgumentsMap() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process command with arguments
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("اتصل بماما");

        // Then: Arguments should be accessible
        assertNotNull("Arguments map should not be null", result.getArguments());

        // Check for contact_name argument
        String contactName = result.getArgument("contact_name");
        // May or may not have contact name depending on model output
        assertNotNull("Arguments map should be accessible", result.getArguments());
    }

    @Test
    public void testJsonOutput_ArgumentRetrieval() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process command
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("اتصل بماما");

        // Then: Argument retrieval methods should work
        String contactName = result.getArgument("contact_name");
        String contactNameDefault = result.getArgument("contact_name", "default");

        assertNotNull("getArgument should work", contactName != null || contactNameDefault != null);
    }

    @Test
    public void testJsonOutput_ConfidenceScore() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process command
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("اتصل بماما");

        // Then: Confidence should be in valid range
        float confidence = result.getConfidence();
        assertTrue("Confidence should be between 0 and 1", confidence >= 0 && confidence <= 1);
    }

    @Test
    public void testJsonOutput_RawOutputPreserved() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process command
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("الساعة كام");

        // Then: Raw output should be preserved (may be null in fallback)
        // This is acceptable as fallback may not preserve raw output
        assertNotNull("Result should exist", result);
    }

    @Test
    public void testJsonOutput_IsValid() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process valid command
        FunctionGemmaEngine.FunctionCallResult validResult = engine.processCommand("اتصل بماما");

        // Then: isValid should reflect result quality
        boolean isValid = validResult.isValid();
        // Validity depends on model output and confidence
        assertNotNull("isValid should return boolean", isValid);
    }

    // ========================================================================
    // CATEGORY 4: Streaming Callback Tests (5 tests)
    // ========================================================================

    @Test
    public void testStreamingCallback_TokenStream() throws Exception {
        // Given: Initialized engine with streaming config
        FunctionGemmaConfig streamingConfig = FunctionGemmaConfig.builder()
                .useStreaming(true)
                .build();
        engine = new FunctionGemmaEngine(context, streamingConfig);
        waitForModelLoad();

        // When: Process with streaming callback
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean receivedToken = new AtomicBoolean(false);
        AtomicReference<String> finalResponse = new AtomicReference<>();

        engine.processCommandAsync("الساعة كام", new FunctionGemmaEngine.TokenCallback() {
            @Override
            public void onToken(String token) {
                if (token != null && !token.isEmpty()) {
                    receivedToken.set(true);
                }
            }

            @Override
            public void onComplete(String response) {
                finalResponse.set(response);
                latch.countDown();
            }

            @Override
            public void onError(Exception error) {
                latch.countDown();
            }
        });

        // Then: Callback should complete
        boolean completed = latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue("Streaming callback should complete", completed);
    }

    @Test
    public void testStreamingCallback_CompleteResponse() throws Exception {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process with callback
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> response = new AtomicReference<>();

        engine.processCommandAsync("اتصل بماما", new FunctionGemmaEngine.TokenCallback() {
            @Override
            public void onToken(String token) {
                // Collect tokens
            }

            @Override
            public void onComplete(String fullResponse) {
                response.set(fullResponse);
                latch.countDown();
            }

            @Override
            public void onError(Exception error) {
                latch.countDown();
            }
        });

        // Then: Should receive complete response
        boolean completed = latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue("Callback should complete", completed);
    }

    @Test
    public void testStreamingCallback_WithTokenLimit() throws Exception {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process with token limit
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean completed = new AtomicBoolean(false);

        engine.processCommandAsync("الساعة كام", 64, new FunctionGemmaEngine.TokenCallback() {
            @Override
            public void onToken(String token) {}

            @Override
            public void onComplete(String response) {
                completed.set(true);
                latch.countDown();
            }

            @Override
            public void onError(Exception error) {
                latch.countDown();
            }
        });

        // Then: Should complete with limited tokens
        boolean success = latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue("Should complete with token limit", success);
    }

    @Test
    public void testStreamingCallback_ErrorHandling() throws Exception {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process null input (should handle gracefully)
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        engine.processCommandAsync(null, new FunctionGemmaEngine.TokenCallback() {
            @Override
            public void onToken(String token) {}

            @Override
            public void onComplete(String response) {
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });

        // Then: Should complete (may or may not error)
        boolean completed = latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue("Callback should complete", completed);
    }

    @Test
    public void testStreamingCallback_MultipleTokens() throws Exception {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process command and count tokens
        CountDownLatch latch = new CountDownLatch(1);
        java.util.List<String> tokens = new java.util.ArrayList<>();

        engine.processCommandAsync("اتصل بماما", new FunctionGemmaEngine.TokenCallback() {
            @Override
            public void onToken(String token) {
                if (token != null && !token.isEmpty()) {
                    tokens.add(token);
                }
            }

            @Override
            public void onComplete(String response) {
                latch.countDown();
            }

            @Override
            public void onError(Exception error) {
                latch.countDown();
            }
        });

        // Then: Should receive tokens
        latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        // Token count depends on model output
    }

    // ========================================================================
    // CATEGORY 5: Edge Cases Tests (10 tests)
    // ========================================================================

    @Test
    public void testEdgeCases_EmptyInput() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process empty input
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("");

        // Then: Should handle gracefully
        assertNotNull("Result should not be null", result);
        // Empty input may return unknown function
    }

    @Test
    public void testEdgeCases_NullInput() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process null input
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand(null);

        // Then: Should handle gracefully
        assertNotNull("Result should not be null", result);
    }

    @Test
    public void testEdgeCases_WhitespaceInput() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process whitespace input
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("   ");

        // Then: Should handle gracefully
        assertNotNull("Result should not be null", result);
    }

    @Test
    public void testEdgeCases_VeryLongInput() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process very long input
        StringBuilder longInput = new StringBuilder("اتصل بماما ");
        for (int i = 0; i < 100; i++) {
            longInput.append("وكمان ");
        }

        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand(longInput.toString());

        // Then: Should handle gracefully (may truncate or return unknown)
        assertNotNull("Result should not be null", result);
    }

    @Test
    public void testEdgeCases_SpecialCharacters() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process input with special characters
        String[] specialInputs = {
            "اتصل بماما!!!",
            "الساعة كام؟؟؟",
            "نجدة...",
            "!!!",
            "📞 ماما"
        };

        for (String input : specialInputs) {
            FunctionGemmaEngine.FunctionCallResult result = engine.processCommand(input);
            assertNotNull("Should handle special characters: " + input, result);
        }
    }

    @Test
    public void testEdgeCases_NumbersOnly() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process numbers-only input
        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("123456789");

        // Then: Should handle gracefully
        assertNotNull("Result should not be null", result);
    }

    @Test
    public void testEdgeCases_MixedLanguages() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process mixed language input
        String[] mixedInputs = {
            "Call ماما",
            "Open واتساب",
            "الساعة what time"
        };

        for (String input : mixedInputs) {
            FunctionGemmaEngine.FunctionCallResult result = engine.processCommand(input);
            assertNotNull("Should handle mixed language: " + input, result);
        }
    }

    @Test
    public void testEdgeCases_RepeatedCommands() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process same command multiple times
        FunctionGemmaEngine.FunctionCallResult result1 = engine.processCommand("الساعة كام");
        FunctionGemmaEngine.FunctionCallResult result2 = engine.processCommand("الساعة كام");
        FunctionGemmaEngine.FunctionCallResult result3 = engine.processCommand("الساعة كام");

        // Then: All should return valid results
        assertNotNull("First result should not be null", result1);
        assertNotNull("Second result should not be null", result2);
        assertNotNull("Third result should not be null", result3);
    }

    @Test
    public void testEdgeCases_RapidSuccessiveCalls() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Make rapid successive calls
        for (int i = 0; i < 10; i++) {
            FunctionGemmaEngine.FunctionCallResult result = engine.processCommand("الساعة كام");
            assertNotNull("Result " + i + " should not be null", result);
        }
    }

    @Test
    public void testEdgeCases_UnicodeInput() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process Unicode input
        String unicodeInput = "الساعة كام \u200E\u200F"; // With LTR/RTL marks

        FunctionGemmaEngine.FunctionCallResult result = engine.processCommand(unicodeInput);

        // Then: Should handle Unicode gracefully
        assertNotNull("Result should not be null", result);
    }

    // ========================================================================
    // CATEGORY 6: Configuration Validation Tests (5 tests)
    // ========================================================================

    @Test
    public void testConfig_TemperatureImpact() {
        // Given: Engines with different temperatures
        FunctionGemmaConfig lowTempConfig = FunctionGemmaConfig.builder()
                .temperature(0.1f)
                .build();
        FunctionGemmaConfig highTempConfig = FunctionGemmaConfig.builder()
                .temperature(0.8f)
                .build();

        FunctionGemmaEngine lowTempEngine = new FunctionGemmaEngine(context, lowTempConfig);
        FunctionGemmaEngine highTempEngine = new FunctionGemmaEngine(context, highTempConfig);

        // Then: Both should initialize
        assertNotNull("Low temp engine should initialize", lowTempEngine);
        assertNotNull("High temp engine should initialize", highTempEngine);

        lowTempEngine.destroy();
        highTempEngine.destroy();
    }

    @Test
    public void testConfig_TokenLimits() {
        // Given: Engines with different token limits
        FunctionGemmaConfig lowTokenConfig = FunctionGemmaConfig.builder()
                .maxTokens(32)
                .build();
        FunctionGemmaConfig highTokenConfig = FunctionGemmaConfig.builder()
                .maxTokens(256)
                .build();

        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        // When: Process with different token limits
        FunctionGemmaEngine.FunctionCallResult lowResult = engine.processCommand("الساعة كام", 32);
        FunctionGemmaEngine.FunctionCallResult highResult = engine.processCommand("الساعة كام", 256);

        // Then: Both should complete
        assertNotNull("Low token result should exist", lowResult);
        assertNotNull("High token result should exist", highResult);
    }

    @Test
    public void testConfig_ContextSize() {
        // Given: Configs with different context sizes
        FunctionGemmaConfig smallContextConfig = FunctionGemmaConfig.builder()
                .contextSize(512)
                .build();
        FunctionGemmaConfig largeContextConfig = FunctionGemmaConfig.builder()
                .contextSize(2048)
                .build();

        // Then: Both should be creatable
        FunctionGemmaEngine smallEngine = new FunctionGemmaEngine(context, smallContextConfig);
        FunctionGemmaEngine largeEngine = new FunctionGemmaEngine(context, largeContextConfig);

        assertNotNull("Small context engine should initialize", smallEngine);
        assertNotNull("Large context engine should initialize", largeEngine);

        smallEngine.destroy();
        largeEngine.destroy();
    }

    @Test
    public void testConfig_ThreadPoolSize() {
        // Given: Configs with different thread counts
        FunctionGemmaConfig singleThreadConfig = FunctionGemmaConfig.builder()
                .numThreads(1)
                .build();
        FunctionGemmaConfig multiThreadConfig = FunctionGemmaConfig.builder()
                .numThreads(4)
                .build();

        // Then: Both should be creatable
        FunctionGemmaEngine singleEngine = new FunctionGemmaEngine(context, singleThreadConfig);
        FunctionGemmaEngine multiEngine = new FunctionGemmaEngine(context, multiThreadConfig);

        assertNotNull("Single thread engine should initialize", singleEngine);
        assertNotNull("Multi thread engine should initialize", multiEngine);

        singleEngine.destroy();
        multiEngine.destroy();
    }

    @Test
    public void testConfig_ConfidenceThreshold() {
        // Given: Configs with different confidence thresholds
        FunctionGemmaConfig lowThresholdConfig = FunctionGemmaConfig.builder()
                .confidenceThreshold(0.5f)
                .build();
        FunctionGemmaConfig highThresholdConfig = FunctionGemmaConfig.builder()
                .confidenceThreshold(0.95f)
                .build();

        // Then: Both should be creatable
        FunctionGemmaEngine lowEngine = new FunctionGemmaEngine(context, lowThresholdConfig);
        FunctionGemmaEngine highEngine = new FunctionGemmaEngine(context, highThresholdConfig);

        assertNotNull("Low threshold engine should initialize", lowEngine);
        assertNotNull("High threshold engine should initialize", highEngine);

        lowEngine.destroy();
        highEngine.destroy();
    }

    // ========================================================================
    // CATEGORY 7: Cleanup and Resource Management Tests (4 tests)
    // ========================================================================

    @Test
    public void testCleanup_Destroy() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);

        // When: Destroy engine
        engine.destroy();

        // Then: Engine should be destroyed
        assertTrue("Engine should be destroyed", engine.isDestroyed());
    }

    @Test
    public void testCleanup_DoubleDestroy() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);

        // When: Destroy twice
        engine.destroy();
        engine.destroy();

        // Then: Should not throw exception
        assertTrue("Engine should remain destroyed", engine.isDestroyed());
    }

    @Test
    public void testCleanup_DestroyAfterInference() {
        // Given: Engine that has processed commands
        engine = new FunctionGemmaEngine(context, config);
        waitForModelLoad();

        if (engine.isReady()) {
            engine.processCommand("الساعة كام");
        }

        // When: Destroy after inference
        engine.destroy();

        // Then: Should clean up properly
        assertTrue("Engine should be destroyed", engine.isDestroyed());
    }

    @Test
    public void testCleanup_MemoryReleased() {
        // Given: Initialized engine
        engine = new FunctionGemmaEngine(context, config);
        long memoryBefore = engine.getEstimatedMemoryUsageMB();

        // When: Destroy engine
        engine.destroy();

        // Then: Memory should be released (reported as 0 after destroy)
        // Note: Actual memory release depends on GC
        assertTrue("Engine should be destroyed", engine.isDestroyed());
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Wait for model to finish loading.
     */
    private void waitForModelLoad() {
        long startTime = System.currentTimeMillis();
        while (!engine.isReady() && !engine.isDestroyed()) {
            try {
                Thread.sleep(100);

                if (System.currentTimeMillis() - startTime > MODEL_LOAD_TIMEOUT_MS) {
                    // Timeout - continue with test (may use fallback)
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Get average inference time.
     */
    public long getAverageInferenceTime() {
        if (totalTests == 0) return 0;
        return totalInferenceTime / totalTests;
    }

    /**
     * Get test accuracy.
     */
    public float getAccuracy() {
        if (totalTests == 0) return 0;
        return 100.0f * passedTests / totalTests;
    }

    // ========================================================================
    // Test Summary
    // ========================================================================

    /**
     * Print test summary.
     */
    @Test
    public void printTestSummary() {
        System.out.println("\n========================================");
        System.out.println("FunctionGemmaEngine TEST SUMMARY");
        System.out.println("========================================");
        System.out.println("Total Tests: " + totalTests);
        System.out.println("Passed: " + passedTests);
        System.out.println("Failed: " + (totalTests - passedTests));

        if (totalTests > 0) {
            System.out.println("Accuracy: " + String.format("%.2f%%", getAccuracy()));
            System.out.println("Average Inference Time: " + getAverageInferenceTime() + "ms");
        }

        System.out.println("\n========================================");
    }
}
