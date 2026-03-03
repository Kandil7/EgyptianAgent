package com.egyptian.agent.nlu;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

/**
 * Unit tests for TFLiteIntentClassifier
 * Tests ML-based intent classification fallback
 */
public class TFLiteIntentClassifierTest {
    
    private TFLiteIntentClassifier classifier;
    
    @Before
    public void setUp() {
        // Use null context - will use heuristic fallback
        classifier = new TFLiteIntentClassifier(null);
    }
    
    @Test
    public void testClassify_EmergencyIntent() {
        // Given: emergency phrase
        String text = "يا نجدة";
        
        // When
        TFLiteIntentClassifier.ClassificationResult result = classifier.classify(text);
        
        // Then
        assertNotNull(result);
        assertEquals(IntentType.EMERGENCY, result.intent);
        assertTrue("Emergency should have high confidence", result.confidence > 0.8);
    }
    
    @Test
    public void testClassify_CallIntent() {
        // Given: call command
        String text = "اتصل بماما";
        
        // When
        TFLiteIntentClassifier.ClassificationResult result = classifier.classify(text);
        
        // Then
        assertNotNull(result);
        assertEquals(IntentType.CALL_CONTACT, result.intent);
    }
    
    @Test
    public void testClassify_WhatsAppIntent() {
        // Given: WhatsApp message command
        String text = "ابعت واتساب لأحمد";
        
        // When
        TFLiteIntentClassifier.ClassificationResult result = classifier.classify(text);
        
        // Then
        assertNotNull(result);
        assertEquals(IntentType.SEND_WHATSAPP, result.intent);
    }
    
    @Test
    public void testClassify_AlarmIntent() {
        // Given: alarm command
        String text = "نبهني بكرة الصبح";
        
        // When
        TFLiteIntentClassifier.ClassificationResult result = classifier.classify(text);
        
        // Then
        assertNotNull(result);
        assertEquals(IntentType.SET_ALARM, result.intent);
    }
    
    @Test
    public void testClassify_WifiIntent() {
        // Given: WiFi toggle command
        String text = "افتح الواي فاي";
        
        // When
        TFLiteIntentClassifier.ClassificationResult result = classifier.classify(text);
        
        // Then
        assertNotNull(result);
        assertEquals(IntentType.TOGGLE_WIFI, result.intent);
    }
    
    @Test
    public void testClassify_BluetoothIntent() {
        // Given: Bluetooth toggle command
        String text = "قفل البلوتوث";
        
        // When
        TFLiteIntentClassifier.ClassificationResult result = classifier.classify(text);
        
        // Then
        assertNotNull(result);
        assertEquals(IntentType.TOGGLE_BLUETOOTH, result.intent);
    }
    
    @Test
    public void testClassify_TimeIntent() {
        // Given: time query
        String text = "الساعة كام";
        
        // When
        TFLiteIntentClassifier.ClassificationResult result = classifier.classify(text);
        
        // Then
        assertNotNull(result);
        assertEquals(IntentType.READ_TIME, result.intent);
    }
    
    @Test
    public void testClassify_GreetingIntent() {
        // Given: greeting
        String text = "صباح الخير";
        
        // When
        TFLiteIntentClassifier.ClassificationResult result = classifier.classify(text);
        
        // Then
        assertNotNull(result);
        assertEquals(IntentType.GREETING, result.intent);
    }
    
    @Test
    public void testClassify_ThankYouIntent() {
        // Given: thanks
        String text = "شكرا";
        
        // When
        TFLiteIntentClassifier.ClassificationResult result = classifier.classify(text);
        
        // Then
        assertNotNull(result);
        assertEquals(IntentType.THANK_YOU, result.intent);
    }
    
    @Test
    public void testClassify_EmptyInput() {
        // Given: empty input
        String text = "";
        
        // When
        TFLiteIntentClassifier.ClassificationResult result = classifier.classify(text);
        
        // Then
        assertNotNull(result);
        assertEquals(IntentType.UNKNOWN, result.intent);
        assertEquals(0.0f, result.confidence, 0.0f);
    }
    
    @Test
    public void testClassify_NullInput() {
        // Given: null input
        String text = null;
        
        // When
        TFLiteIntentClassifier.ClassificationResult result = classifier.classify(text);
        
        // Then
        assertNotNull(result);
        assertEquals(IntentType.UNKNOWN, result.intent);
    }
    
    @Test
    public void testClassify_UnknownIntent() {
        // Given: unrecognized phrase
        String text = "some random text that doesn't match any intent";
        
        // When
        TFLiteIntentClassifier.ClassificationResult result = classifier.classify(text);
        
        // Then
        assertNotNull(result);
        // Should return UNKNOWN or may match some pattern
        assertNotNull(result.intent);
    }
    
    @Test
    public void testIsLoaded() {
        // Then - model may not be loaded without assets
        // Should return false when model unavailable
        boolean loaded = classifier.isLoaded();
        
        // Without actual model file, should be false
        // But heuristic fallback should still work
        assertFalse("Model should not be loaded without assets", loaded);
    }
    
    @Test
    public void testMultipleEmergencyVariants() {
        // Test various emergency phrases
        String[] emergencyPhrases = {
            "يا نجدة",
            "استغاثة",
            "طوارئ",
            "مش قادر",
            "انقذني"
        };
        
        for (String phrase : emergencyPhrases) {
            TFLiteIntentClassifier.ClassificationResult result = classifier.classify(phrase);
            assertEquals(
                "Should recognize emergency: " + phrase,
                IntentType.EMERGENCY, 
                result.intent
            );
        }
    }
    
    @Test
    public void testMultipleCallVariants() {
        // Test various call command phrases
        String[] callPhrases = {
            "اتصل بماما",
            "كلم بابا",
            "رن على أحمد",
            "تصل بأخويا"
        };
        
        for (String phrase : callPhrases) {
            TFLiteIntentClassifier.ClassificationResult result = classifier.classify(phrase);
            assertEquals(
                "Should recognize call: " + phrase,
                IntentType.CALL_CONTACT, 
                result.intent
            );
        }
    }
}
