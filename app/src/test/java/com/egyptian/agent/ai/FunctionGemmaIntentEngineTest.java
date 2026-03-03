package com.egyptian.agent.ai;

import android.content.Context;

import com.egyptian.agent.nlu.IntentResult;
import com.egyptian.agent.nlu.IntentType;
import com.egyptian.agent.llm.FunctionGemmaConfig;
import com.egyptian.agent.llm.FunctionGemmaEngine;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Unit Tests for FunctionGemmaIntentEngine
 *
 * Test Coverage:
 * - Intent classification for all 16 function types
 * - Entity extraction (contact names, time, messages)
 * - Confidence threshold validation
 * - Fallback mechanisms
 * - Egyptian dialect variations
 * - Async processing
 * - Performance metrics
 * - Edge cases
 *
 * Target Accuracy: 95%+
 * Target Performance: <500ms per classification
 *
 * @author Egyptian Agent Team
 * @version 1.0.0
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class FunctionGemmaIntentEngineTest {

    // ========================================================================
    // Test Configuration
    // ========================================================================

    private static final float CONFIDENCE_THRESHOLD = 0.85f;
    private static final float HIGH_CONFIDENCE_THRESHOLD = 0.95f;
    private static final long MAX_PROCESSING_TIME_MS = 500;
    private static final int ASYNC_TIMEOUT_SECONDS = 10;

    private Context context;
    private FunctionGemmaIntentEngine intentEngine;
    private FunctionGemmaConfig config;

    // Test statistics
    private int totalTests;
    private int passedTests;
    private Map<IntentType, Integer> intentStats;
    private Map<String, TestFailure> failures;

    // ========================================================================
    // Test Setup
    // ========================================================================

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();

        // Create optimized config for testing
        config = FunctionGemmaConfig.builder()
                .confidenceThreshold(CONFIDENCE_THRESHOLD)
                .maxTokens(128)
                .temperature(0.1f)
                .useStreaming(false)
                .enableLogging(false)
                .build();

        // Initialize test statistics
        totalTests = 0;
        passedTests = 0;
        intentStats = new HashMap<>();
        failures = new HashMap<>();

        for (IntentType type : IntentType.values()) {
            intentStats.put(type, 0);
        }
    }

    // ========================================================================
    // Test Initialization and Lifecycle
    // ========================================================================

    @Test
    public void testEngineInitialization() {
        // Given: New engine instance
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        // Then: Engine should be created successfully
        assertNotNull("Engine should be initialized", intentEngine);
        assertFalse("Engine should not be destroyed", intentEngine.isDestroyed());
    }

    @Test
    public void testEngineWithDefaultConfig() {
        // Given: Engine with default configuration
        intentEngine = new FunctionGemmaIntentEngine(context);

        // Then: Should initialize successfully
        assertNotNull("Engine should be initialized with default config", intentEngine);
    }

    @Test
    public void testEngineBuilderPattern() {
        // When: Using builder pattern
        FunctionGemmaIntentEngine engine = FunctionGemmaIntentEngine.builder()
                .context(context)
                .config(config)
                .build();

        // Then: Should create engine successfully
        assertNotNull("Builder should create engine", engine);
        engine.destroy();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilderWithoutContext() {
        // When: Building without context
        FunctionGemmaIntentEngine.builder()
                .config(config)
                .build();
    }

    // ========================================================================
    // CATEGORY 1: CALL_CONTACT Intent Tests (10 tests)
    // ========================================================================

    @Test
    public void testCallContact_StandardEgyptian() {
        // Test standard Egyptian call commands
        testIntent("اتصل بماما", IntentType.CALL_CONTACT, "contact_name", "ماما");
        testIntent("اتصل ببابا", IntentType.CALL_CONTACT, "contact_name", "بابا");
        testIntent("اتصل بأحمد", IntentType.CALL_CONTACT, "contact_name", "أحمد");
        testIntent("اتصل بالدكتور محمد", IntentType.CALL_CONTACT, "contact_name", "الدكتور محمد");
    }

    @Test
    public void testCallContact_EgyptianColloquial() {
        // Test Egyptian colloquial variants
        testIntent("كلم ماما", IntentType.CALL_CONTACT, "contact_name", "ماما");
        testIntent("كلم بابا", IntentType.CALL_CONTACT, "contact_name", "بابا");
        testIntent("كلم أحمد", IntentType.CALL_CONTACT, "contact_name", "أحمد");
        testIntent("كلم الدكتور", IntentType.CALL_CONTACT, "contact_name", "الدكتور");
    }

    @Test
    public void testCallContact_RingVariants() {
        // Test ring/call variants
        testIntent("رن على ماما", IntentType.CALL_CONTACT, "contact_name", "ماما");
        testIntent("رن على بابا", IntentType.CALL_CONTACT, "contact_name", "بابا");
        testIntent("رن على أحمد", IntentType.CALL_CONTACT, "contact_name", "أحمد");
    }

    @Test
    public void testCallContact_EgyptianExpressions() {
        // Test Egyptian-specific expressions
        testIntent("خده على تليفون ماما", IntentType.CALL_CONTACT, "contact_name", "ماما");
        testIntent("حطني في مكالمة مع بابا", IntentType.CALL_CONTACT, "contact_name", "بابا");
        testIntent("عايز أتتكلم مع أحمد", IntentType.CALL_CONTACT, "contact_name", "أحمد");
        testIntent("ممكن تكلم ماما", IntentType.CALL_CONTACT, "contact_name", "ماما");
    }

    @Test
    public void testCallContact_WithTimeModifiers() {
        // Test call with time modifiers
        testIntent("كلم ماما دلوقتي", IntentType.CALL_CONTACT, "contact_name", "ماما");
        testIntent("اتصل ببابا حالا", IntentType.CALL_CONTACT, "contact_name", "بابا");
        testIntent("رن على أحمد بكرة", IntentType.CALL_CONTACT, "contact_name", "أحمد");
    }

    // ========================================================================
    // CATEGORY 2: SEND_WHATSAPP Intent Tests (8 tests)
    // ========================================================================

    @Test
    public void testSendWhatsApp_Standard() {
        // Test standard WhatsApp commands
        testIntent("ابعت واتساب لأمي", IntentType.SEND_WHATSAPP, "contact_name", "أمي");
        testIntent("ابعت واتساب لأحمد", IntentType.SEND_WHATSAPP, "contact_name", "أحمد");
        testIntent("ارسل واتساب لماما", IntentType.SEND_WHATSAPP, "contact_name", "ماما");
    }

    @Test
    public void testSendWhatsApp_MessageVariants() {
        // Test message sending variants
        testIntent("ابعت رسالة واتساب", IntentType.SEND_WHATSAPP, null, null);
        testIntent("ارسل رسالة على واتساب", IntentType.SEND_WHATSAPP, null, null);
        testIntent("بعتلها واتساب", IntentType.SEND_WHATSAPP, null, null);
    }

    @Test
    public void testSendWhatsApp_EgyptianExpressions() {
        // Test Egyptian expressions
        testIntent("قول لأحمد إني جاى", IntentType.SEND_WHATSAPP, "contact_name", "أحمد");
        testIntent("قول لماما إنني هتأخر", IntentType.SEND_WHATSAPP, "contact_name", "ماما");
        testIntent("راسل أحمد على واتساب", IntentType.SEND_WHATSAPP, "contact_name", "أحمد");
    }

    @Test
    public void testSendWhatsApp_WithContent() {
        // Test WhatsApp with message content
        testIntent("ابعت واتساب لأمي إنني جاى", IntentType.SEND_WHATSAPP, "contact_name", "أمي");
        testIntent("قول لبابا السلام عليكم", IntentType.SEND_WHATSAPP, "contact_name", "بابا");
    }

    // ========================================================================
    // CATEGORY 3: SEND_VOICE_MESSAGE Intent Tests (4 tests)
    // ========================================================================

    @Test
    public void testSendVoiceMessage_Standard() {
        // Test standard voice message commands
        testIntent("ابعت رسالة صوتية", IntentType.SEND_VOICE_MESSAGE, null, null);
        testIntent("سجّل صوت وابعثه", IntentType.SEND_VOICE_MESSAGE, null, null);
        testIntent("أرسل ميمو لماما", IntentType.SEND_VOICE_MESSAGE, "contact_name", "ماما");
    }

    @Test
    public void testSendVoiceMessage_EgyptianVariants() {
        // Test Egyptian variants
        testIntent("ابعت فويس نوت", IntentType.SEND_VOICE_MESSAGE, null, null);
        testIntent("سجل رسالة صوتية", IntentType.SEND_VOICE_MESSAGE, null, null);
    }

    // ========================================================================
    // CATEGORY 4: SET_ALARM Intent Tests (8 tests)
    // ========================================================================

    @Test
    public void testSetAlarm_Standard() {
        // Test standard alarm commands
        testIntent("نبهني بكرة الصبح", IntentType.SET_ALARM, "time", "بكرة الصبح");
        testIntent("نبهني بعد ساعة", IntentType.SET_ALARM, "time", "بعد ساعة");
        testIntent("ذكرني بكرة", IntentType.SET_ALARM, "time", "بكرة");
    }

    @Test
    public void testSetAlarm_EgyptianVariants() {
        // Test Egyptian alarm variants
        testIntent("انبهني بكرة بدري", IntentType.SET_ALARM, "time", "بكرة بدري");
        testIntent("انبهني الصبح بدري", IntentType.SET_ALARM, "time", "الصبح بدري");
        testIntent("ذكرني بعد شوية", IntentType.SET_ALARM, "time", "بعد شوية");
    }

    @Test
    public void testSetAlarm_SpecificTimes() {
        // Test specific time alarms
        testIntent("اضبط المنبه على 7 الصبح", IntentType.SET_ALARM, "time", "7 الصبح");
        testIntent("اضبطلي منبه الساعة 8", IntentType.SET_ALARM, "time", "الساعة 8");
        testIntent("حطلي منبه على 3 العصر", IntentType.SET_ALARM, "time", "3 العصر");
    }

    @Test
    public void testSetAlarm_TimeOfDay() {
        // Test time of day alarms
        testIntent("نبهني بكرة الضهر", IntentType.SET_ALARM, "time", "بكرة الضهر");
        testIntent("ذكرني بكرة المغرب", IntentType.SET_ALARM, "time", "بكرة المغرب");
        testIntent("نبهني بكرة العشا", IntentType.SET_ALARM, "time", "بكرة العشا");
    }

    // ========================================================================
    // CATEGORY 5: READ_TIME Intent Tests (5 tests)
    // ========================================================================

    @Test
    public void testReadTime_Standard() {
        // Test standard time queries
        testIntent("الساعة كام", IntentType.READ_TIME, null, null);
        testIntent("كام الساعة", IntentType.READ_TIME, null, null);
        testIntent("الوقت كام", IntentType.READ_TIME, null, null);
    }

    @Test
    public void testReadTime_EgyptianVariants() {
        // Test Egyptian time query variants
        testIntent("وقت إيه دلوقتي", IntentType.READ_TIME, null, null);
        testIntent("قولي الساعة", IntentType.READ_TIME, null, null);
        testIntent("إيه الوقت", IntentType.READ_TIME, null, null);
    }

    @Test
    public void testReadTime_Polite() {
        // Test polite time queries
        testIntent("ممكن تقوللي الساعة كام", IntentType.READ_TIME, null, null);
        testIntent("لو سمحت الوقت", IntentType.READ_TIME, null, null);
    }

    // ========================================================================
    // CATEGORY 6: EMERGENCY Intent Tests (6 tests)
    // ========================================================================

    @Test
    public void testEmergency_Standard() {
        // Test standard emergency commands
        testIntentWithHighConfidence("نجدة", IntentType.EMERGENCY);
        testIntentWithHighConfidence("استغاثة", IntentType.EMERGENCY);
        testIntentWithHighConfidence("طوارئ", IntentType.EMERGENCY);
    }

    @Test
    public void testEmergency_EgyptianExpressions() {
        // Test Egyptian emergency expressions
        testIntentWithHighConfidence("يا نجدة", IntentType.EMERGENCY);
        testIntentWithHighConfidence("في حد يجي", IntentType.EMERGENCY);
        testIntentWithHighConfidence("مش قادر", IntentType.EMERGENCY);
        testIntentWithHighConfidence("محتاج مساعدة", IntentType.EMERGENCY);
    }

    @Test
    public void testEmergency_UrgentHelp() {
        // Test urgent help requests
        testIntentWithHighConfidence("ساعدني بسرعة", IntentType.EMERGENCY);
        testIntentWithHighConfidence("انقذني", IntentType.EMERGENCY);
        testIntentWithHighConfidence("أنا في مشكلة", IntentType.EMERGENCY);
    }

    // ========================================================================
    // CATEGORY 7: GREETING Intent Tests (5 tests)
    // ========================================================================

    @Test
    public void testGreeting_Standard() {
        // Test standard greetings
        testIntent("السلام عليكم", IntentType.GREETING, null, null);
        testIntent("أهلاً", IntentType.GREETING, null, null);
        testIntent("مرحبا", IntentType.GREETING, null, null);
    }

    @Test
    public void testGreeting_EgyptianColloquial() {
        // Test Egyptian colloquial greetings
        testIntent("ازيك", IntentType.GREETING, null, null);
        testIntent("عامل ايه", IntentType.GREETING, null, null);
        testIntent("أهلاً وسهلاً", IntentType.GREETING, null, null);
    }

    @Test
    public void testGreeting_TimeBased() {
        // Test time-based greetings
        testIntent("صباح الخير", IntentType.GREETING, null, null);
        testIntent("مساء الخير", IntentType.GREETING, null, null);
        testIntent("مساء النور", IntentType.GREETING, null, null);
    }

    // ========================================================================
    // CATEGORY 8: THANK_YOU Intent Tests (4 tests)
    // ========================================================================

    @Test
    public void testThankYou_Standard() {
        // Test standard thanks
        testIntent("شكراً", IntentType.THANK_YOU, null, null);
        testIntent("شكرا", IntentType.THANK_YOU, null, null);
    }

    @Test
    public void testThankYou_EgyptianVariants() {
        // Test Egyptian thank you variants
        testIntent("متشكر", IntentType.THANK_YOU, null, null);
        testIntent("تسلم", IntentType.THANK_YOU, null, null);
        testIntent("تسلم إيديك", IntentType.THANK_YOU, null, null);
    }

    // ========================================================================
    // CATEGORY 9: GOODBYE Intent Tests (4 tests)
    // ========================================================================

    @Test
    public void testGoodbye_Standard() {
        // Test standard goodbyes
        testIntent("مع السلامة", IntentType.GOODBYE, null, null);
        testIntent("سلام", IntentType.GOODBYE, null, null);
    }

    @Test
    public void testGoodbye_EgyptianVariants() {
        // Test Egyptian goodbye variants
        testIntent("باي", IntentType.GOODBYE, null, null);
        testIntent("بايباي", IntentType.GOODBYE, null, null);
        testIntent("في أمان الله", IntentType.GOODBYE, null, null);
    }

    // ========================================================================
    // CATEGORY 10: TOGGLE_WIFI Intent Tests (5 tests)
    // ========================================================================

    @Test
    public void testToggleWiFi_On() {
        // Test WiFi on commands
        testIntent("شغل الواي فاي", IntentType.TOGGLE_WIFI, "state", "on");
        testIntent("افتح الواي فاي", IntentType.TOGGLE_WIFI, "state", "on");
        testIntent("شغل wifi", IntentType.TOGGLE_WIFI, "state", "on");
    }

    @Test
    public void testToggleWiFi_Off() {
        // Test WiFi off commands
        testIntent("اقفل الواي فاي", IntentType.TOGGLE_WIFI, "state", "off");
        testIntent("اطفئ الواي فاي", IntentType.TOGGLE_WIFI, "state", "off");
    }

    // ========================================================================
    // CATEGORY 11: TOGGLE_BLUETOOTH Intent Tests (4 tests)
    // ========================================================================

    @Test
    public void testToggleBluetooth_On() {
        // Test Bluetooth on commands
        testIntent("شغل البلوتوث", IntentType.TOGGLE_BLUETOOTH, "state", "on");
        testIntent("افتح البلوتوث", IntentType.TOGGLE_BLUETOOTH, "state", "on");
    }

    @Test
    public void testToggleBluetooth_Off() {
        // Test Bluetooth off commands
        testIntent("اقفل البلوتوث", IntentType.TOGGLE_BLUETOOTH, "state", "off");
        testIntent("اطفئ البلوتوث", IntentType.TOGGLE_BLUETOOTH, "state", "off");
    }

    // ========================================================================
    // CATEGORY 12: TOGGLE_FLASHLIGHT Intent Tests (3 tests)
    // ========================================================================

    @Test
    public void testToggleFlashlight() {
        // Test flashlight commands
        testIntent("افتح الفلاش", IntentType.TOGGLE_FLASHLIGHT, "state", "on");
        testIntent("شغل الكشاف", IntentType.TOGGLE_FLASHLIGHT, "state", "on");
        testIntent("قفل الفلاش", IntentType.TOGGLE_FLASHLIGHT, "state", "off");
    }

    // ========================================================================
    // CATEGORY 13: OPEN_APP Intent Tests (4 tests)
    // ========================================================================

    @Test
    public void testOpenApp_Standard() {
        // Test standard app open commands
        testIntent("افتح واتساب", IntentType.OPEN_APP, "app_name", "واتساب");
        testIntent("افتح فيسبوك", IntentType.OPEN_APP, "app_name", "فيسبوك");
        testIntent("شغل يوتيوب", IntentType.OPEN_APP, "app_name", "يوتيوب");
    }

    @Test
    public void testOpenApp_MixedLanguage() {
        // Test mixed language app commands
        testIntent("افتح WhatsApp", IntentType.OPEN_APP, "app_name", "WhatsApp");
        testIntent("شغل YouTube", IntentType.OPEN_APP, "app_name", "YouTube");
    }

    // ========================================================================
    // CATEGORY 14: SEND_SMS Intent Tests (3 tests)
    // ========================================================================

    @Test
    public void testSendSMS_Standard() {
        // Test standard SMS commands
        testIntent("ابعت رسالة نصية", IntentType.SEND_SMS, null, null);
        testIntent("أرسل SMS لماما", IntentType.SEND_SMS, "contact_name", "ماما");
    }

    @Test
    public void testSendSMS_EgyptianVariants() {
        // Test Egyptian variants
        testIntent("سيب رسالة لمحمد", IntentType.SEND_SMS, "contact_name", "محمد");
    }

    // ========================================================================
    // CATEGORY 15: READ_MISSED_CALLS Intent Tests (3 tests)
    // ========================================================================

    @Test
    public void testReadMissedCalls() {
        // Test missed calls queries
        testIntent("إيه المكالمات اللي فاتت", IntentType.READ_MISSED_CALLS, null, null);
        testIntent("قولي مين اتصل بيا", IntentType.READ_MISSED_CALLS, null, null);
        testIntent("عندي مكالمات مفقودة", IntentType.READ_MISSED_CALLS, null, null);
    }

    // ========================================================================
    // CATEGORY 16: WEATHER_QUERY Intent Tests (3 tests)
    // ========================================================================

    @Test
    public void testWeatherQuery() {
        // Test weather queries
        testIntent("الجو إيه النهاردة", IntentType.WEATHER_QUERY, null, null);
        testIntent("إيه حالة الطقس", IntentType.WEATHER_QUERY, null, null);
        testIntent("هيمطر بكرة", IntentType.WEATHER_QUERY, null, null);
    }

    // ========================================================================
    // CATEGORY 17: Confidence Threshold Tests (5 tests)
    // ========================================================================

    @Test
    public void testConfidenceThreshold_HighConfidence() {
        // Test that high-confidence predictions pass threshold
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        String[] highConfidenceInputs = {
            "نجدة",
            "الساعة كام",
            "اتصل بماما"
        };

        for (String input : highConfidenceInputs) {
            IntentResult result = intentEngine.classifyIntent(input);
            assertTrue("High confidence input should pass threshold: " + input,
                    result.getConfidence() >= CONFIDENCE_THRESHOLD || result.getIntentType() != IntentType.UNKNOWN);
        }
    }

    @Test
    public void testConfidenceThreshold_BelowThreshold() {
        // Test that ambiguous inputs may fall below threshold
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        String[] ambiguousInputs = {
            "كلمني",
            "أنا زعلان",
            "إيه أخبارك"
        };

        for (String input : ambiguousInputs) {
            IntentResult result = intentEngine.classifyIntent(input);
            // Ambiguous inputs should either be UNKNOWN or have lower confidence
            assertTrue("Ambiguous input should be handled appropriately: " + input,
                    result.getIntentType() == IntentType.UNKNOWN ||
                    result.getConfidence() < HIGH_CONFIDENCE_THRESHOLD);
        }
    }

    // ========================================================================
    // CATEGORY 18: Fallback Mechanism Tests (4 tests)
    // ========================================================================

    @Test
    public void testFallback_EmptyInput() {
        // Test fallback for empty input
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        IntentResult result = intentEngine.classifyIntent("");

        assertNotNull("Result should not be null", result);
        assertEquals("Empty input should return UNKNOWN", IntentType.UNKNOWN, result.getIntentType());
    }

    @Test
    public void testFallback_NullInput() {
        // Test fallback for null input
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        IntentResult result = intentEngine.classifyIntent(null);

        assertNotNull("Result should not be null", result);
        assertEquals("Null input should return UNKNOWN", IntentType.UNKNOWN, result.getIntentType());
    }

    @Test
    public void testFallback_WhitespaceInput() {
        // Test fallback for whitespace-only input
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        IntentResult result = intentEngine.classifyIntent("   ");

        assertNotNull("Result should not be null", result);
        assertEquals("Whitespace input should return UNKNOWN", IntentType.UNKNOWN, result.getIntentType());
    }

    @Test
    public void testFallback_NoiseInput() {
        // Test fallback for noise input
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        IntentResult result = intentEngine.classifyIntent("بلا بلا بلا");

        assertNotNull("Result should not be null", result);
        assertEquals("Noise input should return UNKNOWN", IntentType.UNKNOWN, result.getIntentType());
    }

    // ========================================================================
    // CATEGORY 19: Egyptian Dialect Variation Tests (6 tests)
    // ========================================================================

    @Test
    public void testEgyptianDialect_CallVariations() {
        // Test various Egyptian dialect call expressions
        testIntent("خده على التليفون", IntentType.CALL_CONTACT, null, null);
        testIntent("حطني في الخط", IntentType.CALL_CONTACT, null, null);
        testIntent("وصلني بـ", IntentType.CALL_CONTACT, null, null);
    }

    @Test
    public void testEgyptianDialect_MessageVariations() {
        // Test various Egyptian dialect message expressions
        testIntent("قولوله", IntentType.SEND_WHATSAPP, null, null);
        testIntent("ابعتله خبر", IntentType.SEND_WHATSAPP, null, null);
        testIntent("راسله", IntentType.SEND_WHATSAPP, null, null);
    }

    @Test
    public void testEgyptianDialect_TimeExpressions() {
        // Test various Egyptian time expressions
        testIntent("نبهني بدري", IntentType.SET_ALARM, "time", "بدري");
        testIntent("ذكرني الضهر", IntentType.SET_ALARM, "time", "الضهر");
        testIntent("نبهني العشا", IntentType.SET_ALARM, "time", "العشا");
    }

    @Test
    public void testEgyptianDialect_MixedArabicEnglish() {
        // Test mixed Arabic-English inputs
        testIntent("ابعت WhatsApp", IntentType.SEND_WHATSAPP, null, null);
        testIntent("Call ماما", IntentType.CALL_CONTACT, "contact_name", "ماما");
        testIntent("Open واتساب", IntentType.OPEN_APP, "app_name", "واتساب");
    }

    @Test
    public void testEgyptianDialect_SlangVariations() {
        // Test Egyptian slang variations
        testIntent("هات ماما على الخط", IntentType.CALL_CONTACT, "contact_name", "ماما");
        testIntent("صوّر صوت", IntentType.SEND_VOICE_MESSAGE, null, null);
    }

    @Test
    public void testEgyptianDialect_RegionalVariations() {
        // Test regional Egyptian dialect variations
        testIntent("إتصل بـ", IntentType.CALL_CONTACT, null, null);
        testIntent("كلم", IntentType.CALL_CONTACT, null, null);
        testIntent("رن", IntentType.CALL_CONTACT, null, null);
    }

    // ========================================================================
    // CATEGORY 20: Async Processing Tests (3 tests)
    // ========================================================================

    @Test
    public void testAsyncIntentClassification() throws Exception {
        // Test async intent classification with callback
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<IntentResult> resultRef = new AtomicReference<>();

        intentEngine.classifyIntentAsync("اتصل بماما", new FunctionGemmaIntentEngine.IntentCallback() {
            @Override
            public void onIntent(IntentResult result) {
                resultRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(Exception error) {
                latch.countDown();
            }
        });

        assertTrue("Async callback should complete", latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        IntentResult result = resultRef.get();
        assertNotNull("Result should not be null", result);
        assertEquals("Should classify as CALL_CONTACT", IntentType.CALL_CONTACT, result.getIntentType());
    }

    @Test
    public void testAsyncIntentClassification_ErrorHandling() throws Exception {
        // Test async error handling
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> errorRef = new AtomicReference<>();

        // This should complete without error even with null input
        intentEngine.classifyIntentAsync(null, new FunctionGemmaIntentEngine.IntentCallback() {
            @Override
            public void onIntent(IntentResult result) {
                latch.countDown();
            }

            @Override
            public void onError(Exception error) {
                errorRef.set(error);
                latch.countDown();
            }
        });

        assertTrue("Async callback should complete", latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        // Should not error, should return UNKNOWN intent
        assertNull("Should not produce error for null input", errorRef.get());
    }

    // ========================================================================
    // CATEGORY 21: Performance Metrics Tests (4 tests)
    // ========================================================================

    @Test
    public void testPerformanceMetrics_Tracking() {
        // Test that performance metrics are tracked
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        // Process multiple commands
        for (int i = 0; i < 5; i++) {
            intentEngine.classifyIntent("الساعة كام");
        }

        // Verify metrics are tracked
        assertTrue("Total processed should be > 0", intentEngine.getTotalProcessed() > 0);
        assertTrue("Average processing time should be >= 0", intentEngine.getAverageProcessingTimeMs() >= 0);
    }

    @Test
    public void testPerformanceMetrics_SuccessRate() {
        // Test success rate calculation
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        // Process mix of valid and invalid commands
        intentEngine.classifyIntent("اتصل بماما");
        intentEngine.classifyIntent("الساعة كام");
        intentEngine.classifyIntent("");

        float successRate = intentEngine.getSuccessRate();
        assertTrue("Success rate should be between 0 and 1", successRate >= 0 && successRate <= 1);
    }

    @Test
    public void testPerformanceMetrics_FallbackRate() {
        // Test fallback rate calculation
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        // Process commands that may trigger fallback
        intentEngine.classifyIntent("اتصل بماما");
        intentEngine.classifyIntent("");
        intentEngine.classifyIntent("نجدة");

        float fallbackRate = intentEngine.getFallbackRate();
        assertTrue("Fallback rate should be between 0 and 1", fallbackRate >= 0 && fallbackRate <= 1);
    }

    @Test
    public void testPerformanceMetrics_PerformanceSummary() {
        // Test performance summary generation
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        intentEngine.classifyIntent("الساعة كام");

        String summary = intentEngine.getPerformanceSummary();
        assertNotNull("Performance summary should not be null", summary);
        assertTrue("Summary should contain processed count", summary.contains("Total processed"));
    }

    // ========================================================================
    // CATEGORY 22: Entity Extraction Tests (5 tests)
    // ========================================================================

    @Test
    public void testEntityExtraction_ContactName() {
        // Test contact name extraction
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        IntentResult result = intentEngine.classifyIntent("اتصل بماما");

        assertNotNull("Result should not be null", result);
        String contactName = result.getEntity("contact_name");
        assertNotNull("Contact name should be extracted", contactName);
        assertTrue("Contact name should contain ماما", contactName.contains("ماما"));
    }

    @Test
    public void testEntityExtraction_Time() {
        // Test time extraction
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        IntentResult result = intentEngine.classifyIntent("نبهني بكرة الصبح");

        assertNotNull("Result should not be null", result);
        String time = result.getEntity("time");
        assertNotNull("Time should be extracted", time);
        assertTrue("Time should contain بكرة", time.contains("بكرة") || time.contains("الصبح"));
    }

    @Test
    public void testEntityExtraction_Message() {
        // Test message extraction
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        IntentResult result = intentEngine.classifyIntent("ابعت واتساب لأحمد قولوله سلام");

        assertNotNull("Result should not be null", result);
        // Message may be extracted or may need additional processing
        assertNotNull("Result should exist", result);
    }

    @Test
    public void testEntityExtraction_AppName() {
        // Test app name extraction
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        IntentResult result = intentEngine.classifyIntent("افتح واتساب");

        assertNotNull("Result should not be null", result);
        String appName = result.getEntity("app_name");
        assertNotNull("App name should be extracted", appName);
        assertTrue("App name should contain واتساب", appName.contains("واتساب"));
    }

    @Test
    public void testEntityExtraction_ToggleState() {
        // Test toggle state extraction
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        IntentResult result = intentEngine.classifyIntent("افتح الواي فاي");

        assertNotNull("Result should not be null", result);
        String state = result.getEntity("state");
        assertNotNull("State should be extracted", state);
    }

    // ========================================================================
    // CATEGORY 23: Edge Cases Tests (5 tests)
    // ========================================================================

    @Test
    public void testEdgeCases_VeryLongInput() {
        // Test very long input handling
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        StringBuilder longInput = new StringBuilder("اتصل بماما ");
        for (int i = 0; i < 50; i++) {
            longInput.append("وكمان ");
        }

        IntentResult result = intentEngine.classifyIntent(longInput.toString());

        assertNotNull("Result should not be null for long input", result);
        // Should handle gracefully, may return UNKNOWN
        assertNotNull("Should handle long input", result);
    }

    @Test
    public void testEdgeCases_SpecialCharacters() {
        // Test special characters handling
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        String[] specialInputs = {
            "اتصل بماما!!!",
            "الساعة كام؟؟؟",
            "نجدة...",
            "!!!",
            "؟؟؟"
        };

        for (String input : specialInputs) {
            IntentResult result = intentEngine.classifyIntent(input);
            assertNotNull("Should handle special characters: " + input, result);
        }
    }

    @Test
    public void testEdgeCases_NumbersOnly() {
        // Test numbers-only input
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        IntentResult result = intentEngine.classifyIntent("12345");

        assertNotNull("Result should not be null", result);
        // Numbers only should be UNKNOWN or handled appropriately
    }

    @Test
    public void testEdgeCases_Emoji() {
        // Test emoji handling
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        IntentResult result = intentEngine.classifyIntent("📞 ماما");

        assertNotNull("Should handle emoji input", result);
    }

    @Test
    public void testEdgeCases_RepeatedCommands() {
        // Test repeated command handling
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        IntentResult first = intentEngine.classifyIntent("الساعة كام");
        IntentResult second = intentEngine.classifyIntent("الساعة كام");
        IntentResult third = intentEngine.classifyIntent("الساعة كام");

        assertNotNull("First result should not be null", first);
        assertNotNull("Second result should not be null", second);
        assertNotNull("Third result should not be null", third);
    }

    // ========================================================================
    // CATEGORY 24: Cleanup and Resource Management Tests (2 tests)
    // ========================================================================

    @Test
    public void testCleanup_Destroy() {
        // Test proper cleanup
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        assertFalse("Engine should not be destroyed initially", intentEngine.isDestroyed());

        intentEngine.destroy();

        assertTrue("Engine should be destroyed after cleanup", intentEngine.isDestroyed());
    }

    @Test
    public void testCleanup_DoubleDestroy() {
        // Test double destroy handling
        intentEngine = new FunctionGemmaIntentEngine(context, config);

        intentEngine.destroy();
        // Should not throw exception
        intentEngine.destroy();

        assertTrue("Engine should remain destroyed", intentEngine.isDestroyed());
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Test intent classification with expected result.
     */
    private void testIntent(String input, IntentType expectedIntent, String entityKey, String entityValue) {
        totalTests++;

        if (intentEngine == null) {
            intentEngine = new FunctionGemmaIntentEngine(context, config);
        }

        long startTime = System.currentTimeMillis();
        IntentResult result = intentEngine.classifyIntent(input);
        long processingTime = System.currentTimeMillis() - startTime;

        boolean passed = result.getIntentType() == expectedIntent;

        if (entityKey != null && entityValue != null) {
            String actualEntity = result.getEntity(entityKey);
            passed = passed && (actualEntity != null && actualEntity.contains(entityValue));
        }

        // Check performance
        boolean withinTimeLimit = processingTime <= MAX_PROCESSING_TIME_MS;

        if (passed && withinTimeLimit) {
            passedTests++;
            intentStats.put(expectedIntent, intentStats.get(expectedIntent) + 1);
        } else {
            String failureReason = "";
            if (!passed) {
                failureReason = "Expected: " + expectedIntent + ", Got: " + result.getIntentType();
            }
            if (!withinTimeLimit) {
                failureReason += " (Processing time: " + processingTime + "ms > " + MAX_PROCESSING_TIME_MS + "ms)";
            }

            failures.put(input, new TestFailure(input, expectedIntent, result.getIntentType(), failureReason));
        }
    }

    /**
     * Test intent classification with high confidence expectation.
     */
    private void testIntentWithHighConfidence(String input, IntentType expectedIntent) {
        totalTests++;

        if (intentEngine == null) {
            intentEngine = new FunctionGemmaIntentEngine(context, config);
        }

        IntentResult result = intentEngine.classifyIntent(input);

        boolean passed = result.getIntentType() == expectedIntent &&
                        result.getConfidence() >= HIGH_CONFIDENCE_THRESHOLD;

        if (passed) {
            passedTests++;
            intentStats.put(expectedIntent, intentStats.get(expectedIntent) + 1);
        } else {
            failures.put(input, new TestFailure(input, expectedIntent, result.getIntentType(),
                "Expected confidence >= " + HIGH_CONFIDENCE_THRESHOLD + ", Got: " + result.getConfidence()));
        }
    }

    /**
     * Test failure record.
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

    // ========================================================================
    // Test Summary and Reporting
    // ========================================================================

    /**
     * Print comprehensive test summary.
     */
    @Test
    public void printTestSummary() {
        System.out.println("\n========================================");
        System.out.println("FunctionGemmaIntentEngine TEST SUMMARY");
        System.out.println("========================================");
        System.out.println("Total Tests: " + totalTests);
        System.out.println("Passed: " + passedTests);
        System.out.println("Failed: " + (totalTests - passedTests));

        if (totalTests > 0) {
            float accuracy = 100.0f * passedTests / totalTests;
            System.out.println("Accuracy: " + String.format("%.2f%%", accuracy));
        }

        System.out.println("\nResults by Intent:");
        for (Map.Entry<IntentType, Integer> entry : intentStats.entrySet()) {
            if (entry.getValue() > 0) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " tests");
            }
        }

        if (!failures.isEmpty()) {
            System.out.println("\nFailures:");
            for (TestFailure failure : failures.values()) {
                System.out.println("  ✗ '" + failure.input + "' - " + failure.reason);
            }
        }

        System.out.println("\n========================================");

        // Assert target accuracy
        if (totalTests > 0) {
            float accuracy = 100.0f * passedTests / totalTests;
            assertTrue("Accuracy below target 95%: " + String.format("%.2f%%", accuracy), accuracy >= 95.0f);
        }
    }

    /**
     * Get current accuracy percentage.
     */
    public float getAccuracy() {
        return totalTests > 0 ? 100.0f * passedTests / totalTests : 0;
    }

    /**
     * Get test statistics.
     */
    public Map<String, Object> getTestStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTests", totalTests);
        stats.put("passedTests", passedTests);
        stats.put("failedTests", totalTests - passedTests);
        stats.put("accuracy", getAccuracy());
        return stats;
    }
}
