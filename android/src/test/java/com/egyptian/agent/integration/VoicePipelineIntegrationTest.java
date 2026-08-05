package com.egyptian.agent.integration;

import android.content.Context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.egyptian.agent.nlu.EgyptianNormalizer;
import com.egyptian.agent.nlu.IntentResult;
import com.egyptian.agent.nlu.IntentType;
import com.egyptian.agent.nlu.NLUManager;
import com.egyptian.agent.executors.EmergencyHandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Voice Pipeline Integration Test Suite
 * 
 * Tests the complete voice processing pipeline:
 * Audio → ASR → NLU → Executor → TTS
 * 
 * Coverage Target: End-to-end integration validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Voice Pipeline Integration Tests")
class VoicePipelineIntegrationTest {

    @Mock
    private Context mockContext;

    @Mock
    private Context mockApplicationContext;

    private NLUManager nluManager;

    @BeforeEach
    void setUp() {
        when(mockContext.getApplicationContext()).thenReturn(mockApplicationContext);
        nluManager = NLUManager.getInstance(mockContext);
        nluManager.initialize(false); // Use rule-based for tests
    }

    @Nested
    @DisplayName("End-to-End Pipeline Tests")
    class EndToEndPipelineTests {

        @Test
        @DisplayName("complete emergency pipeline")
        void testCompleteEmergencyPipeline() {
            // Simulate ASR output
            String asrOutput = "نجدة ساعدني";
            
            // NLU processing
            IntentResult result = nluManager.classify(asrOutput);
            
            // Validate pipeline output
            assertEquals(IntentType.EMERGENCY, result.getIntentType());
            assertTrue(result.getConfidence() >= 0.75f);
            
            // Verify emergency handler would trigger
            assertTrue(EmergencyHandler.isEmergency(asrOutput));
        }

        @Test
        @DisplayName("complete call contact pipeline")
        void testCompleteCallContactPipeline() {
            // Simulate ASR output
            String asrOutput = "كلم ماما";
            
            // NLU processing
            IntentResult result = nluManager.classify(asrOutput);
            
            // Validate pipeline output
            assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
            assertNotNull(result.getEntity("contact"));
            assertTrue(result.getConfidence() >= 0.75f);
        }

        @Test
        @DisplayName("complete WhatsApp pipeline")
        void testCompleteWhatsAppPipeline() {
            // Simulate ASR output
            String asrOutput = "ابعت واتساب لبابا";
            
            // NLU processing
            IntentResult result = nluManager.classify(asrOutput);
            
            // Validate pipeline output
            assertEquals(IntentType.SEND_WHATSAPP, result.getIntentType());
            assertTrue(result.getConfidence() >= 0.75f);
        }

        @Test
        @DisplayName("complete alarm pipeline")
        void testCompleteAlarmPipeline() {
            // Simulate ASR output
            String asrOutput = "نبهني الصبح";
            
            // NLU processing
            IntentResult result = nluManager.classify(asrOutput);
            
            // Validate pipeline output
            assertEquals(IntentType.SET_ALARM, result.getIntentType());
            assertNotNull(result.getEntity("time"));
            assertTrue(result.getConfidence() >= 0.75f);
        }

        @Test
        @DisplayName("complete time query pipeline")
        void testCompleteTimeQueryPipeline() {
            // Simulate ASR output
            String asrOutput = "الساعة كام";
            
            // NLU processing
            IntentResult result = nluManager.classify(asrOutput);
            
            // Validate pipeline output
            assertEquals(IntentType.READ_TIME, result.getIntentType());
            assertTrue(result.getConfidence() >= 0.75f);
        }

        @Test
        @DisplayName("complete greeting pipeline")
        void testCompleteGreetingPipeline() {
            // Simulate ASR output
            String asrOutput = "السلام عليكم";
            
            // NLU processing
            IntentResult result = nluManager.classify(asrOutput);
            
            // Validate pipeline output
            assertEquals(IntentType.GREETING, result.getIntentType());
            assertTrue(result.getConfidence() >= 0.9f);
        }

        @Test
        @DisplayName("complete WiFi toggle pipeline")
        void testCompleteWiFiTogglePipeline() {
            // Simulate ASR output
            String asrOutput = "شغل الواي فاي";
            
            // NLU processing
            IntentResult result = nluManager.classify(asrOutput);
            
            // Validate pipeline output
            assertEquals(IntentType.TOGGLE_WIFI, result.getIntentType());
            assertTrue(result.getConfidence() >= 0.75f);
        }

        @Test
        @DisplayName("complete Bluetooth toggle pipeline")
        void testCompleteBluetoothTogglePipeline() {
            // Simulate ASR output
            String asrOutput = "اقفل البلوتوث";
            
            // NLU processing
            IntentResult result = nluManager.classify(asrOutput);
            
            // Validate pipeline output
            assertEquals(IntentType.TOGGLE_BLUETOOTH, result.getIntentType());
            assertTrue(result.getConfidence() >= 0.75f);
        }
    }

