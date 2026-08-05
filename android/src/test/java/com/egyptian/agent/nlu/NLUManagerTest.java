package com.egyptian.agent.nlu;

import android.content.Context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.robolectric.annotation.Config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * NLUManager Comprehensive Test Suite
 * 
 * Tests for the Natural Language Understanding Manager.
 * Covers hybrid classification, fallback mechanisms, and lifecycle management.
 * 
 * Coverage Target: 90%
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NLUManager Tests")
class NLUManagerTest {

    @Mock
    private Context mockContext;

    @Mock
    private Context mockApplicationContext;

    private NLUManager nluManager;

    @BeforeEach
    void setUp() {
        // lenient: many tests never touch the context mock
        lenient().when(mockContext.getApplicationContext()).thenReturn(mockApplicationContext);
    }

    @Nested
    @DisplayName("Singleton and Initialization Tests")
    class SingletonInitializationTests {

        @Test
        @DisplayName("getInstance creates singleton instance")
        void testGetInstanceCreatesSingleton() {
            NLUManager instance1 = NLUManager.getInstance(mockContext);
            NLUManager instance2 = NLUManager.getInstance(mockContext);
            
            assertSame(instance1, instance2, "Should return same singleton instance");
        }

        @Test
        @DisplayName("initialize sets up manager")
        void testInitialize() {
            nluManager = NLUManager.getInstance(mockContext);
            
            assertDoesNotThrow(() -> nluManager.initialize());
        }

        @Test
        @DisplayName("initialize with Llama enabled")
        void testInitializeWithLlamaEnabled() {
            nluManager = NLUManager.getInstance(mockContext);
            
            assertDoesNotThrow(() -> nluManager.initialize(true));
        }

        @Test
        @DisplayName("initialize with Llama disabled")
        void testInitializeWithLlamaDisabled() {
            nluManager = NLUManager.getInstance(mockContext);
            
            assertDoesNotThrow(() -> nluManager.initialize(false));
        }

        @Test
        @DisplayName("double initialization is safe")
        void testDoubleInitialization() {
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize();
            
            assertDoesNotThrow(() -> nluManager.initialize());
        }

        @Test
        @DisplayName("destroy cleans up resources")
        void testDestroy() {
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize();
            
            assertDoesNotThrow(() -> nluManager.destroy());
        }

        @Test
        @DisplayName("double destroy is safe")
        void testDoubleDestroy() {
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize();
            nluManager.destroy();
            
            assertDoesNotThrow(() -> nluManager.destroy());
        }
    }

    @Nested
    @DisplayName("Classification Tests")
    class ClassificationTests {

        @BeforeEach
        void setUp() {
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize(false); // Use rule-based only for tests
        }

        @AfterEach
        void tearDown() {
            if (nluManager != null) {
                nluManager.destroy();
            }
        }

        @Test
        @DisplayName("classify null text returns UNKNOWN")
        void testClassifyNullText() {
            IntentResult result = nluManager.classify(null);
            
            assertEquals(IntentType.UNKNOWN, result.getIntentType());
        }

        @Test
        @DisplayName("classify empty text returns UNKNOWN")
        void testClassifyEmptyText() {
            IntentResult result = nluManager.classify("");
            
            assertEquals(IntentType.UNKNOWN, result.getIntentType());
        }

        @Test
        @DisplayName("classify emergency text returns EMERGENCY")
        void testClassifyEmergencyText() {
            IntentResult result = nluManager.classify("نجدة ساعدني");
            
            assertEquals(IntentType.EMERGENCY, result.getIntentType());
            assertTrue(result.getConfidence() >= 0.5f);
        }

        @Test
        @DisplayName("classify call command returns CALL_CONTACT")
        void testClassifyCallCommand() {
            IntentResult result = nluManager.classify("كلم ماما");
            
            assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
        }

