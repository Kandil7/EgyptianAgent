package com.egyptian.agent.nlu;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive Egyptian Arabic NLU Test Suite
 * 
 * 100+ test phrases covering all intent categories
 * Target Accuracy: 97.8%
 * 
 * Test Categories:
 * 1. CALL_CONTACT (20 tests)
 * 2. SEND_WHATSAPP (15 tests)
 * 3. SET_ALARM (15 tests)
 * 4. READ_TIME (10 tests)
 * 5. EMERGENCY (10 tests)
 * 6. GREETING (10 tests)
 * 7. THANK_YOU (5 tests)
 * 8. GOODBYE (5 tests)
 * 9. TOGGLE_WIFI (5 tests)
 * 10. TOGGLE_BLUETOOTH (5 tests)
 * 11. OPEN_APP (5 tests)
 * 12. UNKNOWN/Edge Cases (10 tests)
 */
public class EgyptianNLUComprehensiveTest {

    private static final float CONFIDENCE_THRESHOLD = 0.5f;
    private static final float HIGH_CONFIDENCE_THRESHOLD = 0.85f;
    
    private RuleBasedClassifier classifier;
    private int totalTests;
    private int passedTests;
    private Map<IntentType, Integer> intentStats;
    private List<TestFailure> failures;

    @Before
    public void setUp() {
        classifier = new RuleBasedClassifier();
        totalTests = 0;
        passedTests = 0;
        intentStats = new HashMap<>();
        failures = new ArrayList<>();
        
        for (IntentType type : IntentType.values()) {
            intentStats.put(type, 0);
        }
    }

    /**
     * Test result container
     */
    private static class TestResult {
        String input;
        IntentType expectedIntent;
        String expectedEntity;
        IntentResult actualResult;
        boolean passed;
        
        TestResult(String input, IntentType expectedIntent, String expectedEntity, IntentResult actualResult) {
            this.input = input;
            this.expectedIntent = expectedIntent;
            this.expectedEntity = expectedEntity;
            this.actualResult = actualResult;
            this.passed = actualResult.getIntentType() == expectedIntent && 
                         (expectedEntity == null || actualResult.getEntity("contact") != null);
        }
    }

    /**
     * Test failure record
     */
    private static class TestFailure {
        String input;
        IntentType expected;
        IntentType actual;
        String reason;
        
        TestFailure(String input, IntentType expected, IntentType actual, String reason) {
            this.input = input;
            this.expected = expected;
            this.actual = actual;
            this.reason = reason;
        }
    }

    // ============================================================
    // CATEGORY 1: CALL_CONTACT (20 tests)
    // ============================================================
    
    @Test
    public void testCallContact_Standard() {
        // Standard call commands
        testIntent("اتصل بأمي", IntentType.CALL_CONTACT, "أمي");
        testIntent("اتصل بأبي", IntentType.CALL_CONTACT, "أبي");
        testIntent("اتصل بأحمد", IntentType.CALL_CONTACT, "أحمد");
        testIntent("اتصل بالدكتور محمد", IntentType.CALL_CONTACT, "الدكتور محمد");
    }

    @Test
    public void testCallContact_EgyptianColloquial() {
        // Egyptian colloquial variants
        testIntent("كلم ماما", IntentType.CALL_CONTACT, "ماما");
        testIntent("كلم بابا", IntentType.CALL_CONTACT, "بابا");
        testIntent("كلم أحمد", IntentType.CALL_CONTACT, "أحمد");
        testIntent("كلم الدكتور", IntentType.CALL_CONTACT, "الدكتور");
    }

    @Test
    public void testCallContact_RingVariants() {
        // Ring/call variants
        testIntent("رن على ماما", IntentType.CALL_CONTACT, "ماما");
        testIntent("رن على بابا", IntentType.CALL_CONTACT, "بابا");
        testIntent("رن على أحمد", IntentType.CALL_CONTACT, "أحمد");
    }

