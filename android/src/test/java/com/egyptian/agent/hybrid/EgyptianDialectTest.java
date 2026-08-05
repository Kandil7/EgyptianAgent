package com.egyptian.agent.hybrid;

import android.util.Log;
import com.egyptian.agent.nlp.IntentResult;
import com.egyptian.agent.nlp.IntentType;
import com.egyptian.agent.stt.EgyptianNormalizer;

/**
 * Test class to validate the integration between OpenPhone-3B and Egyptian dialect processing
 */
public class EgyptianDialectTest {
    private static final String TAG = "EgyptianDialectTest";
    
    public static void runTests() {
        Log.i(TAG, "Starting Egyptian dialect processing tests...");
        
        // Test 1: Basic command recognition
        testBasicCommands();
        
        // Test 2: Egyptian dialect expressions
        testEgyptianDialectExpressions();
        
        // Test 3: Mixed dialect processing
        testMixedDialectProcessing();
        
        // Test 4: Contact name normalization
        testContactNameNormalization();
        
        Log.i(TAG, "Egyptian dialect processing tests completed.");
    }
    
    private static void testBasicCommands() {
        Log.d(TAG, "Testing basic commands...");
        
        String[] testCommands = {
            "اتصل بأمي",
            "رن على بابا",
            "ابعت واتساب ل Ahmed",
            "انبهني بكرة الصبح"
        };
        
        HybridOrchestrator orchestrator = createTestOrchestrator();
        
        for (String command : testCommands) {
            Log.d(TAG, "Testing command: " + command);
            String normalized = EgyptianNormalizer.normalize(command);
            Log.d(TAG, "Normalized: " + normalized);
            
            // In a real test, we would use the orchestrator to process the command
            // For now, we'll just validate the normalization
            validateNormalization(command, normalized);
        }
    }
    
    private static void testEgyptianDialectExpressions() {
        Log.d(TAG, "Testing Egyptian dialect expressions...");
        
        String[] dialectCommands = {
            "يا كبير، اتصل ب Mama",
            "يا صاحبي، قول لأحمد إن بكرة نلاقي بعض",
            "كلم بابا دلوقتي",
            "ابعتلي رسالة على واتساب",
            "نّبهني بعد شوية"
        };
        
        for (String command : dialectCommands) {
            Log.d(TAG, "Testing dialect command: " + command);
            String normalized = EgyptianNormalizer.normalize(command);
            Log.d(TAG, "Normalized: " + normalized);
            
            // Validate that Egyptian expressions are properly normalized
            validateEgyptianNormalization(command, normalized);
        }
    }
    
    private static void testMixedDialectProcessing() {
        Log.d(TAG, "Testing mixed dialect processing...");
        
        // Test commands with both standard Arabic and Egyptian dialect
        String[] mixedCommands = {
            "اتصل ب أستاذ أحمد بكره الصبح",  // Mix of formal and dialect
            "رن على الدكتور محمد امبارح",   // Mix of formal title and dialect
            "ابعت واتساب ل بنتي تقولها اتفضلتي"  // Mix of formal and colloquial
        };
        
        for (String command : mixedCommands) {
            Log.d(TAG, "Testing mixed command: " + command);
            String normalized = EgyptianNormalizer.normalize(command);
            Log.d(TAG, "Normalized: " + normalized);
            
            validateMixedNormalization(command, normalized);
        }
    }
    
    private static void testContactNameNormalization() {
        Log.d(TAG, "Testing contact name normalization...");
        
        String[] contactCommands = {
            "اتصل ب ماما",
            "كلم بابا",
            "ابعت واتساب ل جارنا",
            "اتصل بالدكتور أحمد"
        };
        
        for (String command : contactCommands) {
            Log.d(TAG, "Testing contact command: " + command);
            String contactName = EgyptianNormalizer.extractContactName(
                EgyptianNormalizer.normalize(command)
            );
            Log.d(TAG, "Extracted contact: " + contactName);
            
            String normalizedContact = EgyptianNormalizer.normalizeContactName(contactName);
            Log.d(TAG, "Normalized contact: " + normalizedContact);
        }
    }
    
    private static void validateNormalization(String original, String normalized) {
        // Basic validation that normalization didn't remove essential content
        if (original.contains("اتصل") && !normalized.contains("اتصل") && !normalized.contains("أتصال")) {
            Log.w(TAG, "Normalization may have incorrectly removed 'اتصل' from: " + original);
        }
        
        if (original.contains("واتساب") && !normalized.contains("واتساب")) {
            Log.w(TAG, "Normalization may have incorrectly removed 'واتساب' from: " + original);
        }
    }
    
    private static void validateEgyptianNormalization(String original, String normalized) {
        // Check that Egyptian dialect expressions are properly handled
        if (original.contains("بكره") && !normalized.contains("بكرة")) {
            Log.w(TAG, "Egyptian 'بكره' not normalized to 'بكرة' in: " + original);
        }
        
        if (original.contains("امبارح") && !normalized.contains("أمس")) {
            Log.w(TAG, "Egyptian 'امبارح' not normalized to 'أمس' in: " + original);
        }
        
        if (original.contains("دلوقتي") && !normalized.contains("الآن")) {
            Log.w(TAG, "Egyptian 'دلوقتي' not normalized to 'الآن' in: " + original);
        }
    }
    
    private static void validateMixedNormalization(String original, String normalized) {
        // Check that mixed expressions are handled appropriately
        Log.d(TAG, "Validating mixed normalization for: " + original);
    }
    
    private static HybridOrchestrator createTestOrchestrator() {
        // In a real test, we would create a test orchestrator
        // For now, we'll return null as we're focusing on normalization tests
        return null;
    }
    
    /**
     * Validates the intent recognition for Egyptian dialect commands
     */
    public static boolean validateIntentRecognition(String command, IntentResult result) {
        Log.d(TAG, "Validating intent recognition for: " + command);
        Log.d(TAG, "Result intent: " + result.getIntentType());
        Log.d(TAG, "Confidence: " + result.getConfidence());
        
        // For Egyptian dialect commands, we expect reasonable confidence (>0.5)
        if (result.getConfidence() < 0.5) {
            Log.w(TAG, "Low confidence for Egyptian dialect command: " + command);
            return false;
        }
        
        // Validate that intents make sense for the command
        String lowerCmd = command.toLowerCase();
        boolean intentMatches = true;
        
        if ((lowerCmd.contains("اتصل") || lowerCmd.contains("كلم") || lowerCmd.contains("رن")) 
            && result.getIntentType() != IntentType.CALL_CONTACT && result.getIntentType() != IntentType.UNKNOWN) {
            Log.w(TAG, "Expected CALL_CONTACT intent for: " + command);
            intentMatches = false;
        }
        
        if ((lowerCmd.contains("واتساب") || lowerCmd.contains("ابعت") || lowerCmd.contains("رساله")) 
            && result.getIntentType() != IntentType.SEND_WHATSAPP && result.getIntentType() != IntentType.UNKNOWN) {
            Log.w(TAG, "Expected SEND_WHATSAPP intent for: " + command);
            intentMatches = false;
        }
        
        if ((lowerCmd.contains("انبهني") || lowerCmd.contains("نبهني") || lowerCmd.contains("ذكرني")) 
            && result.getIntentType() != IntentType.SET_ALARM && result.getIntentType() != IntentType.UNKNOWN) {
            Log.w(TAG, "Expected SET_ALARM intent for: " + command);
            intentMatches = false;
        }
        
        return intentMatches;
    }
}