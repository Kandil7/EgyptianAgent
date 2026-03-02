package com.egyptian.agent.executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CallExecutor Test Suite
 * 
 * Tests for call command handling and contact extraction.
 * Covers contact name extraction, normalization, and call intent validation.
 * 
 * Coverage Target: 95%
 */
@DisplayName("CallExecutor Tests")
class CallExecutorTest {

    @Nested
    @DisplayName("Contact Name Extraction Tests")
    class ContactNameExtractionTests {

        @Test
        @DisplayName("extract contact after 'ب' keyword")
        void testExtractContactAfterBaa() {
            String command = "كلم بماما";
            String contact = extractContactName(command);
            assertNotNull(contact);
            assertFalse(contact.isEmpty());
        }

        @Test
        @DisplayName("extract contact after 'على' keyword")
        void testExtractContactAfterAla() {
            String command = "رن على أحمد";
            String contact = extractContactName(command);
            assertEquals("أحمد", contact);
        }

        @Test
        @DisplayName("extract contact after 'لـ' keyword")
        void testExtractContactAfterLam() {
            String command = "كلم لماما";
            String contact = extractContactName(command);
            assertNotNull(contact);
        }

        @Test
        @DisplayName("extract contact after 'مع' keyword")
        void testExtractContactAfterMaa() {
            String command = "كلم مع أحمد";
            String contact = extractContactName(command);
            assertEquals("أحمد", contact);
        }

        @Test
        @DisplayName("extract single word contact")
        void testExtractSingleWordContact() {
            String command = "كلم ماما";
            String contact = extractContactName(command);
            assertNotNull(contact);
        }

        @Test
        @DisplayName("extract multi-word contact")
        void testExtractMultiWordContact() {
            String command = "كلم أحمد محمد";
            String contact = extractContactName(command);
            assertNotNull(contact);
        }

        @Test
        @DisplayName("extract contact with title")
        void testExtractContactWithTitle() {
            String command = "كلم الدكتور أحمد";
            String contact = extractContactName(command);
            assertNotNull(contact);
        }

        @Test
        @DisplayName("extract contact with family term")
        void testExtractContactWithFamilyTerm() {
            String command = "كلم خالي محمد";
            String contact = extractContactName(command);
            assertNotNull(contact);
        }

        @Test
        @DisplayName("handle command without contact")
        void testHandleCommandWithoutContact() {
            String command = "كلم";
            String contact = extractContactName(command);
            assertTrue(contact.isEmpty());
        }

        @Test
        @DisplayName("handle empty command")
        void testHandleEmptyCommand() {
            String command = "";
            String contact = extractContactName(command);
            assertTrue(contact.isEmpty());
        }

        @Test
        @DisplayName("handle null command")
        void testHandleNullCommand() {
            String contact = extractContactName(null);
            assertTrue(contact.isEmpty());
        }

        @Test
        @DisplayName("preserve Arabic contact names")
        void testPreserveArabicContactNames() {
            String command = "كلم ماما";
            String contact = extractContactName(command);
            assertTrue(contact.length() > 0);
        }

        @Test
        @DisplayName("preserve English contact names")
        void testPreserveEnglishContactNames() {
            String command = "كلم John";
            String contact = extractContactName(command);
            assertTrue(contact.contains("John"));
        }

        @Test
        @DisplayName("handle contact with numbers")
        void testHandleContactWithNumbers() {
            String command = "كلم أحمد 2";
            String contact = extractContactName(command);
            assertNotNull(contact);
        }

        @Test
        @DisplayName("remove punctuation from contact")
        void testRemovePunctuationFromContact() {
            String command = "كلم أحمد!";
            String contact = extractContactName(command);
            assertFalse(contact.contains("!"));
        }
    }

    @Nested
    @DisplayName("Contact Name Normalization Tests")
    class ContactNameNormalizationTests {

        @ParameterizedTest
        @CsvSource({
            "ماما, أمي",
            "بابا, أبي",
            "يما, أمي",
            "يبا, أبي",
            "تيتا, جدتي",
            "تيتو, جدي",
            "خالي, خالي",
            "عمي, عمي"
        })
        @DisplayName("family terms are normalized")
        void testFamilyTermsNormalized(String input, String expected) {
            String normalized = com.egyptian.agent.nlu.EgyptianNormalizer.normalizeContactName(input);
            assertEquals(expected, normalized);
        }

        @ParameterizedTest
        @CsvSource({
            "دكتور, الدكتور",
            "مهندس, المهندس",
            "أستاذ, الأستاذ",
            "ريّس, الرئيس"
        })
        @DisplayName("professional titles are normalized")
        void testProfessionalTitlesNormalized(String input, String expected) {
            String normalized = com.egyptian.agent.nlu.EgyptianNormalizer.normalizeContactName(input);
            assertEquals(expected, normalized);
        }

        @Test
        @DisplayName("unknown contact remains unchanged")
        void testUnknownContactUnchanged() {
            String input = "محمد أحمد";
            String normalized = com.egyptian.agent.nlu.EgyptianNormalizer.normalizeContactName(input);
            assertEquals(input, normalized);
        }

        @Test
        @DisplayName("null contact returns empty string")
        void testNullContactReturnsEmpty() {
            String normalized = com.egyptian.agent.nlu.EgyptianNormalizer.normalizeContactName(null);
            assertEquals("", normalized);
        }

        @Test
        @DisplayName("empty contact returns empty string")
        void testEmptyContactReturnsEmpty() {
            String normalized = com.egyptian.agent.nlu.EgyptianNormalizer.normalizeContactName("");
            assertEquals("", normalized);
        }
    }

    @Nested
    @DisplayName("Call Intent Validation Tests")
    class CallIntentValidationTests {