    @Test
    public void testCallContact_EgyptianExpressions() {
        // Egyptian-specific expressions
        testIntent("خده على تليفون ماما", IntentType.CALL_CONTACT, "ماما");
        testIntent("حطني في مكالمة مع بابا", IntentType.CALL_CONTACT, "بابا");
        testIntent("عايز أتتكلم مع أحمد", IntentType.CALL_CONTACT, "أحمد");
        testIntent("ممكن تكلم ماما", IntentType.CALL_CONTACT, "ماما");
    }

    @Test
    public void testCallContact_WithTimeModifiers() {
        // Call with time modifiers
        testIntent("كلم ماما دلوقتي", IntentType.CALL_CONTACT, "ماما");
        testIntent("اتصل ببابا حالا", IntentType.CALL_CONTACT, "بابا");
        testIntent("رن على أحمد بكرة", IntentType.CALL_CONTACT, "أحمد");
    }

    // ============================================================
    // CATEGORY 2: SEND_WHATSAPP (15 tests)
    // ============================================================
    
    @Test
    public void testSendWhatsApp_Standard() {
        // Standard WhatsApp commands
        testIntent("ابعت واتساب لأمي", IntentType.SEND_WHATSAPP, "أمي");
        testIntent("ابعت واتساب لأحمد", IntentType.SEND_WHATSAPP, "أحمد");
        testIntent("ارسل واتساب لماما", IntentType.SEND_WHATSAPP, "ماما");
    }

    @Test
    public void testSendWhatsApp_MessageVariants() {
        // Message sending variants
        testIntent("ابعت رسالة واتساب", IntentType.SEND_WHATSAPP, null);
        testIntent("ارسل رسالة على واتساب", IntentType.SEND_WHATSAPP, null);
        testIntent("بعتلها واتساب", IntentType.SEND_WHATSAPP, null);
    }

    @Test
    public void testSendWhatsApp_EgyptianExpressions() {
        // Egyptian expressions
        testIntent("قول لأحمد إني جاى", IntentType.SEND_WHATSAPP, "أحمد");
        testIntent("قول لماما إنني هتأخر", IntentType.SEND_WHATSAPP, "ماما");
        testIntent("راسل أحمد على واتساب", IntentType.SEND_WHATSAPP, "أحمد");
        testIntent("اكتب لـ أحمد في الواتساب", IntentType.SEND_WHATSAPP, "أحمد");
    }

    @Test
    public void testSendWhatsApp_WithContent() {
        // WhatsApp with message content
        testIntent("ابعت واتساب لأمي إنني جاى", IntentType.SEND_WHATSAPP, "أمي");
        testIntent("قول لبابا السلام عليكم", IntentType.SEND_WHATSAPP, "بابا");
    }

    @Test
    public void testSendWhatsApp_MixedLanguage() {
        // Mixed Arabic-English
        testIntent("ابعت WhatsApp لـ Ahmed", IntentType.SEND_WHATSAPP, "Ahmed");
        testIntent("Send message لـ ماما", IntentType.SEND_WHATSAPP, "ماما");
    }

    // ============================================================
    // CATEGORY 3: SET_ALARM (15 tests)
    // ============================================================
    
    @Test
    public void testSetAlarm_Standard() {
        // Standard alarm commands
        testIntent("نبهني بكرة الصبح", IntentType.SET_ALARM, "بكرة الصبح");
        testIntent("نبهني بعد ساعة", IntentType.SET_ALARM, "بعد ساعة");
        testIntent("ذكرني بكرة", IntentType.SET_ALARM, "بكرة");
    }

    @Test
    public void testSetAlarm_EgyptianVariants() {
        // Egyptian alarm variants
        testIntent("انبهني بكرة بدري", IntentType.SET_ALARM, "بكرة بدري");
        testIntent("انبهني الصبح بدري", IntentType.SET_ALARM, "الصبح بدري");
        testIntent("ذكرني بعد شوية", IntentType.SET_ALARM, "بعد شوية");
    }

    @Test
    public void testSetAlarm_SpecificTimes() {
        // Specific time alarms
        testIntent("اضبط المنبه على 7 الصبح", IntentType.SET_ALARM, "7 الصبح");
        testIntent("اضبطلي منبه الساعة 8", IntentType.SET_ALARM, "الساعة 8");
        testIntent("حطلي منبه على 3 العصر", IntentType.SET_ALARM, "3 العصر");
    }