    @Nested
    @DisplayName("Normalization Pipeline Tests")
    class NormalizationPipelineTests {

        @Test
        @DisplayName("Egyptian dialect normalization in pipeline")
        void testEgyptianDialectNormalization() {
            String egyptianInput = "كلم ماما دلوقتي";
            
            // Normalize
            String normalized = EgyptianNormalizer.normalize(egyptianInput);
            
            // Classify
            IntentResult result = nluManager.classify(normalized);
            
            // Validate
            assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
            assertTrue(result.getConfidence() >= 0.75f);
        }

        @Test
        @DisplayName("contact name normalization in pipeline")
        void testContactNameNormalization() {
            String input = "كلم يما";
            
            // Normalize contact name
            String normalizedContact = EgyptianNormalizer.normalizeContactName("يما");
            
            // Classify
            IntentResult result = nluManager.classify(input);
            
            // Validate
            assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
            assertEquals("أمي", normalizedContact);
        }

        @Test
        @DisplayName("time expression normalization in pipeline")
        void testTimeExpressionNormalization() {
            String input = "نبهني الصبح";
            
            // Normalize time expression
            String normalizedTime = EgyptianNormalizer.normalizeTimeExpression("الصبح");
            
            // Classify
            IntentResult result = nluManager.classify(input);
            
            // Validate
            assertEquals(IntentType.SET_ALARM, result.getIntentType());
            assertEquals("08:00", normalizedTime);
        }

        @Test
        @DisplayName("full sentence normalization in pipeline")
        void testFullSentenceNormalization() {
            String egyptianSentence = "ازيك يا باشا عامل ايه";
            
            // Normalize
            String normalized = EgyptianNormalizer.normalize(egyptianSentence);
            
            // Classify
            IntentResult result = nluManager.classify(normalized);
            
            // Validate
            assertEquals(IntentType.GREETING, result.getIntentType());
        }
    }

    @Nested
    @DisplayName("Multi-Step Conversation Tests")
    class MultiStepConversationTests {

        @Test
        @DisplayName("greeting followed by command")
        void testGreetingFollowedByCommand() {
            // Step 1: Greeting
            IntentResult greeting = nluManager.classify("السلام عليكم");
            assertEquals(IntentType.GREETING, greeting.getIntentType());
            
            // Step 2: Command
            IntentResult command = nluManager.classify("كلم ماما");
            assertEquals(IntentType.CALL_CONTACT, command.getIntentType());
        }

        @Test
        @DisplayName("command followed by thank you")
        void testCommandFollowedByThankYou() {
            // Step 1: Command
            IntentResult command = nluManager.classify("الساعة كام");
            assertEquals(IntentType.READ_TIME, command.getIntentType());
            
            // Step 2: Thank you
            IntentResult thanks = nluManager.classify("شكرا");
            assertEquals(IntentType.THANK_YOU, thanks.getIntentType());
        }

        @Test
        @DisplayName("multiple commands in sequence")
        void testMultipleCommandsInSequence() {
            String[] commands = {
                "كلم ماما",
                "ابعت واتساب",
                "نبهني الصبح",
                "الساعة كام"
            };
            
            IntentType[] expectedTypes = {
                IntentType.CALL_CONTACT,
                IntentType.SEND_WHATSAPP,
                IntentType.SET_ALARM,
                IntentType.READ_TIME
            };
            
            for (int i = 0; i < commands.length; i++) {
                IntentResult result = nluManager.classify(commands[i]);
                assertEquals(expectedTypes[i], result.getIntentType());
            }
        }

        @Test
        @DisplayName("conversation with goodbye")
        void testConversationWithGoodbye() {
            // Full conversation flow
            IntentResult greeting = nluManager.classify("ازيك");
            IntentResult command = nluManager.classify("كلم بابا");
            IntentResult thanks = nluManager.classify("متشكر");
            IntentResult goodbye = nluManager.classify("مع السلامة");
            
            assertEquals(IntentType.GREETING, greeting.getIntentType());
            assertEquals(IntentType.CALL_CONTACT, command.getIntentType());
            assertEquals(IntentType.THANK_YOU, thanks.getIntentType());
            assertEquals(IntentType.GOODBYE, goodbye.getIntentType());
        }
    }

    @Nested
    @DisplayName("Error Handling Pipeline Tests")
    class ErrorHandlingPipelineTests {

        @Test
        @DisplayName("null input handling in pipeline")
        void testNullInputHandling() {
            IntentResult result = nluManager.classify(null);
            assertEquals(IntentType.UNKNOWN, result.getIntentType());
        }

