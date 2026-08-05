package com.egyptian.agent;

import com.egyptian.agent.nlu.EgyptianNormalizer;
import com.egyptian.agent.nlu.IntentType;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for EgyptianNormalizer (nlu package implementation)
 * 
 * Covers the actual behavior of the production normalizer:
 * - Egyptian dialect to MSA word mapping
 * - Contact name normalization
 * - Time expression normalization and extraction
 * - Entity-aware intent classification smoke tests
 */
public class EgyptianNormalizerTest {

    @Test
    public void testNormalizeCommonEgyptianPhrases() {
        // Test common Egyptian dialect phrases
        assertEquals("اتصل", EgyptianNormalizer.normalize("كلم"));
        assertEquals("غداً", EgyptianNormalizer.normalize("بكرة"));
        assertEquals("اليوم", EgyptianNormalizer.normalize("النهاردة"));
        assertEquals("الآن", EgyptianNormalizer.normalize("دلوقتي"));
    }

    @Test
    public void testNormalizeCommands() {
        // Test command normalization
        assertEquals("اتصل", EgyptianNormalizer.normalize("اتصل"));
        assertEquals("أرسل", EgyptianNormalizer.normalize("ابعت"));
        assertEquals("أحضر", EgyptianNormalizer.normalize("هات"));
        assertEquals("اذهب", EgyptianNormalizer.normalize("روح"));
    }

    @Test
    public void testNormalizeTimeExpressions() {
        // Test time expression normalization
        assertEquals("الصباح", EgyptianNormalizer.normalize("الصبح"));
        assertEquals("الظهر", EgyptianNormalizer.normalize("الظهر"));
        assertEquals("العصر", EgyptianNormalizer.normalize("العصر"));
        assertEquals("الليل", EgyptianNormalizer.normalize("الليل"));
    }

    @Test
    public void testNormalizeUnmappedWordsPassThrough() {
        // Unmapped dialect words pass through unchanged (partial coverage)
        assertEquals("امبارح", EgyptianNormalizer.normalize("امبارح"));
        assertEquals("الضهر", EgyptianNormalizer.normalize("الضهر"));
        assertEquals("ماما", EgyptianNormalizer.normalize("ماما"));
    }

    @Test
    public void testNormalizeContactName() {
        // Test contact name normalization
        assertEquals("أمي", EgyptianNormalizer.normalizeContactName("ماما"));
        assertEquals("أبي", EgyptianNormalizer.normalizeContactName("بابا"));
        assertEquals("جدتي", EgyptianNormalizer.normalizeContactName("تيتا"));
        assertEquals("جدي", EgyptianNormalizer.normalizeContactName("تيتو"));
        assertEquals("زوجي", EgyptianNormalizer.normalizeContactName("حبيبي"));
        assertEquals("زوجتي", EgyptianNormalizer.normalizeContactName("حبيبتي"));
        // Partial match replaces title inside a longer name
        assertEquals("الدكتور أحمد", EgyptianNormalizer.normalizeContactName("دكتور أحمد"));
    }

    @Test
    public void testNormalizeTimeExpression() {
        // Test time expression normalization
        assertEquals("08:00", EgyptianNormalizer.normalizeTimeExpression("الصبح"));
        assertEquals("12:00", EgyptianNormalizer.normalizeTimeExpression("الظهر"));
        assertEquals("غداً الصباح", EgyptianNormalizer.normalizeTimeExpression("بكرة الصبح"));
        assertEquals("60 دقيقة", EgyptianNormalizer.normalizeTimeExpression("بعد ساعة"));
        assertEquals("30 دقيقة", EgyptianNormalizer.normalizeTimeExpression("بعد نص ساعة"));
    }

    @Test
    public void testExtractTimeExpression() {
        // Test time expression extraction from full commands
        assertEquals("بكرة الصبح", EgyptianNormalizer.extractTimeExpression("انبهني بكرة الصبح"));
        assertEquals("بعد ساعة", EgyptianNormalizer.extractTimeExpression("انبهني بعد ساعة"));
        assertEquals("الساعة 8", EgyptianNormalizer.extractTimeExpression("ذكرني الساعة 8"));
        assertEquals("الصبح", EgyptianNormalizer.extractTimeExpression("اضبط منبه الصبح"));
    }

    @Test
    public void testExtractTimeExpression_NoMatch() {
        // Commands without time expressions return empty string
        assertEquals("", EgyptianNormalizer.extractTimeExpression("اتصل بماما"));
        assertEquals("", EgyptianNormalizer.extractTimeExpression(""));
        assertEquals("", EgyptianNormalizer.extractTimeExpression(null));
    }

    @Test
    public void testNormalizeMixedText() {
        // Test mixed text with Egyptian dialect
        String input = "كلم ماما بكرة الصبح";
        String expected = "اتصل ماما غداً الصباح";
        assertEquals(expected, EgyptianNormalizer.normalize(input));
    }

    @Test
    public void testNormalizeEmptyOrNull() {
        // Test edge cases
        assertEquals("", EgyptianNormalizer.normalize(""));
        assertEquals("", EgyptianNormalizer.normalize(null));
        assertEquals("", EgyptianNormalizer.normalizeContactName(""));
        assertEquals("", EgyptianNormalizer.normalizeTimeExpression(""));
    }

    @Test
    public void testClassifyBasicIntent_CallContact() {
        com.egyptian.agent.nlu.IntentResult result = EgyptianNormalizer.classifyBasicIntent("اتصل بماما");
        assertNotNull(result);
        assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
    }

    @Test
    public void testClassifyBasicIntent_SetAlarm() {
        com.egyptian.agent.nlu.IntentResult result = EgyptianNormalizer.classifyBasicIntent("نبهني بكرة الصبح");
        assertNotNull(result);
        assertEquals(IntentType.SET_ALARM, result.getIntentType());
        // ALARM_PATTERN captures the first word after the verb ("غداً" after بكرة→غداً normalization)
        assertEquals("غداً", result.getEntity("time"));
    }

    @Test
    public void testClassifyBasicIntent_Emergency() {
        com.egyptian.agent.nlu.IntentResult result = EgyptianNormalizer.classifyBasicIntent("يا نجدة");
        assertNotNull(result);
        assertEquals(IntentType.EMERGENCY, result.getIntentType());
    }

    @Test
    public void testClassifyBasicIntent_Unknown() {
        com.egyptian.agent.nlu.IntentResult result = EgyptianNormalizer.classifyBasicIntent("جملة عشوائية");
        assertNotNull(result);
        assertEquals(IntentType.UNKNOWN, result.getIntentType());
    }
}