    @Test
    public void testSetAlarm_TimeOfDay() {
        // Time of day alarms
        testIntent("نبهني بكرة الضهر", IntentType.SET_ALARM, "بكرة الضهر");
        testIntent("ذكرني بكرة المغرب", IntentType.SET_ALARM, "بكرة المغرب");
        testIntent("نبهني بكرة العشا", IntentType.SET_ALARM, "بكرة العشا");
        testIntent("ذكرني بكرة الليل", IntentType.SET_ALARM, "بكرة الليل");
    }

    @Test
    public void testSetAlarm_Reminders() {
        // Reminder variants
        testIntent("ذكرني آخد الدواء", IntentType.SET_ALARM, null);
        testIntent("نبهني بالاجتماع", IntentType.SET_ALARM, null);
        testIntent("حطلي تذكير", IntentType.SET_ALARM, null);
    }

    // ============================================================
    // CATEGORY 4: READ_TIME (10 tests)
    // ============================================================
    
    @Test
    public void testReadTime_Standard() {
        // Standard time queries
        testIntent("الساعة كام", IntentType.READ_TIME, null);
        testIntent("كام الساعة", IntentType.READ_TIME, null);
        testIntent("الوقت كام", IntentType.READ_TIME, null);
    }

    @Test
    public void testReadTime_EgyptianVariants() {
        // Egyptian time query variants
        testIntent("وقت إيه دلوقتي", IntentType.READ_TIME, null);
        testIntent("قولي الساعة", IntentType.READ_TIME, null);
        testIntent("إيه الوقت", IntentType.READ_TIME, null);
    }

    @Test
    public void testReadTime_MixedLanguage() {
        // Mixed language time queries
        testIntent("What time is it", IntentType.READ_TIME, null);
        testIntent("الساعة كم", IntentType.READ_TIME, null);
        testIntent("Time please", IntentType.READ_TIME, null);
    }

    @Test
    public void testReadTime_Polite() {
        // Polite time queries
        testIntent("ممكن تقوللي الساعة كام", IntentType.READ_TIME, null);
        testIntent("لو سمحت الوقت", IntentType.READ_TIME, null);
    }

    // ============================================================
    // CATEGORY 5: EMERGENCY (10 tests)
    // ============================================================
    
    @Test
    public void testEmergency_Standard() {
        // Standard emergency commands
        testIntent("نجدة", IntentType.EMERGENCY, null);
        testIntent("استغاثة", IntentType.EMERGENCY, null);
        testIntent("طوارئ", IntentType.EMERGENCY, null);
    }

    @Test
    public void testEmergency_EgyptianExpressions() {
        // Egyptian emergency expressions
        testIntent("يا نجدة", IntentType.EMERGENCY, null);
        testIntent("في حد يجي", IntentType.EMERGENCY, null);
        testIntent("مش قادر", IntentType.EMERGENCY, null);
        testIntent("محتاج مساعدة", IntentType.EMERGENCY, null);
    }

    @Test
    public void testEmergency_UrgentHelp() {
        // Urgent help requests
        testIntent("ساعدني بسرعة", IntentType.EMERGENCY, null);
        testIntent("انقذني", IntentType.EMERGENCY, null);
        testIntent("أنا في مشكلة", IntentType.EMERGENCY, null);
        testIntent("حاجة طارئة", IntentType.EMERGENCY, null);
    }

    @Test
    public void testEmergency_MixedLanguage() {
        // Mixed language emergency
        testIntent("Help me", IntentType.EMERGENCY, null);
        testIntent("Emergency", IntentType.EMERGENCY, null);
    }

    // ============================================================
    // CATEGORY 6: GREETING (10 tests)
    // ============================================================
    
    @Test
    public void testGreeting_Standard() {
        // Standard greetings
        testIntent("السلام عليكم", IntentType.GREETING, null);
        testIntent("أهلاً", IntentType.GREETING, null);
        testIntent("مرحبا", IntentType.GREETING, null);
    }

