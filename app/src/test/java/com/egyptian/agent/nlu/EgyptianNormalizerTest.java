package com.egyptian.agent.nlu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EgyptianNormalizer Comprehensive Test Suite
 * 
 * Tests for Egyptian dialect normalization to Modern Standard Arabic (MSA).
 * Contains 100+ test cases covering all normalization scenarios.
 * 
 * Coverage Target: 90%
 */
@DisplayName("EgyptianNormalizer Tests")
class EgyptianNormalizerTest {

    @Nested
    @DisplayName("Basic Normalization Tests")
    class BasicNormalizationTests {

        @Test
        @DisplayName("Null input returns empty string")
        void testNormalizeNullInput() {
            String result = EgyptianNormalizer.normalize(null);
            assertEquals("", result);
        }

        @Test
        @DisplayName("Empty input returns empty string")
        void testNormalizeEmptyInput() {
            String result = EgyptianNormalizer.normalize("");
            assertEquals("", result);
        }

        @Test
        @DisplayName("Whitespace input returns empty string")
        void testNormalizeWhitespaceInput() {
            String result = EgyptianNormalizer.normalize("   ");
            assertEquals("", result);
        }

        @Test
        @DisplayName("MSA text remains unchanged")
        void testNormalizeMSAUnchanged() {
            String result = EgyptianNormalizer.normalize("اتصل بأمي");
            assertEquals("اتصل بأمي", result);
        }
    }

    @Nested
    @DisplayName("Egyptian Verb Normalization Tests")
    class EgyptianVerbNormalizationTests {

        @ParameterizedTest
        @CsvSource({
            "كلم, اتصل",
            "رن على, اتصل بـ",
            "نادي, اتصل بـ",
            "ابعت, أرسل",
            "قول, قل",
            "هات, أحضر",
            "اعمل, افعل",
            "افتح, افتح",
            "اقفل, أغلق",
            "شغل, شغّل"
        })
        @DisplayName("Egyptian verbs normalize to MSA")
        void testEgyptianVerbsNormalize(String egyptian, String expected) {
            String result = EgyptianNormalizer.normalize(egyptian);
            assertEquals(expected, result);
        }