        @Test
        @DisplayName("classify WhatsApp command returns SEND_WHATSAPP")
        void testClassifyWhatsAppCommand() {
            IntentResult result = nluManager.classify("ابعت واتساب لبابا");
            
            assertEquals(IntentType.SEND_WHATSAPP, result.getIntentType());
        }

        @Test
        @DisplayName("classify alarm command returns SET_ALARM")
        void testClassifyAlarmCommand() {
            IntentResult result = nluManager.classify("نبهني الصبح");
            
            assertEquals(IntentType.SET_ALARM, result.getIntentType());
        }

        @Test
        @DisplayName("classify time query returns READ_TIME")
        void testClassifyTimeQuery() {
            IntentResult result = nluManager.classify("الساعة كام");
            
            assertEquals(IntentType.READ_TIME, result.getIntentType());
        }

        @Test
        @DisplayName("classify greeting returns GREETING")
        void testClassifyGreeting() {
            IntentResult result = nluManager.classify("السلام عليكم");
            
            assertEquals(IntentType.GREETING, result.getIntentType());
            assertTrue(result.getConfidence() >= 0.9f);
        }

        @Test
        @DisplayName("classify thank you returns THANK_YOU")
        void testClassifyThankYou() {
            IntentResult result = nluManager.classify("شكرا");
            
            assertEquals(IntentType.THANK_YOU, result.getIntentType());
        }

        @Test
        @DisplayName("classify goodbye returns GOODBYE")
        void testClassifyGoodbye() {
            IntentResult result = nluManager.classify("مع السلامة");
            
            assertEquals(IntentType.GOODBYE, result.getIntentType());
        }

        @Test
        @DisplayName("classify WiFi command returns TOGGLE_WIFI")
        void testClassifyWiFiCommand() {
            IntentResult result = nluManager.classify("شغل الواي فاي");
            
            assertEquals(IntentType.TOGGLE_WIFI, result.getIntentType());
        }

        @Test
        @DisplayName("classify Bluetooth command returns TOGGLE_BLUETOOTH")
        void testClassifyBluetoothCommand() {
            IntentResult result = nluManager.classify("اقفل البلوتوث");
            
            assertEquals(IntentType.TOGGLE_BLUETOOTH, result.getIntentType());
        }

        @Test
        @DisplayName("unknown command returns UNKNOWN")
        void testClassifyUnknownCommand() {
            IntentResult result = nluManager.classify("كلام عشوائي غير مفهوم");
            
            assertEquals(IntentType.UNKNOWN, result.getIntentType());
        }
    }

    @Nested
    @DisplayName("Async Classification Tests")
    class AsyncClassificationTests {

        @BeforeEach
        void setUp() {
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize(false);
        }

        @AfterEach
        void tearDown() {
            if (nluManager != null) {
                nluManager.destroy();
            }
        }

        @Test
        @DisplayName("classifyAsync calls callback with result")
        void testClassifyAsyncWithCallback() throws InterruptedException {
            final boolean[] callbackCalled = {false};
            final IntentResult[] resultHolder = {null};

            nluManager.classifyAsync("كلم ماما", new NLUManager.ClassificationCallback() {
                @Override
                public void onResult(IntentResult result) {
                    resultHolder[0] = result;
                    callbackCalled[0] = true;
                }

                @Override
                public void onError(Exception error) {
                    callbackCalled[0] = true;
                }
            });

            // Wait for async execution
            Thread.sleep(500);

            assertTrue(callbackCalled[0], "Callback should be called");
            assertNotNull(resultHolder[0], "Result should not be null");
        }

        @Test
        @DisplayName("classifyAsync handles null callback gracefully")
        void testClassifyAsyncWithNullCallback() {
            assertDoesNotThrow(() -> nluManager.classifyAsync("كلم ماما", null));
        }

        @Test
        @DisplayName("classifyAsync handles exception in callback")
        void testClassifyAsyncWithException() {
            assertDoesNotThrow(() -> {
                nluManager.classifyAsync(null, new NLUManager.ClassificationCallback() {
                    @Override
                    public void onResult(IntentResult result) {
                        // Expected
                    }

                    @Override
                    public void onError(Exception error) {
                        // Expected
                    }
                });
            });
        }
    }