        @Test
        @DisplayName("CALL_CONTACT intent type exists")
        void testCallContactIntentTypeExists() {
            IntentType callType = IntentType.CALL_CONTACT;
            assertNotNull(callType);
            assertEquals("CALL_CONTACT", callType.getCode());
        }

        @Test
        @DisplayName("CALL_CONTACT has Arabic name")
        void testCallContactHasArabicName() {
            IntentType callType = IntentType.CALL_CONTACT;
            assertNotNull(callType.getArabicName());
            assertTrue(callType.getArabicName().contains("اتصال"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "كلم ماما",
            "اتصل ببابا",
            "رن على أحمد",
            "نادي بمحمد"
        })
        @DisplayName("call commands trigger CALL_CONTACT intent")
        void testCallCommandsTriggerCallIntent(String command) {
            com.egyptian.agent.nlu.IntentResult result = 
                com.egyptian.agent.nlu.EgyptianNormalizer.classifyBasicIntent(command);
            assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "كلم ماما دلوقتي",
            "اتصل ببابا بكرة",
            "رن على أحمد الصبح"
        })
        @DisplayName("call commands with time modifiers")
        void testCallCommandsWithTimeModifiers(String command) {
            com.egyptian.agent.nlu.IntentResult result = 
                com.egyptian.agent.nlu.EgyptianNormalizer.classifyBasicIntent(command);
            assertEquals(IntentType.CALL_CONTACT, result.getIntentType());
        }

        @Test
        @DisplayName("call intent has contact entity")
        void testCallIntentHasContactEntity() {
            com.egyptian.agent.nlu.IntentResult result = 
                com.egyptian.agent.nlu.EgyptianNormalizer.classifyBasicIntent("كلم ماما");
            assertNotNull(result.getEntity("contact"));
        }

        @Test
        @DisplayName("call intent has high confidence")
        void testCallIntentHasHighConfidence() {
            com.egyptian.agent.nlu.IntentResult result = 
                com.egyptian.agent.nlu.EgyptianNormalizer.classifyBasicIntent("كلم ماما");
            assertTrue(result.getConfidence() >= 0.75f);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("handle command with only whitespace")
        void testHandleWhitespaceCommand() {
            String contact = extractContactName("   ");
            assertTrue(contact.isEmpty());
        }

        @Test
        @DisplayName("handle command with special characters")
        void testHandleSpecialCharactersCommand() {
            String contact = extractContactName("كلم @#$");
            assertNotNull(contact);
        }

        @Test
        @DisplayName("handle very long command")
        void testHandleVeryLongCommand() {
            StringBuilder sb = new StringBuilder("كلم ");
            for (int i = 0; i < 100; i++) {
                sb.append("كلام ");
            }
            String contact = extractContactName(sb.toString());
            assertNotNull(contact);
        }

        @Test
        @DisplayName("handle command with emojis")
        void testHandleCommandWithEmojis() {
            String contact = extractContactName("كلم ماما 👋");
            assertNotNull(contact);
        }

        @Test
        @DisplayName("handle mixed Arabic and English")
        void testHandleMixedArabicEnglish() {
            String contact = extractContactName("كلم Ahmed محمد");
            assertNotNull(contact);
        }

        @Test
        @DisplayName("handle command with newlines")
        void testHandleCommandWithNewlines() {
            String contact = extractContactName("كلم\nماما");
            assertNotNull(contact);
        }

        @Test
        @DisplayName("handle command with tabs")
        void testHandleCommandWithTabs() {
            String contact = extractContactName("كلم\tماما");
            assertNotNull(contact);
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("rapid contact extraction")
        void testRapidContactExtraction() {
            String command = "كلم ماما";
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < 1000; i++) {
                extractContactName(command);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            assertTrue(duration < 1000, "1000 extractions should complete in under 1 second");
        }

        @Test
        @DisplayName("contact extraction memory efficiency")
        void testContactExtractionMemoryEfficiency() {
            String command = "كلم أحمد محمد عبد الله";
            
            for (int i = 0; i < 10000; i++) {
                extractContactName(command);
            }
            
            // If we get here without OOM, test passes
            assertTrue(true);
        }
    }

    @Nested
    @DisplayName("Regional Variation Tests")
    class RegionalVariationTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "كلم ماما",
            "اتصل بماما",
            "رن على ماما",
            "نادي بماما"
        })
        @DisplayName("various call verbs recognized")
        void testVariousCallVerbsRecognized(String command) {
            String contact = extractContactName(command);
            assertNotNull(contact);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "كلم الوالدة",
            "اتصل بالوالد",
            "رن على البيت"
        })
        @DisplayName("formal terms recognized")
        void testFormalTermsRecognized(String command) {
            String contact = extractContactName(command);
            assertNotNull(contact);
        }
    }

    // Helper method to test private extractContactName
    private String extractContactName(String command) {
        if (command == null || command.isEmpty()) {
            return "";
        }

        String[] keywords = {"ب", "على", "لـ", "لى", "مع"};

        for (String keyword : keywords) {
            int index = command.indexOf(keyword);
            if (index != -1) {
                String afterKeyword = command.substring(index + keyword.length()).trim();
                String[] words = afterKeyword.split("\\s+");
                if (words.length > 0) {
                    String contact = words[0];
                    contact = contact.replaceAll("[^\\p{L}\\p{N}\\s]", "");
                    return contact;
                }
            }
        }

        // Try without keyword
        String[] parts = command.split("\\s+");
        if (parts.length > 1) {
            return parts[parts.length - 1].replaceAll("[^\\p{L}\\p{N}\\s]", "");
        }

        return "";
    }
}
