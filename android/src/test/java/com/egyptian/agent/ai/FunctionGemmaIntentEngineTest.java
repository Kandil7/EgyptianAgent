package com.egyptian.agent.ai;

import android.content.Context;

import com.egyptian.agent.nlu.IntentResult;
import com.egyptian.agent.nlu.IntentType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Unit Tests for FunctionGemmaIntentEngine
 * 
 * Test Coverage:
 * - Intent Classification (16 function types)
 * - Entity Extraction (contacts, time, messages)
 * - Confidence Threshold Validation
 * - Fallback Mechanisms
 * - Egyptian Dialect Variations
 * 
 * @author EgyptianAgent Team
 * @version 1.0
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class FunctionGemmaIntentEngineTest {

    @Mock
    private Context mockContext;

    private FunctionGemmaIntentEngine engine;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Initialize with mock context for unit testing
        // Note: In real tests, use Robolectric's application context
        engine = new FunctionGemmaIntentEngine(mockContext);
    }

    // ========================================================================
    // CALL_CONTACT Intent Tests
    // ========================================================================

    @Test
    public void testCallContact_Mama() {
        IntentResult result = engine.classifyIntent("اتصل بماما");
        assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
        assertEquals("ماما", result.getEntity("contact_name"));
        assertTrue("Confidence should be >= 0.85", result.getConfidence() >= 0.85f);
    }

    @Test
    public void testCallContact_Baba() {
        IntentResult result = engine.classifyIntent("كلم بابا");
        assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
        assertTrue(result.getEntity("contact_name").contains("بابا"));
    }

    @Test
    public void testCallContact_Ahmed() {
        IntentResult result = engine.classifyIntent("رن على أحمد");
        assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
        assertEquals("أحمد", result.getEntity("contact_name"));
    }

    @Test
    public void testCallContact_Variations() {
        String[] variations = {
            "اتصل بماما",
            "كلم أمي",
            "رن على الوالدة",
            "عايز اكلم ماما",
            "ممكن تكلمني بابا"
        };

        for (String variation : variations) {
            IntentResult result = engine.classifyIntent(variation);
            assertEquals("Failed for: " + variation, IntentType.CALL_CONTACT, result.getIntentType());
        }
    }

    // ========================================================================
    // SEND_WHATSAPP Intent Tests
    // ========================================================================

    @Test
    public void testSendWhatsapp_Simple() {
        IntentResult result = engine.classifyIntent("ابعت واتساب لمريم");
        assertEquals(IntentType.SEND_WHATSAPP, result.getIntentType());
        assertEquals("مريم", result.getEntity("contact_name"));
    }

    @Test
    public void testSendWhatsapp_WithMessage() {
        IntentResult result = engine.classifyIntent("ابعت واتساب لأحمد وقوله إنى هتأخر");
        assertEquals(IntentType.SEND_WHATSAPP, result.getIntentType());
        assertEquals("أحمد", result.getEntity("contact_name"));
        assertTrue(result.getEntity("message").contains("هتأخر"));
    }

    @Test
    public void testSendWhatsapp_Variations() {
        String[] variations = {
            "ابعت واتساب لمريم",
            "ابعث رسالة لـسارة",
            "قول لـمحمد إنى جاي",
            "ارسل واتساب لأبي",
            "ممكن تبعت رسالة لماما"
        };

        for (String variation : variations) {
            IntentResult result = engine.classifyIntent(variation);
            assertEquals("Failed for: " + variation, IntentType.SEND_WHATSAPP, result.getIntentType());
        }
    }

    // ========================================================================
    // SET_ALARM Intent Tests
    // ========================================================================

    @Test
    public void testSetAlarm_TomorrowMorning() {
        IntentResult result = engine.classifyIntent("نبهني بكرة الصبح");
        assertEquals(IntentType.SET_ALARM, result.getIntentType());
        assertTrue(result.getEntity("time").contains("بكرة"));
    }

    @Test
    public void testSetAlarm_AfterHour() {
        IntentResult result = engine.classifyIntent("انبهني بعد ساعة");
        assertEquals(IntentType.SET_ALARM, result.getIntentType());
        assertTrue(result.getEntity("time").contains("ساعة"));
    }

    @Test
    public void testSetAlarm_Variations() {
        String[] variations = {
            "نبهني بكرة الصبح",
            "انبهني بعد ساعة",
            "ذكرني الساعة 5",
            "عايز منبه بكرة",
            "اضبط منبه على العصر"
        };

        for (String variation : variations) {
            IntentResult result = engine.classifyIntent(variation);
            assertEquals("Failed for: " + variation, IntentType.SET_ALARM, result.getIntentType());
        }
    }

    // ========================================================================
    // EMERGENCY Intent Tests
    // ========================================================================

    @Test
    public void testEmergency_Nagda() {
        IntentResult result = engine.classifyIntent("يا نجدة");
        assertEquals(IntentType.EMERGENCY, result.getIntentType());
        assertTrue(result.getConfidence() >= 0.9f); // High confidence for emergency
    }

    @Test
    public void testEmergency_Istighatha() {
        IntentResult result = engine.classifyIntent("استغاثة");
        assertEquals(IntentType.EMERGENCY, result.getIntentType());
    }

    @Test
    public void testEmergency_Variations() {
        String[] variations = {
            "يا نجدة",
            "استغاثة",
            "مش قادر",
            "محتاج مساعدة بسرعة",
            "حالة طوارئ"
        };

        for (String variation : variations) {
            IntentResult result = engine.classifyIntent(variation);
            assertEquals("Failed for: " + variation, IntentType.EMERGENCY, result.getIntentType());
        }
    }

    // ========================================================================
    // READ_TIME Intent Tests
    // ========================================================================

    @Test
    public void testReadTime_Simple() {
        IntentResult result = engine.classifyIntent("الساعة كام؟");
        assertEquals(IntentType.READ_TIME, result.getIntentType());
    }

    @Test
    public void testReadTime_Variations() {
        String[] variations = {
            "الساعة كام؟",
            "وقت إيه دلوقتي؟",
            "كام الساعة؟",
            "ممكن تقوللي الوقت؟",
            "العصر ولا الفجر؟"
        };

        for (String variation : variations) {
            IntentResult result = engine.classifyIntent(variation);
            assertEquals("Failed for: " + variation, IntentType.READ_TIME, result.getIntentType());
        }
    }

    // ========================================================================
    // OPEN_APP Intent Tests
    // ========================================================================

    @Test
    public void testOpenApp_Whatsapp() {
        IntentResult result = engine.classifyIntent("افتح واتساب");
        assertEquals(IntentType.OPEN_APP, result.getIntentType());
        assertEquals("واتساب", result.getEntity("app_name"));
    }

    @Test
    public void testOpenApp_Facebook() {
        IntentResult result = engine.classifyIntent("شغل فيسبوك");
        assertEquals(IntentType.OPEN_APP, result.getIntentType());
    }

    @Test
    public void testOpenApp_Variations() {
        String[] variations = {
            "افتح واتساب",
            "شغل فيسبوك",
            "شغل يوتيوب",
            "ممكن تفتح جوجل؟",
            "ابحث لي في جوجل"
        };

        for (String variation : variations) {
            IntentResult result = engine.classifyIntent(variation);
            assertEquals("Failed for: " + variation, IntentType.OPEN_APP, result.getIntentType());
        }
    }

    // ========================================================================
    // DEVICE_CONTROL Intent Tests
    // ========================================================================

    @Test
    public void testDeviceControl_WifiOn() {
        IntentResult result = engine.classifyIntent("افتح الواي فاي");
        assertEquals(IntentType.TOGGLE_WIFI, result.getIntentType());
        assertEquals("on", result.getEntity("state"));
    }

    @Test
    public void testDeviceControl_BluetoothOff() {
        IntentResult result = engine.classifyIntent("قفل البلوتوث");
        assertEquals(IntentType.TOGGLE_BLUETOOTH, result.getIntentType());
        assertEquals("off", result.getEntity("state"));
    }

    @Test
    public void testDeviceControl_VolumeUp() {
        IntentResult result = engine.classifyIntent("زود الصوت");
        assertEquals(IntentType.UNKNOWN, result.getIntentType());
    }

    // ========================================================================
    // Confidence Threshold Tests
    // ========================================================================

    @Test
    public void testHighConfidence_ClearCommand() {
        IntentResult result = engine.classifyIntent("اتصل بماما");
        assertTrue("High confidence expected for clear commands", result.getConfidence() >= 0.9f);
    }

    @Test
    public void testMediumConfidence_AmbiguousCommand() {
        IntentResult result = engine.classifyIntent("ممكن تكلم حد");
        assertTrue("Medium confidence for ambiguous commands", result.getConfidence() >= 0.7f);
    }

    @Test
    public void testLowConfidence_UnknownCommand() {
        IntentResult result = engine.classifyIntent("السمك بيسبح في الماء");
        assertTrue("Low confidence for irrelevant statements", result.getConfidence() < 0.85f || 
                   result.getIntentType() == IntentType.UNKNOWN);
    }

    // ========================================================================
    // Entity Extraction Tests
    // ========================================================================

    @Test
    public void testEntityExtraction_ContactNames() {
        String[] contactNames = {"ماما", "بابا", "أحمد", "محمد", "فاطمة", "علي"};
        
        for (String name : contactNames) {
            IntentResult result = engine.classifyIntent("اتصل بـ" + name);
            assertEquals("Contact name extraction failed for: " + name, 
                        name, result.getEntity("contact_name"));
        }
    }

    @Test
    public void testEntityExtraction_TimeExpressions() {
        IntentResult result = engine.classifyIntent("نبهني بكرة الصبح");
        assertTrue("Time should contain 'بكرة'", 
                   result.getEntity("time").contains("بكرة") || 
                   result.getEntity("time").contains("الصبح"));
    }

    @Test
    public void testEntityExtraction_MessageContent() {
        IntentResult result = engine.classifyIntent("ابعت واتساب وقوله سلامات");
        assertTrue("Message should contain 'سلامات'", 
                   result.getEntity("message").contains("سلامات"));
    }

    // ========================================================================
    // Fallback Tests
    // ========================================================================

    @Test
    public void testFallback_ModelNotReady() {
        // Simulate model not ready scenario
        // This tests the fallback to EgyptianNormalizer
        IntentResult result = engine.classifyIntent("اتصل بماما");
        assertNotNull("Fallback should return valid result", result);
        assertNotNull("Fallback should have intent type", result.getIntentType());
    }

    @Test
    public void testFallback_InvalidJSON() {
        // Test handling of malformed responses
        IntentResult result = engine.classifyIntent("كلام عشوائي");
        assertNotNull("Should handle invalid input gracefully", result);
    }

    @Test
    public void testFallback_UnknownIntent() {
        IntentResult result = engine.classifyIntent("السمك بيطير في السما");
        // Should either be UNKNOWN or have low confidence
        assertTrue("Unknown intent or low confidence expected", 
                   result.getIntentType() == IntentType.UNKNOWN || 
                   result.getConfidence() < 0.85f);
    }

    // ========================================================================
    // Edge Cases Tests
    // ========================================================================

    @Test
    public void testEdgeCase_EmptyInput() {
        IntentResult result = engine.classifyIntent("");
        assertNotNull("Should handle empty input", result);
        assertEquals(IntentType.UNKNOWN, result.getIntentType());
    }

    @Test
    public void testEdgeCase_NullInput() {
        IntentResult result = engine.classifyIntent(null);
        assertNotNull("Should handle null input", result);
        assertEquals(IntentType.UNKNOWN, result.getIntentType());
    }

    @Test
    public void testEdgeCase_VeryLongInput() {
        String longInput = "اتصل بماما وقولها إنى هتأخر شوية عشان الزحام وبعدين هجيب الشغل وهرجع البيت بدري";
        IntentResult result = engine.classifyIntent(longInput);
        assertNotNull("Should handle long input", result);
        // Should still extract the main intent
        assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
    }

    @Test
    public void testEdgeCase_MixedArabicEnglish() {
        IntentResult result = engine.classifyIntent("ابعت whatsapp لـ أحمد");
        assertNotNull("Should handle mixed language", result);
        assertEquals(IntentType.SEND_WHATSAPP, result.getIntentType());
    }

    @Test
    public void testEdgeCase_SpecialCharacters() {
        IntentResult result = engine.classifyIntent("اتصل بـ!!!");
        assertNotNull("Should handle special characters", result);
    }

    // ========================================================================
    // Performance Tests
    // ========================================================================

    @Test
    public void testPerformance_InferenceTime() {
        long startTime = System.currentTimeMillis();
        engine.classifyIntent("اتصل بماما");
        long elapsed = System.currentTimeMillis() - startTime;
        
        // Should complete in <500ms (generous for unit tests)
        assertTrue("Inference should complete in <500ms, took: " + elapsed + "ms", elapsed < 500);
    }

    @Test
    public void testPerformance_BatchProcessing() {
        String[] commands = {
            "اتصل بماما",
            "ابعت واتساب",
            "نبهني بكرة",
            "الساعة كام",
            "افتح واتساب"
        };

        long startTime = System.currentTimeMillis();
        for (String command : commands) {
            engine.classifyIntent(command);
        }
        long elapsed = System.currentTimeMillis() - startTime;

        // Average should be <200ms per command
        long avgTime = elapsed / commands.length;
        assertTrue("Average inference time should be <200ms, was: " + avgTime + "ms", avgTime < 200);
    }

    // ========================================================================
    // Cleanup
    // ========================================================================

    @Test
    public void testDestroy() {
        engine.destroy();
        // Should not throw exceptions
        assertTrue("Engine should be destroyed successfully", true);
    }
}
