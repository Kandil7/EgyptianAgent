package com.egyptian.agent.hybrid;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.egyptian.agent.R;
import com.egyptian.agent.nlp.IntentResult;
import java.util.concurrent.CountDownLatch;

/**
 * Test activity for evaluating the Llama model with Egyptian dialect commands
 */
public class LlamaModelTesterActivity extends Activity {
    private static final String TAG = "LlamaModelTester";
    
    private TextView resultsTextView;
    private StringBuilder testResults;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize UI
        resultsTextView = new TextView(this);
        setContentView(resultsTextView);
        
        testResults = new StringBuilder();
        
        // Run the tests
        runTests();
    }
    
    private void runTests() {
        logResult("Starting Llama 3.2 3B Egyptian Dialect Tests...\n");
        
        // Test individual commands
        testCommand("اتصل بأمي");
        testCommand("كلم بابا");
        testCommand("ابعت واتساب لامي");
        testCommand("رن على ماما");
        testCommand("نبهني بكرة الصبح");
        testCommand("قولي الوقت");
        testCommand("يا نجدة");
        testCommand("استغاثة");
        
        // Test more complex commands
        testCommand("قول لأحمد إنني جاى بعد شوية");
        testCommand("اتصل بطبيب الأسنان وقوله إن عندي موعد بكرة");
        
        logResult("\nTests completed!");
        resultsTextView.setText(testResults.toString());
    }
    
    private void testCommand(String command) {
        logResult("\nTesting command: \"" + command + "\"");
        
        OpenPhoneIntegration modelIntegration = new OpenPhoneIntegration(this);
        
        // Wait for model to be ready
        waitForModelReady(modelIntegration);
        
        // Create a latch to wait for async result
        CountDownLatch latch = new CountDownLatch(1);
        IntentResult[] resultHolder = new IntentResult[1];
        
        modelIntegration.analyzeText(command, new OpenPhoneIntegration.AnalysisCallback() {
            @Override
            public void onResult(IntentResult result) {
                resultHolder[0] = result;
                logResult("  Intent: " + result.getIntentType());
                logResult("  Confidence: " + result.getConfidence());
                
                // Print entities if any
                if (result.getEntities() != null && !result.getEntities().isEmpty()) {
                    logResult("  Entities: " + result.getEntities().toString());
                }
                
                latch.countDown();
            }

            @Override
            public void onFallbackRequired(String reason) {
                logResult("  Fallback required: " + reason);
                latch.countDown();
            }
        });
        
        try {
            latch.await(); // Wait for the async operation to complete
        } catch (InterruptedException e) {
            Log.e(TAG, "Test interrupted", e);
        }
        
        modelIntegration.destroy();
    }
    
    private void waitForModelReady(OpenPhoneIntegration modelIntegration) {
        int attempts = 0;
        final int maxAttempts = 30; // Wait up to 30 seconds
        
        while (!modelIntegration.isReady() && attempts < maxAttempts) {
            try {
                Thread.sleep(1000); // Wait 1 second
                attempts++;
                
                if (attempts % 5 == 0) {
                    logResult("  Waiting for model to load... (" + attempts + "s)");
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Wait interrupted", e);
                break;
            }
        }
        
        if (!modelIntegration.isReady()) {
            logResult("  ERROR: Model failed to load within timeout period");
        } else {
            logResult("  Model loaded successfully");
        }
    }
    
    private void logResult(String message) {
        Log.d(TAG, message);
        testResults.append(message).append("\n");
    }
}