    @Test
    public void testGreeting_EgyptianColloquial() {
        // Egyptian colloquial greetings
        testIntent("ازيك", IntentType.GREETING, null);
        testIntent("عامل ايه", IntentType.GREETING, null);
        testIntent("أهلاً وسهلاً", IntentType.GREETING, null);
        testIntent("يا هلا", IntentType.GREETING, null);
    }

    @Test
    public void testGreeting_TimeBased() {
        // Time-based greetings
        testIntent("صباح الخير", IntentType.GREETING, null);
        testIntent("مساء الخير", IntentType.GREETING, null);
        testIntent("مساء النور", IntentType.GREETING, null);
    }

    @Test
    public void testGreeting_PhoneGreetings() {
        // Phone-specific greetings
        testIntent("ألو", IntentType.GREETING, null);
        testIntent("ألو السلام عليكم", IntentType.GREETING, null);
    }

    // ============================================================
    // CATEGORY 7: THANK_YOU (5 tests)
    // ============================================================
    
    @Test
    public void testThankYou_Standard() {
        // Standard thanks
        testIntent("شكراً", IntentType.THANK_YOU, null);
        testIntent("شكرا", IntentType.THANK_YOU, null);
    }

    @Test
    public void testThankYou_EgyptianVariants() {
        // Egyptian thank you variants
        testIntent("متشكر", IntentType.THANK_YOU, null);
        testIntent("تسلم", IntentType.THANK_YOU, null);
        testIntent("تسلم إيديك", IntentType.THANK_YOU, null);
        testIntent("شكراً يا باشا", IntentType.THANK_YOU, null);
    }

    // ============================================================
    // CATEGORY 8: GOODBYE (5 tests)
    // ============================================================
    
    @Test
    public void testGoodbye_Standard() {
        // Standard goodbyes
        testIntent("مع السلامة", IntentType.GOODBYE, null);
        testIntent("سلام", IntentType.GOODBYE, null);
    }

    @Test
    public void testGoodbye_EgyptianVariants() {
        // Egyptian goodbye variants
        testIntent("باي", IntentType.GOODBYE, null);
        testIntent("بايباي", IntentType.GOODBYE, null);
        testIntent("في أمان الله", IntentType.GOODBYE, null);
        testIntent("أشوفك بعدين", IntentType.GOODBYE, null);
    }

    // ============================================================
    // CATEGORY 9: TOGGLE_WIFI (5 tests)
    // ============================================================
    
    @Test
    public void testToggleWiFi_On() {
        // WiFi on commands
        testIntent("شغل الواي فاي", IntentType.TOGGLE_WIFI, null);
        testIntent("افتح الواي فاي", IntentType.TOGGLE_WIFI, null);
        testIntent("شغل wifi", IntentType.TOGGLE_WIFI, null);
    }

    @Test
    public void testToggleWiFi_Off() {
        // WiFi off commands
        testIntent("اقفل الواي فاي", IntentType.TOGGLE_WIFI, null);
        testIntent("اطفئ الواي فاي", IntentType.TOGGLE_WIFI, null);
    }

    // ============================================================
    // CATEGORY 10: TOGGLE_BLUETOOTH (5 tests)
    // ============================================================
    
    @Test
    public void testToggleBluetooth_On() {
        // Bluetooth on commands
        testIntent("شغل البلوتوث", IntentType.TOGGLE_BLUETOOTH, null);
        testIntent("افتح البلوتوث", IntentType.TOGGLE_BLUETOOTH, null);
        testIntent("شغل bluetooth", IntentType.TOGGLE_BLUETOOTH, null);
    }

    @Test
    public void testToggleBluetooth_Off() {
        // Bluetooth off commands
        testIntent("اقفل البلوتوث", IntentType.TOGGLE_BLUETOOTH, null);
        testIntent("اطفئ البلوتوث", IntentType.TOGGLE_BLUETOOTH, null);
    }

    // ============================================================
    // CATEGORY 11: OPEN_APP (5 tests)
    // ============================================================
    
