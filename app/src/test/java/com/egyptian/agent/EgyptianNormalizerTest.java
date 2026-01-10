package com.egyptian.agent;

import com.egyptian.agent.stt.EgyptianNormalizer;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for EgyptianNormalizer
 */
public class EgyptianNormalizerTest {

    @Test
    public void testNormalizeCommonEgyptianPhrases() {
        // Test common Egyptian dialect phrases
        assertEquals("اتصل بالأم", EgyptianNormalizer.normalize("اتصل بأمي"));
        assertEquals("اتصل بالأب", EgyptianNormalizer.normalize("اتصل بأبي"));
        assertEquals("غداً", EgyptianNormalizer.normalize("بكرة"));
        assertEquals("أمس", EgyptianNormalizer.normalize("امبارح"));
        assertEquals("اليوم", EgyptianNormalizer.normalize("النهارده"));
        assertEquals("الآن", EgyptianNormalizer.normalize("دلوقتي"));
    }

    @Test
    public void testNormalizeCommands() {
        // Test command normalization
        assertEquals("اتصل", EgyptianNormalizer.normalize("كلم"));
        assertEquals("اتصل", EgyptianNormalizer.normalize("رن"));
        assertEquals("أرسل", EgyptianNormalizer.normalize("ابعت"));
        assertEquals("ذكرني", EgyptianNormalizer.normalize("انبهني"));
        assertEquals("ذكرني", EgyptianNormalizer.normalize("نبهني"));
    }

    @Test
    public void testNormalizeTimeExpressions() {
        // Test time expression normalization
        assertEquals("الصباح", EgyptianNormalizer.normalize("الصبح"));
        assertEquals("الظهر", EgyptianNormalizer.normalize("الضهر"));
        assertEquals("المساء", EgyptianNormalizer.normalize("العشية"));
        assertEquals("الليل", EgyptianNormalizer.normalize("الليل"));
    }

    @Test
    public void testNormalizeContactReferences() {
        // Test contact reference normalization
        assertEquals("الأم", EgyptianNormalizer.normalize("ماما"));
        assertEquals("الأم", EgyptianNormalizer.normalize("أمي"));
        assertEquals("الأب", EgyptianNormalizer.normalize("بابا"));
        assertEquals("الأب", EgyptianNormalizer.normalize("أبي"));
        assertEquals("الزوجة", EgyptianNormalizer.normalize("مراتي"));
        assertEquals("الزوج", EgyptianNormalizer.normalize("جوزي"));
    }

    @Test
    public void testExtractContactName() {
        // Test contact name extraction
        assertEquals("أمي", EgyptianNormalizer.extractContactName("اتصل بأمي دلوقتي"));
        assertEquals("بابا", EgyptianNormalizer.extractContactName("كلم بابا بكرة"));
        assertEquals("دكتور أحمد", EgyptianNormalizer.extractContactName("ابعت واتساب لدكتور أحمد"));
    }

    @Test
    public void testExtractTimeExpression() {
        // Test time expression extraction
        assertEquals("بكرة الصبح", EgyptianNormalizer.extractTimeExpression("انبهني بكرة الصبح"));
        assertEquals("بعد ساعة", EgyptianNormalizer.extractTimeExpression("انبهني بعد ساعة"));
    }

    @Test
    public void testNormalizeMixedText() {
        // Test mixed text with Egyptian dialect
        String input = "اتصل بأمي بكرة الصبح";
        String expected = "اتصل بالأم غداً الصباح";
        String result = EgyptianNormalizer.normalize(input);
        
        assertTrue(result.contains("اتصل"));
        assertTrue(result.contains("الأم"));
        assertTrue(result.contains("غداً"));
        assertTrue(result.contains("الصباح"));
    }

    @Test
    public void testNormalizeEmptyOrNull() {
        // Test edge cases
        assertEquals("", EgyptianNormalizer.normalize(""));
        assertEquals("", EgyptianNormalizer.normalize(null));
    }

    @Test
    public void testNormalizeWithNumbers() {
        // Test normalization with Arabic numerals
        String input = "الرقم ٠١٠٠٠٠٠٠٠٠";
        String expected = "الرقم 0100000000";
        assertEquals(expected, EgyptianNormalizer.normalize(input));
    }
}