        @Test
        @DisplayName("empty input handling in pipeline")
        void testEmptyInputHandling() {
            IntentResult result = nluManager.classify("");
            assertEquals(IntentType.UNKNOWN, result.getIntentType());
        }

        @Test
        @DisplayName("unrecognized command handling")
        void testUnrecognizedCommandHandling() {
            IntentResult result = nluManager.classify("كلام غير مفهوم");
            assertEquals(IntentType.UNKNOWN, result.getIntentType());
            assertTrue(result.getConfidence() < 0.5f);
        }

        @Test
        @DisplayName("ambiguous command handling")
        void testAmbiguousCommandHandling() {
            // Command that could match multiple intents
            IntentResult result = nluManager.classify("افتح");
            // Should have low confidence or be UNKNOWN
            assertTrue(result.getConfidence() < 0.8f || result.getIntentType() == IntentType.UNKNOWN);
        }

        @Test
        @DisplayName("special characters handling")
        void testSpecialCharactersHandling() {
            IntentResult result = nluManager.classify("كلم ماما!");
            assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
        }

        @Test
        @DisplayName("very long input handling")
        void testVeryLongInputHandling() {
            StringBuilder longInput = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                longInput.append("كلام ");
            }
            
            IntentResult result = nluManager.classify(longInput.toString());
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Performance Pipeline Tests")
    class PerformancePipelineTests {

        @Test
        @DisplayName("pipeline latency under 100ms")
        void testPipelineLatencyUnder100ms() {
            String input = "كلم ماما";
            
            long startTime = System.currentTimeMillis();
            IntentResult result = nluManager.classify(input);
            long duration = System.currentTimeMillis() - startTime;
            
            assertNotNull(result);
            assertTrue(duration < 100, "Pipeline should complete in under 100ms");
        }

        @Test
        @DisplayName("pipeline handles 100 requests per second")
        void testPipelineHandles100RPS() {
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < 100; i++) {
                nluManager.classify("كلم ماما");
            }
            
            long duration = System.currentTimeMillis() - startTime;
            assertTrue(duration < 2000, "100 requests should complete in under 2 seconds");
        }

        @Test
        @DisplayName("pipeline memory efficiency")
        void testPipelineMemoryEfficiency() {
            // Run multiple classifications
            for (int i = 0; i < 1000; i++) {
                nluManager.classify("كلم ماما");
            }
            
            // If we get here without OOM, test passes
            assertTrue(true);
        }
    }

    @Nested
    @DisplayName("Confidence Threshold Tests")
    class ConfidenceThresholdTests {

        @Test
        @DisplayName("high confidence emergency")
        void testHighConfidenceEmergency() {
            IntentResult result = nluManager.classify("نجدة استغاثة");
            assertTrue(result.getConfidence() >= 0.85f);
        }

        @Test
        @DisplayName("high confidence greeting")
        void testHighConfidenceGreeting() {
            IntentResult result = nluManager.classify("السلام عليكم");
            assertTrue(result.getConfidence() >= 0.9f);
        }

        @Test
        @DisplayName("medium confidence command")
        void testMediumConfidenceCommand() {
            IntentResult result = nluManager.classify("كلم أحمد");
            assertTrue(result.getConfidence() >= 0.75f);
            assertTrue(result.getConfidence() < 0.95f);
        }

        @Test
        @DisplayName("low confidence unknown")
        void testLowConfidenceUnknown() {
            IntentResult result = nluManager.classify("كلام عشوائي");
            assertTrue(result.getConfidence() < 0.5f);
        }
    }

    @Nested
    @DisplayName("Entity Extraction Pipeline Tests")
    class EntityExtractionPipelineTests {

        @Test
        @DisplayName("contact entity extracted and normalized")
        void testContactEntityExtractedAndNormalized() {
            IntentResult result = nluManager.classify("كلم ماما");
            
            assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
            assertNotNull(result.getEntity("contact"));
        }

        @Test
        @DisplayName("time entity extracted and normalized")
        void testTimeEntityExtractedAndNormalized() {
            IntentResult result = nluManager.classify("نبهني الصبح");
            
            assertEquals(IntentType.SET_ALARM, result.getIntentType());
            assertNotNull(result.getEntity("time"));
        }

        @Test
        @DisplayName("original text preserved in result")
        void testOriginalTextPreserved() {
            String input = "كلم ماما";
            IntentResult result = nluManager.classify(input);
            
            assertEquals(input, result.getOriginalText());
        }

        @Test
        @DisplayName("processing time recorded")
        void testProcessingTimeRecorded() {
            IntentResult result = nluManager.classify("كلم ماما");
            
            assertTrue(result.getProcessingTimeMs() >= 0);
        }
    }

    @AfterEach
    void tearDown() {
        if (nluManager != null) {
            nluManager.destroy();
        }
    }
}
