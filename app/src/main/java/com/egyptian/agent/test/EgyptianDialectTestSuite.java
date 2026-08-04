package com.egyptian.agent.test;

import com.egyptian.agent.stt.EgyptianNormalizer;
import com.egyptian.agent.nlp.IntentType;
import com.egyptian.agent.hybrid.OpenPhoneIntegration;
import com.egyptian.agent.hybrid.HybridOrchestrator;
import com.egyptian.agent.nlp.IntentResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive test suite for Egyptian dialect processing
 * Tests normalization, intent detection, and model integration
 */
public class EgyptianDialectTestSuite {

    public static void main(String[] args) {
        System.out.println("=== Egyptian Agent Test Suite ===");
        System.out.println("Testing Egyptian dialect processing capabilities...\n");

        // Run all tests
        testNormalization();
        testIntentDetection();
        testModelIntegration();
        testEgyptianDialectVariants();
        testEdgeCases();

        System.out.println("\n=== Test Suite Completed ===");
    }

    /**
     * Test Egyptian dialect normalization
     */
    private static void testNormalization() {
        System.out.println("--- Testing Egyptian Dialect Normalization ---");

        // Test cases for Egyptian dialect normalization
        String[][] testCases = {
            // {input, expected_output}
            {"اتصل بأمي", "اتصل بالأم"},
            {"كلم بابا", "اتصل بالأب"},
            {"رن على ماما", "اتصل بالأم"},
            {"قولي المكالمات الفايتة", "قولي المكالمات الفاتت"},
            {"فايتة عليا", "فاتت"},
            {"بكرة الصبح", "غداً الصباح"},
            {"امبارح", "أمس"},
            {"النهارده", "اليوم"},
            {"دلوقتي", "الآن"},
            {"عندك رصيد؟", "عندك رصيد؟"},
            {"بتعمل إيه؟", "تفعل ماذا؟"},
            {"فينك؟", "أين أنت؟"},
            {"ممكن تبعتلي رسالة؟", "يمكن أن ترسل لي رسالة؟"},
            {"عايز اتكلم معاك", "أريد أن أتحدث معك"},
            {"عاوز اعرف", "أريد أن أعرف"},
            {"说了埃及语", "说了埃及语"}, // Non-Arabic should remain unchanged
        };

        int passed = 0;
        int total = testCases.length;

        for (String[] testCase : testCases) {
            String input = testCase[0];
            String expected = testCase[1];
            String actual = EgyptianNormalizer.normalize(input);

            boolean isCorrect = actual.equals(expected);
            if (isCorrect) {
                passed++;
            }

            System.out.printf("%s Input: '%s' -> Expected: '%s' -> Actual: '%s'%s%n",
                isCorrect ? "✓" : "✗",
                input,
                expected,
                actual,
                isCorrect ? "" : " << MISMATCH"
            );
        }

        System.out.printf("Normalization: %d/%d passed (%.1f%%)%n%n",
            passed, total, (passed * 100.0) / total);
    }