    @Nested
    @DisplayName("Llama Availability Tests")
    class LlamaAvailabilityTests {

        @BeforeEach
        void setUp() {
            nluManager = NLUManager.getInstance(mockContext);
        }

        @AfterEach
        void tearDown() {
            if (nluManager != null) {
                nluManager.destroy();
            }
        }

        @Test
        @DisplayName("isLlamaAvailable returns false when not initialized")
        void testIsLlamaAvailableNotInitialized() {
            assertFalse(nluManager.isLlamaAvailable());
        }

        @Test
        @DisplayName("isLlamaAvailable returns false when Llama disabled")
        void testIsLlamaAvailableDisabled() {
            nluManager.initialize(false);
            assertFalse(nluManager.isLlamaAvailable());
        }

        @Test
        @DisplayName("setUseLlama updates configuration")
        void testSetUseLlama() {
            nluManager.initialize(false);
            nluManager.setUseLlama(true);
            // Note: Llama won't actually be available without proper initialization
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @BeforeEach
        void setUp() {
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize(false);
        }

        @AfterEach
        void tearDown() {
            if (nluManager != null) {
                nluManager.destroy();
            }
        }

        @Test
        @DisplayName("classify completes within time limit")
        void testClassifyPerformance() {
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < 100; i++) {
                nluManager.classify("كلم ماما");
            }
            
            long duration = System.currentTimeMillis() - startTime;
            assertTrue(duration < 5000, "100 classifications should complete in under 5 seconds");
        }