        @ParameterizedTest
        @CsvSource({
            "خده, خذه",
            "جيب, أحضر",
            "روح, اذهب",
            "استنى, انتظر",
            "فهم, افهم",
            "عرف, اعرف",
            "لاقى, وجد",
            "مشى, ذهب",
            "رجع, عاد",
            "سيب, اترك",
            "خد, خذ",
            "حط, ضع",
            "حطني, ضعني",
            "حطلي, ضع لي"
        })
        @DisplayName("Additional Egyptian verbs normalize correctly")
        void testAdditionalEgyptianVerbs(String egyptian, String expected) {
            String result = EgyptianNormalizer.normalize(egyptian);
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("Common Expressions Normalization Tests")
    class CommonExpressionsNormalizationTests {

        @ParameterizedTest
        @CsvSource({
            "ازيك, كيف حالك",
            "عامل ايه, كيف حالك",
            "عامل إيه, كيف حالك",
            "تمام, جيد",
            "مش قادر, لا أستطيع",
            "مفيش, لا يوجد",
            "فيه, يوجد",
            "دلوقتي, الآن",
            "بعدين, لاحقاً",
            "بكرة, غداً",
            "النهاردة, اليوم"
        })
        @DisplayName("Common Egyptian expressions normalize correctly")
        void testCommonExpressions(String egyptian, String expected) {
            String result = EgyptianNormalizer.normalize(egyptian);
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("Negation Normalization Tests")
    class NegationNormalizationTests {

        @ParameterizedTest
        @CsvSource({
            "مش, ليس",
            "مقدرش, لا أستطيع",
            "معاياش, ليس معي",
            "معرش, لا أعرف",
            "مفيش حاجة, لا شيء"
        })
        @DisplayName("Egyptian negation forms normalize correctly")
        void testNegationForms(String egyptian, String expected) {
            String result = EgyptianNormalizer.normalize(egyptian);
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("Time Expressions Normalization Tests")
    class TimeExpressionsNormalizationTests {

        @ParameterizedTest
        @CsvSource({
            "الصبح, الصباح",
            "الظهر, الظهر",
            "العصر, العصر",
            "المغرب, المغرب",
            "العشا, العشاء",
            "الليل, الليل",
            "كام ساعة, بعد كم ساعة",
            "بعد شوية, بعد قليل",
            "بدري, مبكراً",
            "متأخر, متأخراً",
            "حالاً, فوراً",
            "بعد نص ساعة, بعد 30 دقيقة",
            "بعد ساعة, بعد ساعة",
            "دلوقتي حالا, الآن فوراً"
        })
        @DisplayName("Time expressions normalize correctly")
        void testTimeExpressions(String egyptian, String expected) {
            String result = EgyptianNormalizer.normalize(egyptian);
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("Emergency Expressions Normalization Tests")
    class EmergencyExpressionsNormalizationTests {

        @ParameterizedTest
        @CsvSource({
            "يا نجدة, نجدة",
            "استغاثة, استغاثة",
            "حاجة طارئة, حالة طارئة",
            "مش قادر, أحتاج مساعدة",
            "محتاج مساعدة, أحتاج مساعدة",
            "ساعدني, ساعدني",
            "انقذني, أنقذني",
            "في حد يجي, أحتاج مساعدة"
        })
        @DisplayName("Emergency expressions normalize correctly")
        void testEmergencyExpressions(String egyptian, String expected) {
            String result = EgyptianNormalizer.normalize(egyptian);
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("Question Words Normalization Tests")
    class QuestionWordsNormalizationTests {

        @ParameterizedTest
        @CsvSource({
            "إيه, ماذا",
            "إزاي, كيف",
            "إمتى, متى",
            "فين, أين",
            "ليه, لماذا",
            "كام, كم",
            "مين, من"
        })
        @DisplayName("Question words normalize correctly")
        void testQuestionWords(String egyptian, String expected) {
            String result = EgyptianNormalizer.normalize(egyptian);
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("Affirmations and Negations Tests")
    class AffirmationsNegationsTests {

        @ParameterizedTest
        @CsvSource({
            "أه, نعم",
            "أيوة, نعم",
            "لا, لا",
            "معلش, عفواً",
            "خلاص, حسناً",
            "زي الفل, ممتاز",
            "بلاش, لا شكراً"
        })
        @DisplayName("Affirmations and negations normalize correctly")
        void testAffirmationsNegations(String egyptian, String expected) {
            String result = EgyptianNormalizer.normalize(egyptian);
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("Contact Name Normalization Tests")
    class ContactNameNormalizationTests {

        @Test
        @DisplayName("Null contact name returns empty string")
        void testNormalizeContactNameNull() {
            String result = EgyptianNormalizer.normalizeContactName(null);
            assertEquals("", result);
        }

        @Test
        @DisplayName("Empty contact name returns empty string")
        void testNormalizeContactNameEmpty() {
            String result = EgyptianNormalizer.normalizeContactName("");
            assertEquals("", result);
        }

        @ParameterizedTest
        @CsvSource({
            "ماما, أمي",
            "بابا, أبي",
            "يما, أمي",
            "يبا, أبي",
            "ست الحبايب, أمي",
            "الحاج, الأب",
            "عمو, عمي",
            "خالو, خالي",
            "تيتا, جدتي",
            "تيتو, جدي",
            "نينا, جدتي",
            "سيد, السيد",
            "مدام, السيدة"
        })
        @DisplayName("Family terms normalize correctly")
        void testFamilyTermsNormalize(String egyptian, String expected) {
            String result = EgyptianNormalizer.normalizeContactName(egyptian);
            assertEquals(expected, result);
        }

        @ParameterizedTest
        @CsvSource({
            "حبيبي, زوجي",
            "حبيبتي, زوجتي",
            "ابني, ولدي",
            "بنتي, ابنتي",
            "أختي, أختي",
            "أخويا, أخي",
            "خويا, أخي",
            "ختي, أختي",
            "خالة, خالتي",
            "عمة, عمتي"
        })
        @DisplayName("Extended family terms normalize correctly")
        void testExtendedFamilyTerms(String egyptian, String expected) {
            String result = EgyptianNormalizer.normalizeContactName(egyptian);
            assertEquals(expected, result);
        }

        @ParameterizedTest
        @CsvSource({
            "ريّس, الرئيس",
            "أستاذ, الأستاذ",
            "دكتور, الدكتور",
            "مهندس, المهندس",
            "باشا, السيد",
            "هانم, السيدة",
            "بيك, السيد",
            "كابتن, الكابتن"
        })
        @DisplayName("Professional titles normalize correctly")
        void testProfessionalTitles(String egyptian, String expected) {
            String result = EgyptianNormalizer.normalizeContactName(egyptian);
            assertEquals(expected, result);
        }

        @ParameterizedTest
        @CsvSource({
            "يا روحي, حبيبي",
            "يا عمري, حبيبي",
            "يا قمر, حبيبي",
            "يا غالي, حبيبي"
        })
        @DisplayName("Endearment terms normalize correctly")
        void testEndearmentTerms(String egyptian, String expected) {
            String result = EgyptianNormalizer.normalizeContactName(egyptian);
            assertEquals(expected, result);
        }

        @ParameterizedTest
        @CsvSource({
            "أحمدو, أحمد",
            "محمودو, محمود",
            "سوسو, سوزان",
            "ميمي, مريم"
        })
        @DisplayName("Name aliases normalize correctly")
        void testNameAliases(String egyptian, String expected) {
            String result = EgyptianNormalizer.normalizeContactName(egyptian);
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("Unknown contact name remains unchanged")
        void testUnknownContactNameUnchanged() {
            String result = EgyptianNormalizer.normalizeContactName("محمد أحمد");
            assertEquals("محمد أحمد", result);
        }

        @Test
        @DisplayName("Partial match in contact name is replaced")
        void testPartialMatchContactName() {
            String result = EgyptianNormalizer.normalizeContactName("كلم ماما");
            assertTrue(result.contains("أمي"));
        }
    }

    @Nested
    @DisplayName("Time Expression Normalization Tests")
    class TimeExpressionNormalizationTests {

        @Test
        @DisplayName("Null time expression returns empty string")
        void testNormalizeTimeExpressionNull() {
            String result = EgyptianNormalizer.normalizeTimeExpression(null);
            assertEquals("", result);
        }

        @Test
        @DisplayName("Empty time expression returns empty string")
        void testNormalizeTimeExpressionEmpty() {
            String result = EgyptianNormalizer.normalizeTimeExpression("");
            assertEquals("", result);
        }

        @ParameterizedTest
        @CsvSource({
            "الصبح, 08:00",
            "الصبح بدري, 07:00",
            "الظهر, 12:00",
            "بعد الظهر, 14:00",
            "العصر, 16:00",
            "المغرب, 18:00",
            "العشا, 20:00",
            "الليل, 21:00",
            "بالليل, 21:00",
            "نص الليل, 00:00",
            "نص النهار, 12:00"
        })
        @DisplayName("Standard time expressions normalize correctly")
        void testStandardTimeExpressions(String expression, String expected) {
            String result = EgyptianNormalizer.normalizeTimeExpression(expression);
            assertEquals(expected, result);
        }

        @ParameterizedTest
        @CsvSource({
            "الفجر, 05:00",
            "الشروق, 06:00",
            "بدري الصبح, 07:00",
            "قبل الضهر, 11:00",
            "بعد الضهر, 14:00",
            "قبل المغرب, 17:00",
            "بعد المغرب, 19:00",
            "بعد العشا, 21:00",
            "آخر الليل, 23:00"
        })
        @DisplayName("Expanded time expressions normalize correctly")
        void testExpandedTimeExpressions(String expression, String expected) {
            String result = EgyptianNormalizer.normalizeTimeExpression(expression);
            assertEquals(expected, result);
        }

        @ParameterizedTest
        @CsvSource({
            "بعد نص ساعة, 30 دقيقة",
            "بعد ساعة, 60 دقيقة",
            "بعد ساعتين, 120 دقيقة",
            "بعد تلت ساعة, 20 دقيقة",
            "بعد ربع ساعة, 15 دقيقة",
            "بعد شوية, بعد قليل"
        })
        @DisplayName("Relative time expressions normalize correctly")
        void testRelativeTimeExpressions(String expression, String expected) {
            String result = EgyptianNormalizer.normalizeTimeExpression(expression);
            assertEquals(expected, result);
        }

        @ParameterizedTest
        @CsvSource({
            "النهاردة الصبح, اليوم الصباح",
            "النهاردة الضهر, اليوم الظهر",
            "بكرة بدري, غداً مبكراً",
            "بكرة الصبح, غداً الصباح",
            "بكرة الضهر, غداً الظهر",
            "بكرة العصر, غداً العصر",
            "بكرة المغرب, غداً المغرب",
            "بكرة العشا, غداً العشاء",
            "بكرة الليل, غداً الليل"
        })
        @DisplayName("Day references normalize correctly")
        void testDayReferences(String expression, String expected) {
            String result = EgyptianNormalizer.normalizeTimeExpression(expression);
            assertEquals(expected, result);
        }

        @ParameterizedTest
        @CsvSource({
            "يوم الجمعة, الجمعة",
            "الجمعة الجاي, الجمعة القادمة",
            "يوم الاتنين, الاثنين",
            "الاتنين الجاي, الاثنين القادم"
        })
        @DisplayName("Weekend references normalize correctly")
        void testWeekendReferences(String expression, String expected) {
            String result = EgyptianNormalizer.normalizeTimeExpression(expression);
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("Advanced Time Parser Tests")
    class AdvancedTimeParserTests {

        @Test
        @DisplayName("Null input returns empty string")
        void testParseTimeExpressionAdvancedNull() {
            String result = EgyptianNormalizer.parseTimeExpressionAdvanced(null);
            assertEquals("", result);
        }

        @Test
        @DisplayName("Empty input returns empty string")
        void testParseTimeExpressionAdvancedEmpty() {
            String result = EgyptianNormalizer.parseTimeExpressionAdvanced("");
            assertEquals("", result);
        }

        @Test
        @DisplayName("Numeric time with morning period")
        void testNumericTimeWithMorning() {
            String result = EgyptianNormalizer.parseTimeExpressionAdvanced("7 الصبح");
            assertTrue(result.contains("7") || result.contains("الصباح"));
        }

        @Test
        @DisplayName("Numeric time with noon period")
        void testNumericTimeWithNoon() {
            String result = EgyptianNormalizer.parseTimeExpressionAdvanced("3 العصر");
            assertTrue(result.contains("3") || result.contains("العصر"));
        }

        @Test
        @DisplayName("Relative time with 'بعد'")
        void testRelativeTimeWithBaad() {
            String result = EgyptianNormalizer.parseTimeExpressionAdvanced("بعد ساعة");
            assertEquals("بعد ساعة", result);
        }

        @Test
        @DisplayName("Relative time with 'نص ساعة'")
        void testRelativeTimeWithNosSaa() {
            String result = EgyptianNormalizer.parseTimeExpressionAdvanced("بعد نص ساعة");
            assertEquals("بعد 30 دقيقة", result);
        }

        @Test
        @DisplayName("Relative time with 'ربع ساعة'")
        void testRelativeTimeWithRobSaa() {
            String result = EgyptianNormalizer.parseTimeExpressionAdvanced("بعد ربع ساعة");
            assertEquals("بعد 15 دقيقة", result);
        }

        @Test
        @DisplayName("Relative time with 'دقيقة'")
        void testRelativeTimeWithDakika() {
            String result = EgyptianNormalizer.parseTimeExpressionAdvanced("بعد 5 دقيقة");
            assertEquals("بعد دقائق", result);
        }

        @Test
        @DisplayName("Relative time with 'شوية'")
        void testRelativeTimeWithShwaya() {
            String result = EgyptianNormalizer.parseTimeExpressionAdvanced("بعد شوية");
            assertEquals("بعد قليل", result);
        }
    }

    @Nested
    @DisplayName("Intent Classification Tests")
    class IntentClassificationTests {

        @Test
        @DisplayName("Null text returns UNKNOWN intent")
        void testClassifyBasicIntentNull() {
            IntentResult result = EgyptianNormalizer.classifyBasicIntent(null);
            assertEquals(IntentType.UNKNOWN, result.getIntentType());
        }

        @Test
        @DisplayName("Empty text returns UNKNOWN intent")
        void testClassifyBasicIntentEmpty() {
            IntentResult result = EgyptianNormalizer.classifyBasicIntent("");
            assertEquals(IntentType.UNKNOWN, result.getIntentType());
        }

        @ParameterizedTest
        @CsvSource({
            "نجدة",
            "استغاثة",
            "طوارئ",
            "ساعدني",
            "انقذني",
            "مش قادر",
            "محتاج مساعدة",
            "حاجة طارئة",
            "في حد يجي"
        })
        @DisplayName("Emergency keywords trigger EMERGENCY intent")
        void testEmergencyIntent(String text) {
            IntentResult result = EgyptianNormalizer.classifyBasicIntent(text);
            assertEquals(IntentType.EMERGENCY, result.getIntentType());
            assertTrue(result.getConfidence() >= 0.75f);
        }

        @ParameterizedTest
        @CsvSource({
            "اتصل بماما",
            "كلم بابا",
            "رن على أحمد",
            "نادي بمحمد"
        })
        @DisplayName("Call patterns trigger CALL_CONTACT intent")
        void testCallContactIntent(String text) {
            IntentResult result = EgyptianNormalizer.classifyBasicIntent(text);
            assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
            assertNotNull(result.getEntity("contact"));
        }

        @ParameterizedTest
        @CsvSource({
            "ابعت واتساب لماما",
            "ارسل رسالة واتساب لبابا",
            "قول واتساب لأحمد"
        })
        @DisplayName("WhatsApp patterns trigger SEND_WHATSAPP intent")
        void testWhatsAppIntent(String text) {
            IntentResult result = EgyptianNormalizer.classifyBasicIntent(text);
            assertEquals(IntentType.SEND_WHATSAPP, result.getIntentType());
        }

        @ParameterizedTest
        @CsvSource({
            "نبهني الصبح",
            "ذكرني الظهر",
            "انبهني بعد ساعة",
            "اضبط منبه للعشا"
        })
        @DisplayName("Alarm patterns trigger SET_ALARM intent")
        void testAlarmIntent(String text) {
            IntentResult result = EgyptianNormalizer.classifyBasicIntent(text);
            assertEquals(IntentType.SET_ALARM, result.getIntentType());
        }

        @ParameterizedTest
        @CsvSource({
            "الساعة كام",
            "كام الساعة",
            "الوقت إيه"
        })
        @DisplayName("Time query patterns trigger READ_TIME intent")
        void testReadTimeIntent(String text) {
            IntentResult result = EgyptianNormalizer.classifyBasicIntent(text);
            assertEquals(IntentType.READ_TIME, result.getIntentType());
        }

        @ParameterizedTest
        @CsvSource({
            "السلام عليكم",
            "أهلاً",
            "مرحبا",
            "ازيك",
            "عامل ايه",
            "صباح الخير",
            "مساء الخير",
            "ألو"
        })
        @DisplayName("Greeting patterns trigger GREETING intent")
        void testGreetingIntent(String text) {
            IntentResult result = EgyptianNormalizer.classifyBasicIntent(text);
            assertEquals(IntentType.GREETING, result.getIntentType());
            assertTrue(result.getConfidence() >= 0.9f);
        }

        @ParameterizedTest
        @CsvSource({
            "شكرا",
            "متشكر",
            "تسلم",
            "يسلمو"
        })
        @DisplayName("Thank you patterns trigger THANK_YOU intent")
        void testThankYouIntent(String text) {
            IntentResult result = EgyptianNormalizer.classifyBasicIntent(text);
            assertEquals(IntentType.THANK_YOU, result.getIntentType());
        }

        @ParameterizedTest
        @CsvSource({
            "مع السلامة",
            "باي",
            "سلام",
            "في أمان الله",
            "أشوفك بعدين"
        })
        @DisplayName("Goodbye patterns trigger GOODBYE intent")
        void testGoodbyeIntent(String text) {
            IntentResult result = EgyptianNormalizer.classifyBasicIntent(text);
            assertEquals(IntentType.GOODBYE, result.getIntentType());
        }

        @ParameterizedTest
        @CsvSource({
            "شغل الواي فاي",
            "افتح الواي فاي",
            "اقفل الواي فاي",
            "شغل wifi",
            "اقفل wifi"
        })
        @DisplayName("WiFi patterns trigger TOGGLE_WIFI intent")
        void testWifiIntent(String text) {
            IntentResult result = EgyptianNormalizer.classifyBasicIntent(text);
            assertEquals(IntentType.TOGGLE_WIFI, result.getIntentType());
        }

        @ParameterizedTest
        @CsvSource({
            "شغل البلوتوث",
            "افتح البلوتوث",
            "اقفل البلوتوث",
            "شغل bluetooth",
            "اقفل bluetooth"
        })
        @DisplayName("Bluetooth patterns trigger TOGGLE_BLUETOOTH intent")
        void testBluetoothIntent(String text) {
            IntentResult result = EgyptianNormalizer.classifyBasicIntent(text);
            assertEquals(IntentType.TOGGLE_BLUETOOTH, result.getIntentType());
        }
    }

    @Nested
    @DisplayName("Confidence Score Tests")
    class ConfidenceScoreTests {

        @Test
        @DisplayName("High confidence keywords boost score")
        void testHighConfidenceKeywords() {
            float score = EgyptianNormalizer.calculateConfidenceScore("نجدة", IntentType.EMERGENCY, false);
            assertTrue(score >= 0.88f, "High confidence keyword should boost score");
        }

        @Test
        @DisplayName("Entity extraction boosts confidence")
        void testEntityBoostsConfidence() {
            float scoreWithEntity = EgyptianNormalizer.calculateConfidenceScore("اتصل بماما", IntentType.CALL_CONTACT, true);
            float scoreWithoutEntity = EgyptianNormalizer.calculateConfidenceScore("اتصل", IntentType.CALL_CONTACT, false);
            assertTrue(scoreWithEntity > scoreWithoutEntity, "Entity should boost confidence");
        }

        @Test
        @DisplayName("Confidence capped at 0.98")
        void testConfidenceCap() {
            float score = EgyptianNormalizer.calculateConfidenceScore("السلام عليكم", IntentType.GREETING, true);
            assertTrue(score <= 0.98f, "Confidence should be capped at 0.98");
        }

        @Test
        @DisplayName("Medium confidence keywords provide moderate boost")
        void testMediumConfidenceKeywords() {
            float score = EgyptianNormalizer.calculateConfidenceScore("كلم", IntentType.CALL_CONTACT, false);
            assertTrue(score >= 0.75f, "Medium confidence keyword should provide boost");
        }
    }

    @Nested
    @DisplayName("Post-Processing Tests")
    class PostProcessingTests {

        @Test
        @DisplayName("Null result handled gracefully")
        void testPostProcessingNullResult() {
            assertDoesNotThrow(() -> EgyptianNormalizer.applyPostProcessingRules(null));
        }

        @Test
        @DisplayName("Contact entity is normalized")
        void testContactEntityNormalized() {
            IntentResult result = new IntentResult();
            result.setEntity("contact", "ماما");
            EgyptianNormalizer.applyPostProcessingRules(result);
            assertEquals("أمي", result.getEntity("contact"));
        }

        @Test
        @DisplayName("Time entity is normalized")
        void testTimeEntityNormalized() {
            IntentResult result = new IntentResult();
            result.setEntity("time", "الصبح");
            EgyptianNormalizer.applyPostProcessingRules(result);
            assertEquals("08:00", result.getEntity("time"));
        }

        @Test
        @DisplayName("Multiple entities are normalized")
        void testMultipleEntitiesNormalized() {
            IntentResult result = new IntentResult();
            result.setEntity("contact", "بابا");
            result.setEntity("time", "الظهر");
            EgyptianNormalizer.applyPostProcessingRules(result);
            assertEquals("أبي", result.getEntity("contact"));
            assertEquals("12:00", result.getEntity("time"));
        }
    }

    @Nested
    @DisplayName("Sentence Normalization Tests")
    class SentenceNormalizationTests {

        @Test
        @DisplayName("Full Egyptian sentence normalizes correctly")
        void testFullEgyptianSentence() {
            String result = EgyptianNormalizer.normalize("كلم ماما دلوقتي");
            assertTrue(result.contains("اتصل") || result.contains("أمي"));
        }

        @Test
        @DisplayName("Mixed Egyptian and MSA normalizes correctly")
        void testMixedEgyptianMSA() {
            String result = EgyptianNormalizer.normalize("ابعت رسالة لماما");
            assertTrue(result.contains("أرسل") || result.contains("أمي"));
        }

        @Test
        @DisplayName("Long Egyptian sentence normalizes correctly")
        void testLongEgyptianSentence() {
            String result = EgyptianNormalizer.normalize("ازيك يا باشا عامل ايه كله تمام");
            assertTrue(result.contains("كيف حالك") || result.contains("جيد"));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Robustness Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Single character input handled")
        void testSingleCharacterInput() {
            String result = EgyptianNormalizer.normalize("أ");
            assertEquals("أ", result);
        }

        @Test
        @DisplayName("Numbers in text preserved")
        void testNumbersPreserved() {
            String result = EgyptianNormalizer.normalize("الساعة 7 الصبح");
            assertTrue(result.contains("7") || result.contains("الصباح"));
        }

        @Test
        @DisplayName("Punctuation preserved")
        void testPunctuationPreserved() {
            String result = EgyptianNormalizer.normalize("ازيك؟");
            assertTrue(result.contains("?") || result.contains("كيف"));
        }

        @Test
        @DisplayName("Multiple spaces collapsed")
        void testMultipleSpacesCollapsed() {
            String result = EgyptianNormalizer.normalize("كلم   ماما");
            assertFalse(result.contains("  "));
        }

        @Test
        @DisplayName("Leading and trailing spaces trimmed")
        void testSpacesTrimmed() {
            String result = EgyptianNormalizer.normalize("  كلم ماما  ");
            assertFalse(result.startsWith(" "));
            assertFalse(result.endsWith(" "));
        }

        @Test
        @DisplayName("English words preserved")
        void testEnglishWordsPreserved() {
            String result = EgyptianNormalizer.normalize("شغل wifi");
            assertTrue(result.contains("wifi") || result.contains("واي فاي"));
        }

        @Test
        @DisplayName("Arabic numerals handled")
        void testArabicNumerals() {
            String result = EgyptianNormalizer.normalize("الساعة ٧");
            assertNotNull(result);
        }

        @Test
        @DisplayName("Diacritics handled gracefully")
        void testDiacriticsHandled() {
            String result = EgyptianNormalizer.normalize("كِتَابٌ");
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Regional Variation Tests")
    class RegionalVariationTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "ازيك",
            "عامل ايه",
            "أهلاً",
            "يا باشا"
        })
        @DisplayName("Cairo dialect variations handled")
        void testCairoDialect(String text) {
            String result = EgyptianNormalizer.normalize(text);
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "إيه الأخبار",
            "أخبار إيه",
            "كله تمام",
            "الحمد لله"
        })
        @DisplayName("Common Egyptian phrases handled")
        void testCommonPhrases(String text) {
            String result = EgyptianNormalizer.normalize(text);
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("IntentResult Entity Tests")
    class IntentResultEntityTests {

        @Test
        @DisplayName("Call intent extracts contact entity")
        void testCallExtractsContact() {
            IntentResult result = EgyptianNormalizer.classifyBasicIntent("كلم أحمد");
            assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
            assertNotNull(result.getEntity("contact"));
        }

        @Test
        @DisplayName("Alarm intent extracts time entity")
        void testAlarmExtractsTime() {
            IntentResult result = EgyptianNormalizer.classifyBasicIntent("نبهني الصبح");
            assertEquals(IntentType.SET_ALARM, result.getIntentType());
            assertNotNull(result.getEntity("time"));
        }

        @Test
        @DisplayName("WhatsApp intent extracts contact entity")
        void testWhatsAppExtractsContact() {
            IntentResult result = EgyptianNormalizer.classifyBasicIntent("ابعت واتساب لمحمد");
            assertEquals(IntentType.SEND_WHATSAPP, result.getIntentType());
            assertNotNull(result.getEntity("contact"));
        }

        @Test
        @DisplayName("Original text preserved in result")
        void testOriginalTextPreserved() {
            String input = "كلم ماما";
            IntentResult result = EgyptianNormalizer.classifyBasicIntent(input);
            assertEquals(input, result.getOriginalText());
        }
    }

    @Nested
    @DisplayName("Performance and Stress Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Rapid consecutive normalizations")
        void testRapidConsecutiveNormalizations() {
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                EgyptianNormalizer.normalize("كلم ماما");
            }
            long duration = System.currentTimeMillis() - startTime;
            assertTrue(duration < 5000, "1000 normalizations should complete in under 5 seconds");
        }

        @Test
        @DisplayName("Long text normalization")
        void testLongTextNormalization() {
            String longText = "كلم ماما وقولها اني هاجي متأخر شوية لأن في زحمة في الطريق وهحاول اوصل بدري";
            String result = EgyptianNormalizer.normalize(longText);
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Unicode characters handled")
        void testUnicodeCharacters() {
            String text = "ازيك 😊 عامل ايه 👋";
            String result = EgyptianNormalizer.normalize(text);
            assertNotNull(result);
        }
    }
}