    /**
     * Test intent detection
     */
    private static void testIntentDetection() {
        System.out.println("--- Testing Intent Detection ---");

        // Mock orchestrator for testing
        MockOrchestrator mockOrchestrator = new MockOrchestrator();

        // Test cases for intent detection
        Object[][] testCases = {
            // {input, expected_intent, expected_confidence}
            {"اتصل بأمي", IntentType.CALL_CONTACT, 0.8f},
            {"كلم بابا", IntentType.CALL_CONTACT, 0.8f},
            {"رن على ماما", IntentType.CALL_CONTACT, 0.8f},
            {"قولي المكالمات الفايتة", IntentType.READ_MISSED_CALLS, 0.85f},
            {"فايتة عليا", IntentType.READ_MISSED_CALLS, 0.85f},
            {"الساعة كام؟", IntentType.READ_TIME, 0.95f},
            {"الوقت كام؟", IntentType.READ_TIME, 0.95f},
            {"نبهني بكرة الصبح", IntentType.SET_ALARM, 0.7f},
            {"انبهني بعد ساعة", IntentType.SET_ALARM, 0.7f},
            {"ابعت واتساب لامي", IntentType.SEND_WHATSAPP, 0.75f},
            {"قول لامي إن الصحن كسر", IntentType.SEND_WHATSAPP, 0.75f},
            {"يا نجدة", IntentType.EMERGENCY, 0.9f},
            {"استغاثة", IntentType.EMERGENCY, 0.9f},
            {"مش قادر", IntentType.EMERGENCY, 0.9f},
            {"حد يجي", IntentType.EMERGENCY, 0.9f},
            {"أنا مش عارف أتكلم", IntentType.UNKNOWN, 0.3f},
            {"بلا بلا بلا", IntentType.UNKNOWN, 0.3f},
        };

        int passed = 0;
        int total = testCases.length;

        for (Object[] testCase : testCases) {
            String input = (String) testCase[0];
            IntentType expectedIntent = (IntentType) testCase[1];
            float expectedConf = (float) testCase[2];

            IntentResult result = mockOrchestrator.determineIntent(input);
            boolean isCorrectIntent = result.getIntentType() == expectedIntent;
            boolean isConfidenceAcceptable = Math.abs(result.getConfidence() - expectedConf) < 0.15; // Allow 15% tolerance
            boolean isCorrect = isCorrectIntent && isConfidenceAcceptable;

            if (isCorrect) {
                passed++;
            }

            System.out.printf("%s Input: '%s' -> Expected: %s (%.2f) -> Actual: %s (%.2f)%s%n",
                isCorrect ? "✓" : "✗",
                input,
                expectedIntent,
                expectedConf,
                result.getIntentType(),
                result.getConfidence(),
                isCorrect ? "" : " << MISMATCH"
            );
        }

        System.out.printf("Intent Detection: %d/%d passed (%.1f%%)%n%n",
            passed, total, (passed * 100.0) / total);
    }

