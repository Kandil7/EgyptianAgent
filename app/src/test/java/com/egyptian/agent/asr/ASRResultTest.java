package com.egyptian.agent.asr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ASRResult Comprehensive Test Suite
 * 
 * Tests for ASR result container and validation.
 * Covers result creation, validation, and edge cases.
 * 
 * Coverage Target: 85%
 */
@DisplayName("ASRResult Tests")
class ASRResultTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("default constructor creates valid result")
        void testDefaultConstructor() {
            ASRResult result = new ASRResult();
            
            assertNotNull(result);
            assertEquals("", result.getText());
            assertEquals(0.0f, result.getConfidence());
            assertEquals(0, result.getProcessingTimeMs());
            assertFalse(result.isFinal());
        }

        @Test
        @DisplayName("full constructor creates result with all fields")
        void testFullConstructor() {
            ASRResult result = new ASRResult("test text", 0.95f, 100, true);
            
            assertNotNull(result);
            assertEquals("test text", result.getText());
            assertEquals(0.95f, result.getConfidence());
            assertEquals(100, result.getProcessingTimeMs());
            assertTrue(result.isFinal());
        }

        @Test
        @DisplayName("constructor with empty text")
        void testConstructorWithEmptyText() {
            ASRResult result = new ASRResult("", 0.5f, 50, false);
            
            assertEquals("", result.getText());
            assertEquals(0.5f, result.getConfidence());
            assertEquals(50, result.getProcessingTimeMs());
            assertFalse(result.isFinal());
        }

        @Test
        @DisplayName("constructor with null text")
        void testConstructorWithNullText() {
            ASRResult result = new ASRResult(null, 0.5f, 50, false);
            
            assertNull(result.getText());
            assertEquals(0.5f, result.getConfidence());
        }

        @Test
        @DisplayName("constructor with zero confidence")
        void testConstructorWithZeroConfidence() {
            ASRResult result = new ASRResult("text", 0.0f, 50, true);
            
            assertEquals(0.0f, result.getConfidence());
        }

        @Test
        @DisplayName("constructor with max confidence")
        void testConstructorWithMaxConfidence() {
            ASRResult result = new ASRResult("text", 1.0f, 50, true);
            
            assertEquals(1.0f, result.getConfidence());
        }

        @Test
        @DisplayName("constructor with zero processing time")
        void testConstructorWithZeroProcessingTime() {
            ASRResult result = new ASRResult("text", 0.5f, 0, true);
            
            assertEquals(0, result.getProcessingTimeMs());
        }

        @Test
        @DisplayName("constructor with large processing time")
        void testConstructorWithLargeProcessingTime() {
            ASRResult result = new ASRResult("text", 0.5f, 10000, true);
            
            assertEquals(10000, result.getProcessingTimeMs());
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("getText returns text")
        void testGetText() {
            ASRResult result = new ASRResult("hello world", 0.9f, 100, true);
            assertEquals("hello world", result.getText());
        }

        @Test
        @DisplayName("getConfidence returns confidence")
        void testGetConfidence() {
            ASRResult result = new ASRResult("text", 0.85f, 100, true);
            assertEquals(0.85f, result.getConfidence());
        }

        @Test
        @DisplayName("getProcessingTimeMs returns processing time")
        void testGetProcessingTimeMs() {
            ASRResult result = new ASRResult("text", 0.85f, 250, true);
            assertEquals(250, result.getProcessingTimeMs());
        }

        @Test
        @DisplayName("isFinal returns final flag")
        void testIsFinal() {
            ASRResult result1 = new ASRResult("text", 0.85f, 100, true);
            ASRResult result2 = new ASRResult("text", 0.85f, 100, false);
            
            assertTrue(result1.isFinal());
            assertFalse(result2.isFinal());
        }
    }

    @Nested
    @DisplayName("Setter Tests")
    class SetterTests {

        @Test
        @DisplayName("setText updates text")
        void testSetText() {
            ASRResult result = new ASRResult();
            result.setText("new text");
            assertEquals("new text", result.getText());
        }

        @Test
        @DisplayName("setText with null")
        void testSetTextNull() {
            ASRResult result = new ASRResult();
            result.setText(null);
            assertNull(result.getText());
        }

        @Test
        @DisplayName("setText with empty string")
        void testSetTextEmpty() {
            ASRResult result = new ASRResult();
            result.setText("");
            assertEquals("", result.getText());
        }

        @Test
        @DisplayName("setConfidence updates confidence")
        void testSetConfidence() {
            ASRResult result = new ASRResult();
            result.setConfidence(0.75f);
            assertEquals(0.75f, result.getConfidence());
        }

        @Test
        @DisplayName("setConfidence with zero")
        void testSetConfidenceZero() {
            ASRResult result = new ASRResult();
            result.setConfidence(0.0f);
            assertEquals(0.0f, result.getConfidence());
        }

        @Test
        @DisplayName("setConfidence with one")
        void testSetConfidenceOne() {
            ASRResult result = new ASRResult();
            result.setConfidence(1.0f);
            assertEquals(1.0f, result.getConfidence());
        }

        @Test
        @DisplayName("setConfidence with negative value")
        void testSetConfidenceNegative() {
            ASRResult result = new ASRResult();
            result.setConfidence(-0.5f);
            assertEquals(-0.5f, result.getConfidence());
        }

        @Test
        @DisplayName("setConfidence with value greater than one")
        void testSetConfidenceGreaterThanOne() {
            ASRResult result = new ASRResult();
            result.setConfidence(1.5f);
            assertEquals(1.5f, result.getConfidence());
        }

        @Test
        @DisplayName("setProcessingTimeMs updates processing time")
        void testSetProcessingTimeMs() {
            ASRResult result = new ASRResult();
            result.setProcessingTimeMs(500);
            assertEquals(500, result.getProcessingTimeMs());
        }

        @Test
        @DisplayName("setProcessingTimeMs with zero")
        void testSetProcessingTimeMsZero() {
            ASRResult result = new ASRResult();
            result.setProcessingTimeMs(0);
            assertEquals(0, result.getProcessingTimeMs());
        }

        @Test
        @DisplayName("setProcessingTimeMs with negative value")
        void testSetProcessingTimeMsNegative() {
            ASRResult result = new ASRResult();
            result.setProcessingTimeMs(-100);
            assertEquals(-100, result.getProcessingTimeMs());
        }

        @Test
        @DisplayName("setFinal updates final flag")
        void testSetFinal() {
            ASRResult result = new ASRResult();
            result.setFinal(true);
            assertTrue(result.isFinal());
            
            result.setFinal(false);
            assertFalse(result.isFinal());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("isValid returns false for empty text")
        void testIsValidEmptyText() {
            ASRResult result = new ASRResult("", 0.9f, 100, true);
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("isValid returns false for null text")
        void testIsValidNullText() {
            ASRResult result = new ASRResult(null, 0.9f, 100, true);
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("isValid returns false for low confidence")
        void testIsValidLowConfidence() {
            ASRResult result = new ASRResult("text", 0.3f, 100, true);
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("isValid returns true for valid result")
        void testIsValidValidResult() {
            ASRResult result = new ASRResult("text", 0.8f, 100, true);
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("isValid with threshold confidence")
        void testIsValidThresholdConfidence() {
            ASRResult result = new ASRResult("text", 0.5f, 100, true);
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("isValid with just below threshold confidence")
        void testIsValidJustBelowThreshold() {
            ASRResult result = new ASRResult("text", 0.49f, 100, true);
            assertFalse(result.isValid());
        }
    }

    @Nested
    @DisplayName("Arabic Text Tests")
    class ArabicTextTests {

        @Test
        @DisplayName("constructor with Arabic text")
        void testConstructorWithArabicText() {
            ASRResult result = new ASRResult("مرحبا بك", 0.9f, 100, true);
            assertEquals("مرحبا بك", result.getText());
        }

        @Test
        @DisplayName("constructor with Egyptian dialect")
        void testConstructorWithEgyptianDialect() {
            ASRResult result = new ASRResult("ازيك يا باشا", 0.85f, 100, true);
            assertEquals("ازيك يا باشا", result.getText());
        }

        @Test
        @DisplayName("constructor with mixed Arabic and English")
        void testConstructorWithMixedText() {
            ASRResult result = new ASRResult("كلم ماما على whatsapp", 0.8f, 100, true);
            assertEquals("كلم ماما على whatsapp", result.getText());
        }

        @Test
        @DisplayName("constructor with Arabic numbers")
        void testConstructorWithArabicNumbers() {
            ASRResult result = new ASRResult("الساعة ٧:٣٠", 0.75f, 100, true);
            assertEquals("الساعة ٧:٣٠", result.getText());
        }

        @Test
        @DisplayName("setText with Arabic text")
        void testSetTextWithArabicText() {
            ASRResult result = new ASRResult();
            result.setText("نبهني الصبح");
            assertEquals("نبهني الصبح", result.getText());
        }

        @Test
        @DisplayName("setText with long Arabic text")
        void testSetTextWithLongArabicText() {
            ASRResult result = new ASRResult();
            String longText = "أريد أن أقول كلاماً طويلاً جداً باللغة العربية الفصحى";
            result.setText(longText);
            assertEquals(longText, result.getText());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("result with whitespace only text")
        void testResultWithWhitespaceOnlyText() {
            ASRResult result = new ASRResult("   ", 0.5f, 100, true);
            assertEquals("   ", result.getText());
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("result with special characters")
        void testResultWithSpecialCharacters() {
            ASRResult result = new ASRResult("!@#$%^&*()", 0.5f, 100, true);
            assertEquals("!@#$%^&*()", result.getText());
        }

        @Test
        @DisplayName("result with emojis")
        void testResultWithEmojis() {
            ASRResult result = new ASRResult("مرحبا 👋 🎉", 0.8f, 100, true);
            assertEquals("مرحبا 👋 🎉", result.getText());
        }

        @Test
        @DisplayName("result with newlines")
        void testResultWithNewlines() {
            ASRResult result = new ASRResult("line1\nline2\nline3", 0.8f, 100, true);
            assertEquals("line1\nline2\nline3", result.getText());
        }

        @Test
        @DisplayName("result with tabs")
        void testResultWithTabs() {
            ASRResult result = new ASRResult("col1\tcol2\tcol3", 0.8f, 100, true);
            assertEquals("col1\tcol2\tcol3", result.getText());
        }

        @Test
        @DisplayName("result with very long text")
        void testResultWithVeryLongText() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                sb.append("word ");
            }
            ASRResult result = new ASRResult(sb.toString(), 0.8f, 100, true);
            assertTrue(result.getText().length() > 4000);
        }

        @Test
        @DisplayName("result with unicode characters")
        void testResultWithUnicodeCharacters() {
            ASRResult result = new ASRResult("Hello 世界 🌍", 0.8f, 100, true);
            assertEquals("Hello 世界 🌍", result.getText());
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("rapid result creation")
        void testRapidResultCreation() {
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                new ASRResult("text", 0.8f, 100, true);
            }
            long duration = System.currentTimeMillis() - startTime;
            
            assertTrue(duration < 5000, "10000 result creations should complete in under 5 seconds");
        }

        @Test
        @DisplayName("rapid getter calls")
        void testRapidGetterCalls() {
            ASRResult result = new ASRResult("text", 0.8f, 100, true);
            
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 100000; i++) {
                result.getText();
                result.getConfidence();
                result.getProcessingTimeMs();
                result.isFinal();
            }
            long duration = System.currentTimeMillis() - startTime;
            
            assertTrue(duration < 5000, "100000 getter calls should complete in under 5 seconds");
        }

        @Test
        @DisplayName("rapid setter calls")
        void testRapidSetterCalls() {
            ASRResult result = new ASRResult();
            
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                result.setText("text" + i);
                result.setConfidence(0.8f);
                result.setProcessingTimeMs(100);
                result.setFinal(true);
            }
            long duration = System.currentTimeMillis() - startTime;
            
            assertTrue(duration < 5000, "10000 setter calls should complete in under 5 seconds");
        }
    }

    @Nested
    @DisplayName("Confidence Threshold Tests")
    class ConfidenceThresholdTests {

        @ParameterizedTest
        @ValueSource(floats = {0.0f, 0.1f, 0.2f, 0.3f, 0.4f})
        @DisplayName("low confidence results are invalid")
        void testLowConfidenceInvalid(float confidence) {
            ASRResult result = new ASRResult("text", confidence, 100, true);
            assertFalse(result.isValid());
        }

        @ParameterizedTest
        @ValueSource(floats = {0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f})
        @DisplayName("high confidence results are valid")
        void testHighConfidenceValid(float confidence) {
            ASRResult result = new ASRResult("text", confidence, 100, true);
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("confidence at exactly 0.5 is valid")
        void testConfidenceExactlyThreshold() {
            ASRResult result = new ASRResult("text", 0.5f, 100, true);
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("confidence just below 0.5 is invalid")
        void testConfidenceJustBelowThreshold() {
            ASRResult result = new ASRResult("text", 0.499f, 100, true);
            assertFalse(result.isValid());
        }
    }

    @Nested
    @DisplayName("Processing Time Tests")
    class ProcessingTimeTests {

        @Test
        @DisplayName("processing time of 0 is valid")
        void testProcessingTimeZero() {
            ASRResult result = new ASRResult("text", 0.8f, 0, true);
            assertEquals(0, result.getProcessingTimeMs());
        }

        @Test
        @DisplayName("processing time under 100ms is fast")
        void testProcessingTimeFast() {
            ASRResult result = new ASRResult("text", 0.8f, 50, true);
            assertTrue(result.getProcessingTimeMs() < 100);
        }

        @Test
        @DisplayName("processing time over 1000ms is slow")
        void testProcessingTimeSlow() {
            ASRResult result = new ASRResult("text", 0.8f, 1500, true);
            assertTrue(result.getProcessingTimeMs() > 1000);
        }

        @Test
        @DisplayName("processing time can be negative (edge case)")
        void testProcessingTimeNegative() {
            ASRResult result = new ASRResult("text", 0.8f, -100, true);
            assertEquals(-100, result.getProcessingTimeMs());
        }

        @Test
        @DisplayName("processing time max integer value")
        void testProcessingTimeMaxValue() {
            ASRResult result = new ASRResult("text", 0.8f, Integer.MAX_VALUE, true);
            assertEquals(Integer.MAX_VALUE, result.getProcessingTimeMs());
        }
    }

    @Nested
    @DisplayName("Final Flag Tests")
    class FinalFlagTests {

        @Test
        @DisplayName("final result indicates complete transcription")
        void testFinalResultIndicatesComplete() {
            ASRResult result = new ASRResult("complete text", 0.9f, 100, true);
            assertTrue(result.isFinal());
        }

        @Test
        @DisplayName("non-final result indicates partial transcription")
        void testNonFinalResultIndicatesPartial() {
            ASRResult result = new ASRResult("partial", 0.7f, 50, false);
            assertFalse(result.isFinal());
        }

        @Test
        @DisplayName("can toggle final flag")
        void testCanToggleFinalFlag() {
            ASRResult result = new ASRResult();
            result.setFinal(true);
            assertTrue(result.isFinal());
            
            result.setFinal(false);
            assertFalse(result.isFinal());
        }

        @Test
        @DisplayName("final flag does not affect validity")
        void testFinalFlagDoesNotAffectValidity() {
            ASRResult finalResult = new ASRResult("text", 0.8f, 100, true);
            ASRResult nonFinalResult = new ASRResult("text", 0.8f, 100, false);
            
            assertTrue(finalResult.isValid());
            assertTrue(nonFinalResult.isValid());
        }
    }
}