        @Test
        @DisplayName("getLastClassificationTime returns value")
        void testGetLastClassificationTime() {
            nluManager.classify("كلم ماما");
            long time = nluManager.getLastClassificationTime();
            
            assertTrue(time >= 0, "Classification time should be non-negative");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("classify before initialization uses fallback")
        void testClassifyBeforeInitialization() {
            nluManager = NLUManager.getInstance(mockContext);
            // Don't initialize
            
            IntentResult result = nluManager.classify("كلم ماما");
            
            assertNotNull(result);
            assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
        }

        @Test
        @DisplayName("destroy after getInstance without initialize")
        void testDestroyWithoutInitialize() {
            nluManager = NLUManager.getInstance(mockContext);
            
            assertDoesNotThrow(() -> nluManager.destroy());
        }

        @Test
        @DisplayName("classify after destroy handles gracefully")
        void testClassifyAfterDestroy() {
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize();
            nluManager.destroy();
            
            // Should not throw exception
            IntentResult result = nluManager.classify("كلم ماما");
            
            // Result may be null or UNKNOWN depending on implementation
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Confidence Threshold Tests")
    class ConfidenceThresholdTests {

        @BeforeEach
        void setUp() {
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize(false);
        }

        @AfterEach
        void tearDown() {
            if (nluManager != null) {
                nluManager.destroy();
            }
        }

        @Test
        @DisplayName("emergency intent has high confidence")
        void testEmergencyConfidence() {
            IntentResult result = nluManager.classify("نجدة استغاثة");
            
            assertTrue(result.getConfidence() >= 0.75f, "Emergency should have high confidence");
        }

        @Test
        @DisplayName("greeting intent has high confidence")
        void testGreetingConfidence() {
            IntentResult result = nluManager.classify("السلام عليكم");
            
            assertTrue(result.getConfidence() >= 0.9f, "Greeting should have high confidence");
        }

        @Test
        @DisplayName("unknown intent has low confidence")
        void testUnknownConfidence() {
            IntentResult result = nluManager.classify("كلام غير مفهوم");
            
            assertTrue(result.getConfidence() < 0.5f, "Unknown should have low confidence");
        }
    }

    @Nested
    @DisplayName("Entity Extraction Tests")
    class EntityExtractionTests {

        @BeforeEach
        void setUp() {
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize(false);
        }

        @AfterEach
        void tearDown() {
            if (nluManager != null) {
                nluManager.destroy();
            }
        }

        @Test
        @DisplayName("call command extracts contact entity")
        void testCallExtractsContact() {
            IntentResult result = nluManager.classify("كلم أحمد محمد");
            
            assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
            assertNotNull(result.getEntity("contact"));
        }

        @Test
        @DisplayName("alarm command extracts time entity")
        void testAlarmExtractsTime() {
            IntentResult result = nluManager.classify("نبهني الساعة 7 الصبح");
            
            assertEquals(IntentType.SET_ALARM, result.getIntentType());
            assertNotNull(result.getEntity("time"));
        }

        @Test
        @DisplayName("WhatsApp command extracts contact entity")
        void testWhatsAppExtractsContact() {
            IntentResult result = nluManager.classify("ابعت واتساب لماما");
            
            assertEquals(IntentType.SEND_WHATSAPP, result.getIntentType());
            assertNotNull(result.getEntity("contact"));
        }

        @Test
        @DisplayName("result contains original text")
        void testOriginalTextPreserved() {
            String input = "كلم ماما";
            IntentResult result = nluManager.classify(input);
            
            assertEquals(input, result.getOriginalText());
        }

        @Test
        @DisplayName("result contains processing time")
        void testProcessingTimeRecorded() {
            IntentResult result = nluManager.classify("كلم ماما");
            
            assertTrue(result.getProcessingTimeMs() >= 0, "Processing time should be recorded");
        }
    }

    @Nested
    @DisplayName("Intent Type Coverage Tests")
    class IntentTypeCoverageTests {

        @BeforeEach
        void setUp() {
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize(false);
        }

        @AfterEach
        void tearDown() {
            if (nluManager != null) {
                nluManager.destroy();
            }
        }

        @Test
        @DisplayName("all communication intents are recognized")
        void testCommunicationIntents() {
            IntentResult call = nluManager.classify("كلم ماما");
            IntentResult whatsapp = nluManager.classify("ابعت واتساب");
            IntentResult sms = nluManager.classify("ابعت رسالة");
            
            assertEquals(IntentType.CALL_CONTACT, call.getIntentType());
            assertEquals(IntentType.SEND_WHATSAPP, whatsapp.getIntentType());
        }

        @Test
        @DisplayName("all system control intents are recognized")
        void testSystemControlIntents() {
            IntentResult alarm = nluManager.classify("نبهني");
            IntentResult time = nluManager.classify("الساعة كام");
            IntentResult wifi = nluManager.classify("شغل الواي فاي");
            IntentResult bluetooth = nluManager.classify("اقفل البلوتوث");
            
            assertEquals(IntentType.SET_ALARM, alarm.getIntentType());
            assertEquals(IntentType.READ_TIME, time.getIntentType());
            assertEquals(IntentType.TOGGLE_WIFI, wifi.getIntentType());
            assertEquals(IntentType.TOGGLE_BLUETOOTH, bluetooth.getIntentType());
        }

        @Test
        @DisplayName("all conversation intents are recognized")
        void testConversationIntents() {
            IntentResult greeting = nluManager.classify("ازيك");
            IntentResult thanks = nluManager.classify("شكرا");
            IntentResult goodbye = nluManager.classify("مع السلامة");
            
            assertEquals(IntentType.GREETING, greeting.getIntentType());
            assertEquals(IntentType.THANK_YOU, thanks.getIntentType());
            assertEquals(IntentType.GOODBYE, goodbye.getIntentType());
        }

        @Test
        @DisplayName("emergency intent is highest priority")
        void testEmergencyPriority() {
            // Emergency keywords in any context should trigger emergency
            IntentResult result = nluManager.classify("كلم ماما نجدة ساعدني");
            
            assertEquals(IntentType.EMERGENCY, result.getIntentType());
        }
    }
}
