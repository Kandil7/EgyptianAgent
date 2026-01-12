package com.egyptian.agent.hybrid;

import android.content.Context;
import android.util.Log;
import com.egyptian.agent.nlp.IntentResult;
import com.egyptian.agent.nlp.IntentType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class for evaluating the accuracy of the Llama model with Egyptian dialect intents
 */
public class EgyptianDialectAccuracyTester {
    private static final String TAG = "EgyptianDialectTester";
    
    // Test data for Egyptian dialect commands
    private static final List<EgyptianTestCommand> TEST_COMMANDS = new ArrayList<>();
    
    static {
        // Call commands
        TEST_COMMANDS.add(new EgyptianTestCommand("اتصل بأمي", IntentType.CALL_CONTACT, "Call mother"));
        TEST_COMMANDS.add(new EgyptianTestCommand("كلم بابا", IntentType.CALL_CONTACT, "Call father"));
        TEST_COMMANDS.add(new EgyptianTestCommand("رن على ماما", IntentType.CALL_CONTACT, "Call mother"));
        TEST_COMMANDS.add(new EgyptianTestCommand("اتصل بـ أحمد", IntentType.CALL_CONTACT, "Call Ahmed"));
        
        // WhatsApp commands
        TEST_COMMANDS.add(new EgyptianTestCommand("ابعت واتساب لـ أمي", IntentType.SEND_WHATSAPP, "Send WhatsApp to mother"));
        TEST_COMMANDS.add(new EgyptianTestCommand("قول لأحمد إنني جاى", IntentType.SEND_WHATSAPP, "Tell Ahmed I'm coming"));
        
        // Alarm commands
        TEST_COMMANDS.add(new EgyptianTestCommand("نبهني بكرة الصبح", IntentType.SET_ALARM, "Set alarm for tomorrow morning"));
        TEST_COMMANDS.add(new EgyptianTestCommand("انبهني بعد ساعة", IntentType.SET_ALARM, "Set alarm for 1 hour from now"));
        
        // Time commands
        TEST_COMMANDS.add(new EgyptianTestCommand("الوقت كام؟", IntentType.READ_TIME, "What time is it?"));
        TEST_COMMANDS.add(new EgyptianTestCommand("قولي الساعة", IntentType.READ_TIME, "Tell the time"));
        
        // Emergency commands
        TEST_COMMANDS.add(new EgyptianTestCommand("يا نجدة", IntentType.EMERGENCY, "Emergency call"));
        TEST_COMMANDS.add(new EgyptianTestCommand("استغاثة", IntentType.EMERGENCY, "Distress call"));
        
        // Unknown commands
        TEST_COMMANDS.add(new EgyptianTestCommand(" playing music", IntentType.UNKNOWN, "Playing music (unknown intent)"));
        TEST_COMMANDS.add(new EgyptianTestCommand("weather today", IntentType.UNKNOWN, "Weather info (unknown intent)"));
    }
    
    private Context context;
    private OpenPhoneIntegration modelIntegration;
    
    public EgyptianDialectAccuracyTester(Context context) {
        this.context = context;
        this.modelIntegration = new OpenPhoneIntegration(context);
    }
    
    /**
     * Runs the accuracy test on the Egyptian dialect commands
     */
    public void runAccuracyTest() {
        Log.i(TAG, "Starting Egyptian dialect accuracy test...");
        
        int totalTests = TEST_COMMANDS.size();
        int correctPredictions = 0;
        int totalTokensProcessed = 0;
        
        for (int i = 0; i < TEST_COMMANDS.size(); i++) {
            EgyptianTestCommand testCmd = TEST_COMMANDS.get(i);
            
            Log.d(TAG, String.format("Testing (%d/%d): '%s'", i+1, totalTests, testCmd.command));
            
            // Test the command
            IntentResult result = testCommand(testCmd.command);
            
            boolean isCorrect = result.getIntentType() == testCmd.expectedIntent;
            if (isCorrect) {
                correctPredictions++;
                Log.d(TAG, String.format("✓ Correct prediction: %s -> %s", 
                    testCmd.command, result.getIntentType()));
            } else {
                Log.d(TAG, String.format("✗ Incorrect prediction: %s -> Expected: %s, Got: %s", 
                    testCmd.command, testCmd.expectedIntent, result.getIntentType()));
            }
            
            // Count tokens processed (approximate)
            totalTokensProcessed += testCmd.command.split("\\s+").length;
        }
        
        // Calculate and report results
        float accuracy = (float) correctPredictions / totalTests * 100;
        
        Log.i(TAG, "=== Egyptian Dialect Accuracy Test Results ===");
        Log.i(TAG, String.format("Total tests: %d", totalTests));
        Log.i(TAG, String.format("Correct predictions: %d", correctPredictions));
        Log.i(TAG, String.format("Accuracy: %.2f%%", accuracy));
        Log.i(TAG, String.format("Tokens processed: %d", totalTokensProcessed));
        
        // Additional metrics
        Log.i(TAG, String.format("Average tokens per command: %.2f", (float) totalTokensProcessed / totalTests));
        
        if (accuracy >= 90.0f) {
            Log.i(TAG, "✓ Model meets accuracy requirements for Egyptian dialect");
        } else {
            Log.w(TAG, "⚠ Model accuracy below threshold, consider fine-tuning");
        }
    }
    
    /**
     * Tests a single command and returns the model's prediction
     */
    private IntentResult testCommand(String command) {
        // Since the analyzeText method is asynchronous, we'll use a synchronous approach for testing
        // In a real scenario, you'd need to handle this asynchronously

        // In a real implementation, you'd need to use a latch or callback to wait for the async result
        // For now, we'll implement a synchronous version for testing purposes
        return analyzeCommandSync(command);
    }