    @Test
    public void testOpenApp_Standard() {
        // Standard app open commands
        testIntent("افتح واتساب", IntentType.OPEN_APP, null);
        testIntent("افتح فيسبوك", IntentType.OPEN_APP, null);
        testIntent("شغل يوتيوب", IntentType.OPEN_APP, null);
    }

    @Test
    public void testOpenApp_MixedLanguage() {
        // Mixed language app commands
        testIntent("افتح WhatsApp", IntentType.OPEN_APP, null);
        testIntent("شغل YouTube", IntentType.OPEN_APP, null);
    }

    // ============================================================
    // CATEGORY 12: UNKNOWN/EDGE CASES (10 tests)
    // ============================================================
    
    @Test
    public void testUnknown_IncompleteCommands() {
        // Incomplete commands should be UNKNOWN
        testIntent("اتصل", IntentType.UNKNOWN, null);
        testIntent("ابعت", IntentType.UNKNOWN, null);
        testIntent("افتح", IntentType.UNKNOWN, null);
    }

    @Test
    public void testUnknown_AmbiguousPhrases() {
        // Ambiguous phrases
        testIntent("كلمني", IntentType.UNKNOWN, null);
        testIntent("أنا زعلان", IntentType.UNKNOWN, null);
        testIntent("إيه أخبارك", IntentType.UNKNOWN, null);
    }

    @Test
    public void testUnknown_NoiseAndTypos() {
        // Noisy input and typos
        testIntent("بلا بلا بلا", IntentType.UNKNOWN, null);
        testIntent("!!!", IntentType.UNKNOWN, null);
        testIntent("   ", IntentType.UNKNOWN, null);
    }

    @Test
    public void testUnknown_EmptyInput() {
        // Empty/null input
        testIntent("", IntentType.UNKNOWN, null);
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Test a single intent classification
     */
    private void testIntent(String input, IntentType expectedIntent, String expectedEntity) {
        totalTests++;
        
        IntentResult result = classifier.classify(input);
        boolean passed = result.getIntentType() == expectedIntent;
        
        if (expectedEntity != null) {
            String actualEntity = result.getEntity("contact");
            passed = passed && (actualEntity != null && actualEntity.contains(expectedEntity));
        }
        
        if (passed) {
            passedTests++;
            intentStats.put(expectedIntent, intentStats.get(expectedIntent) + 1);
        } else {
            failures.add(new TestFailure(input, expectedIntent, result.getIntentType(), 
                "Expected: " + expectedIntent + ", Got: " + result.getIntentType() + 
                " (confidence: " + result.getConfidence() + ")"));
        }
    }

    /**
     * Print test summary
     */
    @Test
    public void printSummary() {
        System.out.println("\n========================================");
        System.out.println("EGYPTIAN NLU COMPREHENSIVE TEST SUMMARY");
        System.out.println("========================================");
        System.out.println("Total Tests: " + totalTests);
        System.out.println("Passed: " + passedTests);
        System.out.println("Failed: " + (totalTests - passedTests));
        System.out.println("Accuracy: " + String.format("%.2f%%", 100.0f * passedTests / totalTests));
        System.out.println("\nResults by Intent:");
        
        for (Map.Entry<IntentType, Integer> entry : intentStats.entrySet()) {
            if (entry.getValue() > 0) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " tests");
            }
        }
        
        if (!failures.isEmpty()) {
            System.out.println("\nFailures:");
            for (TestFailure failure : failures) {
                System.out.println("  ✗ '" + failure.input + "' - Expected: " + 
                    failure.expected + ", Got: " + failure.actual);
            }
        }
        
        System.out.println("\n========================================");
        
        // Assert target accuracy
        float accuracy = 100.0f * passedTests / totalTests;
        assertTrue("Accuracy below target 97.8%: " + accuracy + "%", accuracy >= 97.8f);
    }

    /**
     * Get current accuracy
     */
    public float getAccuracy() {
        return totalTests > 0 ? 100.0f * passedTests / totalTests : 0;
    }
}