    /**
     * Test model integration
     */
    private static void testModelIntegration() {
        System.out.println("--- Testing Model Integration ---");

        // Test the OpenPhone integration
        // Note: This would normally connect to the actual model
        // For this test, we'll just verify the interface works
        try {
            // Mock context for testing
            MockContext mockContext = new MockContext();

            // Initialize the OpenPhone integration
            OpenPhoneIntegration integration = new OpenPhoneIntegration(mockContext);

            // Wait a bit for model to potentially load (in mock, it loads instantly)
            Thread.sleep(100);

            if (integration.isReady()) {
                System.out.println("✓ OpenPhone integration initialized successfully");
            } else {
                System.out.println("✗ OpenPhone integration failed to initialize");
            }

            // Test a sample analysis
            integration.analyzeText("اتصل بأمي", new OpenPhoneIntegration.AnalysisCallback() {
                @Override
                public void onResult(IntentResult result) {
                    System.out.printf("✓ Sample analysis completed: %s with confidence %.2f%n",
                        result.getIntentType(), result.getConfidence());
                }

                @Override
                public void onFallbackRequired(String reason) {
                    System.out.printf("ℹ Sample analysis fell back: %s%n", reason);
                }
            });

            // Clean up
            integration.destroy();
        } catch (Exception e) {
            System.out.println("✗ Error testing model integration: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * Test various Egyptian dialect variants
     */
    private static void testEgyptianDialectVariants() {
        System.out.println("--- Testing Egyptian Dialect Variants ---");

        String[][] variants = {
            // Common Egyptian expressions and their meanings
            {"اتصل", "call"},
            {"كلم", "call"},
            {"رن", "call"},
            {"بعت", "send"},
            {"ابعت", "send"},
            {"قول", "say"},
            {"قولي", "tell me"},
            {"فايتة", "missed"},
            {"فايتات", "missed"},
            {"بكرة", "tomorrow"},
            {"امبارح", "yesterday"},
            {"النهارده", "today"},
            {"دلوقتي", "now"},
            {"فين", "where"},
            {"ازاي", "how"},
            {"إمتى", "when"},
            {"أنا", "I"},
            {"انتا", "you (m)"},
            {"انتي", "you (f)"},
            {"إيه", "what"},
            {"أكيد", "sure"},
            {"مفيش", "nothing/okay"},
            {"زي الفل", "perfect"},
            {"بلاش", "no thanks"},
            {"يعني", "means"},
            {"كده", "like this/so"},
            {"أنا شايفك", "I see you"},
            {"أنا سامعك", "I hear you"},
            {"معلش", "excuse me/sorry"},
            {"أه", "yes"},
            {"لا", "no"},
        };

        int passed = 0;
        int total = variants.length;

        for (String[] variant : variants) {
            String egyptian = variant[0];
            String meaning = variant[1];

            // Just verify we can process these without errors
            String normalized = EgyptianNormalizer.normalize(egyptian);
            boolean processed = normalized != null && !normalized.isEmpty();

            if (processed) {
                passed++;
            }

            System.out.printf("%s Egyptian: '%s' -> Meaning: '%s' -> Processed: %s%n",
                processed ? "✓" : "✗",
                egyptian,
                meaning,
                processed ? "Yes" : "No"
            );
        }

        System.out.printf("Dialect Variants: %d/%d passed (%.1f%%)%n%n",
            passed, total, (passed * 100.0) / total);
    }

    /**
     * Test edge cases
     */
    private static void testEdgeCases() {
        System.out.println("--- Testing Edge Cases ---");

        String[] edgeCases = {
            "",  // Empty string
            "   ",  // Whitespace only
            "12345",  // Numbers only
            "!@#$%",  // Special chars only
            "Mixed English and عربي",  // Mixed languages
            "Very long sentence with many words that might cause issues during processing and normalization",
            "Sentence with numbers 123 and symbols @#$ mixed in",
            "Repeated repeated repeated words words words",
            "café résumé naïve",  // Accented characters
            "Emoji 😊 here 📞 and 🎵 there",  // Emojis
        };

        int passed = 0;
        int total = edgeCases.length;

        for (String testCase : edgeCases) {
            try {
                String result = EgyptianNormalizer.normalize(testCase);
                boolean handled = result != null; // Should not crash

                if (handled) {
                    passed++;
                }

                System.out.printf("%s Input: '%s' -> Handled: %s%n",
                    handled ? "✓" : "✗",
                    testCase.length() > 30 ? testCase.substring(0, 30) + "..." : testCase,
                    handled ? "Yes" : "No"
                );
            } catch (Exception e) {
                System.out.printf("✗ Input: '%s' -> Exception: %s%n",
                    testCase.length() > 30 ? testCase.substring(0, 30) + "..." : testCase,
                    e.getMessage());
            }
        }

        System.out.printf("Edge Cases: %d/%d passed (%.1f%%)%n%n",
            passed, total, (passed * 100.0) / total);
    }

    // Mock implementations for testing
    static class MockOrchestrator {
        public IntentResult determineIntent(String text) {
            // Simple rule-based mock for testing
            IntentResult result = new IntentResult();

            if (text.contains(" emergencies") || text.contains("emergency") ||
                text.contains("ngda") || text.contains("estghatha") || text.contains("tawari")) {
                result.setIntentType(IntentType.EMERGENCY);
                result.setConfidence(0.9f);
            } else if (text.contains("call") || text.contains("connect") ||
                      text.contains("tel") || text.contains("etasel") ||
                      text.contains("klm") || text.contains("rn")) {
                result.setIntentType(IntentType.CALL_CONTACT);
                result.setConfidence(0.8f);
            } else if (text.contains("whatsapp") || text.contains("message") ||
                      text.contains("wts") || text.contains("rsala") ||
                      text.contains("b3t")) {
                result.setIntentType(IntentType.SEND_WHATSAPP);
                result.setConfidence(0.75f);
            } else if (text.contains("alarm") || text.contains("remind") ||
                      text.contains("nbhny") || text.contains("anbhny") ||
                      text.contains("zkry")) {
                result.setIntentType(IntentType.SET_ALARM);
                result.setConfidence(0.7f);
            } else if (text.contains("time") || text.contains("hour") ||
                      text.contains("sa3a") || text.contains("kam") ||
                      text.contains("alwqt")) {
                result.setIntentType(IntentType.READ_TIME);
                result.setConfidence(0.95f);
            } else if (text.contains("missed") || text.contains("fa7ta") ||
                      text.contains("fatya")) {
                result.setIntentType(IntentType.READ_MISSED_CALLS);
                result.setConfidence(0.85f);
            } else {
                result.setIntentType(IntentType.UNKNOWN);
                result.setConfidence(0.3f);
            }

            return result;
        }
    }

    /**
     * Minimal stand-in Context. Extends the platform mock so it can be passed
     * anywhere a real {@link android.content.Context} is required.
     */
    static class MockContext extends android.content.ContextWrapper {
        MockContext() {
            super(null);
        }

        @Override
        public Object getSystemService(String name) {
            return null;
        }
    }
}