    /**
     * Synchronous version of command analysis for testing purposes
     */
    private IntentResult analyzeCommandSync(String command) {
        // In a real implementation, this would properly wait for the async result
        try {
            // Create a CountDownLatch to wait for the async result
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            final IntentResult[] resultHolder = new IntentResult[1];

            // Call the async method and wait for completion
            modelIntegration.analyzeText(command, new OpenPhoneIntegration.AnalysisCallback() {
                @Override
                public void onSuccess(IntentResult result) {
                    resultHolder[0] = result;
                    latch.countDown();
                }

                @Override
                public void onError(Exception error) {
                    resultHolder[0] = new IntentResult();
                    resultHolder[0].setIntentType(com.egyptian.agent.nlp.IntentType.UNKNOWN);
                    resultHolder[0].setConfidence(0.0f);
                    latch.countDown();
                }
            });

            // Wait for the result with a timeout
            latch.await(10, java.util.concurrent.TimeUnit.SECONDS);

            if (resultHolder[0] != null) {
                return resultHolder[0];
            } else {
                // If timeout occurred, return a default result
                IntentResult result = new IntentResult();
                result.setIntentType(com.egyptian.agent.nlp.IntentType.UNKNOWN);
                result.setConfidence(0.0f);
                return result;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in synchronous command analysis", e);
            IntentResult result = new IntentResult();
            result.setIntentType(com.egyptian.agent.nlp.IntentType.UNKNOWN);
            result.setConfidence(0.0f);
            return result;
        }
    }
    
    /**
     * Mock implementation to simulate command analysis
     * In a real implementation, this would wait for the async result
     */
    private IntentResult mockAnalyzeCommand(String command) {
        IntentResult result = new IntentResult();

        // Simple keyword matching for mock purposes
        String lowerCmd = command.toLowerCase();

        if (lowerCmd.contains("اتصل") || lowerCmd.contains("كلم") || lowerCmd.contains("رن") ||
            lowerCmd.contains("call") || lowerCmd.contains("connect")) {
            result.setIntentType(IntentType.CALL_CONTACT);
        } else if (lowerCmd.contains("واتساب") || lowerCmd.contains("رسالة") ||
                   lowerCmd.contains("whats") || lowerCmd.contains("message")) {
            result.setIntentType(IntentType.SEND_WHATSAPP);
        } else if (lowerCmd.contains("نبه") || lowerCmd.contains("انبه") ||
                   lowerCmd.contains("alarm") || lowerCmd.contains("remind")) {
            result.setIntentType(IntentType.SET_ALARM);
        } else if (lowerCmd.contains("وقت") || lowerCmd.contains("ساعة") ||
                   lowerCmd.contains("time") || lowerCmd.contains("hour")) {
            result.setIntentType(IntentType.READ_TIME);
        } else if (lowerCmd.contains("نجدة") || lowerCmd.contains("استغاثة") ||
                   lowerCmd.contains("emergency") || lowerCmd.contains("help")) {
            result.setIntentType(IntentType.EMERGENCY);
        } else {
            result.setIntentType(IntentType.UNKNOWN);
        }

        // Set a confidence based on how strong the match is
        result.setConfidence(determineMockConfidence(command, result.getIntentType()));

        return result;
    }
    
    /**
     * Determines a mock confidence score based on the command and predicted intent
     */
    private float determineMockConfidence(String command, IntentType predictedIntent) {
        // Higher confidence for commands with clear intent indicators
        String lowerCmd = command.toLowerCase();
        float confidence = 0.5f; // Base confidence
        
        // Boost confidence based on specific keywords
        switch (predictedIntent) {
            case CALL_CONTACT:
                if (lowerCmd.contains("امي") || lowerCmd.contains("ابي") || lowerCmd.contains("ماما") || 
                    lowerCmd.contains("بابا") || lowerCmd.contains("mother") || lowerCmd.contains("father")) {
                    confidence += 0.3f;
                }
                break;
            case SEND_WHATSAPP:
                if (lowerCmd.contains("واتساب") || lowerCmd.contains("whatsapp")) {
                    confidence += 0.3f;
                }
                break;
            case SET_ALARM:
                if (lowerCmd.contains("بكرة") || lowerCmd.contains("بعد") || 
                    lowerCmd.contains("tomorrow") || lowerCmd.contains("hour")) {
                    confidence += 0.2f;
                }
                break;
            case EMERGENCY:
                if (lowerCmd.contains("نجدة") || lowerCmd.contains("استغاثة") || 
                    lowerCmd.contains("emergency") || lowerCmd.contains("help")) {
                    confidence += 0.4f;
                }
                break;
            case READ_TIME:
                if (lowerCmd.contains("وقت") || lowerCmd.contains("ساعة") || 
                    lowerCmd.contains("time") || lowerCmd.contains("hour")) {
                    confidence += 0.3f;
                }
                break;
            default:
                // For unknown, keep base confidence
                break;
        }
        
        // Cap at 0.95
        return Math.min(confidence, 0.95f);
    }
    
    /**
     * Represents a test command with expected outcome
     */
    private static class EgyptianTestCommand {
        public final String command;
        public final IntentType expectedIntent;
        public final String description;
        
        public EgyptianTestCommand(String command, IntentType expectedIntent, String description) {
            this.command = command;
            this.expectedIntent = expectedIntent;
            this.description = description;
        }
    }
    
    /**
     * Cleans up resources after testing
     */
    public void cleanup() {
        if (modelIntegration != null) {
            modelIntegration.destroy();
        }
    }
}