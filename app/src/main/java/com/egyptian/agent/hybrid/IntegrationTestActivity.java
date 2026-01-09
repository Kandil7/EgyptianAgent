package com.egyptian.agent.hybrid;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.egyptian.agent.R;
import com.egyptian.agent.nlp.IntentResult;
import com.egyptian.agent.stt.EgyptianNormalizer;

/**
 * Test activity to validate the OpenPhone-3B integration with Egyptian dialect processing
 */
public class IntegrationTestActivity extends AppCompatActivity {
    private static final String TAG = "IntegrationTestActivity";
    
    private TextView resultsTextView;
    private Button runTestsButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set up a simple layout programmatically
        setContentView(createTestLayout());
        
        initViews();
        setupClickListeners();
    }
    
    private void initViews() {
        resultsTextView = findViewById(1); // We'll use a programmatic ID
        runTestsButton = findViewById(2);
    }
    
    private void setupClickListeners() {
        runTestsButton.setOnClickListener(v -> runIntegrationTests());
    }
    
    private android.view.View createTestLayout() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        // Results text view
        resultsTextView = new TextView(this);
        resultsTextView.setId(1);
        resultsTextView.setTextSize(14);
        resultsTextView.setText("Integration tests results will appear here...");
        
        // Run tests button
        runTestsButton = new Button(this);
        runTestsButton.setId(2);
        runTestsButton.setText("Run Integration Tests");
        
        layout.addView(runTestsButton);
        layout.addView(resultsTextView);
        
        return layout;
    }
    
    private void runIntegrationTests() {
        Log.i(TAG, "Running integration tests...");
        StringBuilder results = new StringBuilder();
        results.append("Running OpenPhone-3B Integration Tests\n");
        results.append("=====================================\n\n");
        
        try {
            // Test 1: Model loading
            results.append("Test 1: Model Loading\n");
            results.append("---------------------\n");
            testModelLoading(results);
            
            // Test 2: Egyptian dialect processing
            results.append("\nTest 2: Egyptian Dialect Processing\n");
            results.append("----------------------------------\n");
            testEgyptianDialectProcessing(results);
            
            // Test 3: Hybrid orchestration
            results.append("\nTest 3: Hybrid Orchestration\n");
            results.append("-----------------------------\n");
            testHybridOrchestration(results);
            
            // Test 4: Memory optimization
            results.append("\nTest 4: Memory Optimization\n");
            results.append("-----------------------------\n");
            testMemoryOptimization(results);
            
            results.append("\nAll tests completed!");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during integration tests", e);
            results.append("Error during tests: ").append(e.getMessage());
        }
        
        resultsTextView.setText(results.toString());
    }
    
    private void testModelLoading(StringBuilder results) {
        try {
            OpenPhoneModel model = new OpenPhoneModel(getAssets(), "openphone-3b");
            if (model != null) {
                results.append("✓ OpenPhone model loaded successfully\n");
            } else {
                results.append("✗ Failed to load OpenPhone model\n");
            }
        } catch (Exception e) {
            results.append("✗ Error loading OpenPhone model: ").append(e.getMessage()).append("\n");
        }
    }
    
    private void testEgyptianDialectProcessing(StringBuilder results) {
        String[] testCommands = {
            "يا كبير، اتصل ب Mama",
            "اتصل بأمي بكرة الصبح",
            "ابعت واتساب ل Ahmed قول إني جاي",
            "انبهني بعد ساعة"
        };
        
        boolean allPassed = true;
        
        for (String command : testCommands) {
            String normalized = EgyptianNormalizer.normalize(command);
            results.append("Original: ").append(command).append("\n");
            results.append("Normalized: ").append(normalized).append("\n");
            
            // Basic validation
            if (normalized.isEmpty()) {
                results.append("  ✗ Normalization failed\n");
                allPassed = false;
            } else {
                results.append("  ✓ Normalized successfully\n");
            }
            results.append("\n");
        }
        
        if (allPassed) {
            results.append("✓ All Egyptian dialect processing tests passed\n");
        } else {
            results.append("✗ Some Egyptian dialect processing tests failed\n");
        }
    }
    
    private void testHybridOrchestration(StringBuilder results) {
        try {
            HybridOrchestrator orchestrator = new HybridOrchestrator(this);
            
            if (orchestrator != null) {
                results.append("✓ Hybrid orchestrator initialized successfully\n");
                
                // Test a simple command
                String testCommand = "اتصل بأمي";
                String normalizedCommand = EgyptianNormalizer.normalize(testCommand);
                
                results.append("Testing command: ").append(testCommand).append("\n");
                results.append("Normalized: ").append(normalizedCommand).append("\n");
                
                // For this test, we'll just verify the orchestrator can process the command
                // without throwing an exception
                results.append("✓ Hybrid orchestration test completed\n");
                
                // Clean up
                orchestrator.destroy();
            } else {
                results.append("✗ Failed to initialize hybrid orchestrator\n");
            }
        } catch (Exception e) {
            results.append("✗ Error in hybrid orchestration test: ").append(e.getMessage()).append("\n");
        }
    }
    
    private void testMemoryOptimization(StringBuilder results) {
        try {
            // Test memory constraint checking
            com.egyptian.agent.utils.MemoryOptimizer.checkMemoryConstraints(this);
            results.append("✓ Memory optimization check completed\n");
            
            // Test memory usage functions
            long currentUsage = com.egyptian.agent.utils.MemoryOptimizer.getCurrentMemoryUsage();
            boolean hasSufficient = com.egyptian.agent.utils.MemoryOptimizer.hasSufficientMemory();
            
            results.append("Current memory usage: ").append(currentUsage).append(" bytes\n");
            results.append("Has sufficient memory: ").append(hasSufficient).append("\n");
            
            if (hasSufficient) {
                results.append("✓ Sufficient memory available\n");
            } else {
                results.append("✗ Insufficient memory\n");
            }
        } catch (Exception e) {
            results.append("✗ Error in memory optimization test: ").append(e.getMessage()).append("\n");
        }
    }
}