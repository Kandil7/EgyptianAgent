package com.egyptian.agent.testing;

import android.content.Context;
import android.util.Log;
import com.egyptian.agent.core.Quantum;
import com.egyptian.agent.core.OfflineGrammarProcessor;
import com.egyptian.agent.core.ContextMemory;
import com.egyptian.agent.nlp.IntentResult;
import com.egyptian.agent.nlp.IntentType;
import com.egyptian.agent.stt.EgyptianNormalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AutomatedTestSuite - Comprehensive testing framework for Egyptian Agent
 * Tests all major components and functionality
 */
public class AutomatedTestSuite {
    private static final String TAG = "AutomatedTestSuite";
    
    private Context context;
    private Quantum quantum;
    private OfflineGrammarProcessor grammarProcessor;
    private ContextMemory contextMemory;
    private List<TestResult> testResults;
    
    public AutomatedTestSuite(Context context) {
        this.context = context;
        this.quantum = new Quantum(context);
        this.grammarProcessor = new OfflineGrammarProcessor(context);
        this.contextMemory = ContextMemory.getInstance(context);
        this.testResults = new ArrayList<>();
    }
    
    /**
     * Runs all tests in the suite
     */
    public TestReport runAllTests() {
        Log.i(TAG, "Starting automated test suite...");
        
        // Reset context memory for clean test run
        contextMemory.clearAll();
        
        // Run all test categories
        testIntentClassification();
        testContextMemory();
        testOfflineGrammarProcessing();
        testEgyptianNormalization();
        testCommandProcessing();
        testQuantumIntegration();
        
        // Generate and return report
        TestReport report = generateReport();
        Log.i(TAG, "Automated test suite completed. " + testResults.size() + " tests run.");
        
        return report;
    }
    
    /**
     * Tests intent classification functionality
     */
    private void testIntentClassification() {
        Log.i(TAG, "Running intent classification tests...");
        
        String[] testCommands = {
            "اتصل بماما",
            "ابعت رسالة لجدو",
            "شغّل قرآن",
            "علّي الصوت",
            "افتح واتساب",
            "قولي المكالمات الفايتة",
            "يا نجدة",
            "ابعت فويس لأخويا"
        };
        
        String[] expectedIntents = {
            "CALL_PERSON",
            "SEND_MSG",
            "PLAY_QURAN",
            "VOLUME_CONTROL",
            "OPEN_APP",
            "UNKNOWN",  // This might not be implemented in basic classification
            "EMERGENCY", // This might be handled by emergency detection
            "SEND_VOICE"
        };
        
        for (int i = 0; i < testCommands.length; i++) {
            String command = testCommands[i];
            String expected = expectedIntents[i];
            
            String actual = quantum.classifyIntent(command);
            boolean passed = actual.equals(expected);
            
            addTestResult("Intent Classification: " + command, passed, 
                         "Expected: " + expected + ", Actual: " + actual);
        }
    }
    
    /**
     * Tests context memory functionality
     */
    private void testContextMemory() {
        Log.i(TAG, "Running context memory tests...");
        
        // Test setting and getting values
        contextMemory.setLastContact("أمي");
        String lastContact = contextMemory.getLastContact();
        addTestResult("Context Memory - Set/Get Last Contact", 
                     "أمي".equals(lastContact), 
                     "Expected: أمي, Actual: " + lastContact);
        
        // Test context update
        contextMemory.updateContext("call_person", "contact", "بابا");
        String updatedContact = contextMemory.getLastContact();
        addTestResult("Context Memory - Update Context", 
                     "بابا".equals(updatedContact), 
                     "Expected: بابا, Actual: " + updatedContact);
        
        // Test last app
        contextMemory.setLastApp("واتساب");
        String lastApp = contextMemory.getLastApp();
        addTestResult("Context Memory - Set/Get Last App", 
                     "واتساب".equals(lastApp), 
                     "Expected: واتساب, Actual: " + lastApp);
    }
    
    /**
     * Tests offline grammar processing
     */
    private void testOfflineGrammarProcessing() {
        Log.i(TAG, "Running offline grammar processing tests...");
        
        String[] testCommands = {
            "اتصل بماما",
            "ابعت واتساب لجدو",
            "شغّل قرآن كريم",
            "علّي الصوت شوية",
            "افتح التليفون",
            "إيه الوقت دلوقتي"
        };
        
        String[] expectedIntents = {
            "CALL_PERSON",  // This would be detected by grammar rules
            "SEND_MSG",     // This would be detected by grammar rules
            "PLAY_QURAN",   // This would be detected by grammar rules
            "VOLUME_CONTROL", // This would be detected by grammar rules
            "OPEN_APP",     // This would be detected by grammar rules
            "READ_TIME"     // This would be detected by grammar rules
        };
        
        for (int i = 0; i < testCommands.length; i++) {
            String command = testCommands[i];
            String expected = expectedIntents[i];
            
            String actual = grammarProcessor.processCommand(command);
            boolean passed = actual.equals(expected);
            
            addTestResult("Grammar Processing: " + command, passed, 
                         "Expected: " + expected + ", Actual: " + actual);
        }
    }
    
    /**
     * Tests Egyptian dialect normalization
     */
    private void testEgyptianNormalization() {
        Log.i(TAG, "Running Egyptian normalization tests...");
        
        String[] testInputs = {
            "عايز أكلم ماما",
            "فايتة عليا",
            "رن على بابا",
            "ابعتلي رسالة"
        };
        
        String[] expectedOutputs = {
            "أريد أن أتصل أمي",  // This is what the normalizer should produce
            "فاتت",             // This is what the normalizer should produce
            "اتصل على أب",      // This is what the normalizer should produce
            "أرسل رسالة"        // This is what the normalizer should produce
        };
        
        for (int i = 0; i < testInputs.length; i++) {
            String input = testInputs[i];
            String expected = expectedOutputs[i];
            
            String actual = EgyptianNormalizer.normalize(input);
            boolean passed = actual.contains(expected) || actual.equals(expected);
            
            addTestResult("Egyptian Normalization: " + input, passed, 
                         "Expected: " + expected + ", Actual: " + actual);
        }
    }
    
    /**
     * Tests command processing
     */
    private void testCommandProcessing() {
        Log.i(TAG, "Running command processing tests...");
        
        // Test that command processing doesn't crash
        String[] testCommands = {
            "اتصل بماما",
            "ابعت فويس لجدو",
            "شغّل أغاني محمد منير",
            "هدّي الصوت",
            "قول لتيتا إني جايلك بكرة"
        };
        
        for (String command : testCommands) {
            try {
                quantum.processCommand(command);
                addTestResult("Command Processing: " + command, true, 
                             "Command processed without error: " + command);
            } catch (Exception e) {
                addTestResult("Command Processing: " + command, false, 
                             "Error processing command: " + e.getMessage());
            }
        }
    }
    
    /**
     * Tests integration between components
     */
    private void testQuantumIntegration() {
        Log.i(TAG, "Running quantum integration tests...");
        
        // Test context memory integration
        contextMemory.setLastContact("أمي");
        String lastContact = quantum.getLastContact();
        addTestResult("Quantum-ContextMemory Integration", 
                     "أمي".equals(lastContact), 
                     "Expected: أمي, Actual: " + lastContact);
        
        // Test that quantum can process commands and update context
        quantum.processCommand("اتصل بجدو");
        String updatedContact = contextMemory.getLastContact();
        addTestResult("Quantum Context Update", 
                     "جدو".equals(updatedContact), 
                     "Expected: جدو, Actual: " + updatedContact);
    }
    
    /**
     * Adds a test result to the collection
     */
    private void addTestResult(String testName, boolean passed, String details) {
        TestResult result = new TestResult(testName, passed, details);
        testResults.add(result);
        
        if (passed) {
            Log.i(TAG, "PASS: " + testName);
        } else {
            Log.e(TAG, "FAIL: " + testName + " - " + details);
        }
    }
    
    /**
     * Generates a test report
     */
    private TestReport generateReport() {
        int totalTests = testResults.size();
        long passedTests = testResults.stream().filter(TestResult::isPassed).count();
        long failedTests = totalTests - passedTests;
        
        Map<String, Integer> categoryResults = new HashMap<>();
        // In a real implementation, we would categorize tests and count by category
        
        return new TestReport(totalTests, (int) passedTests, (int) failedTests, testResults, categoryResults);
    }
    
    /**
     * Represents a single test result
     */
    public static class TestResult {
        private String testName;
        private boolean passed;
        private String details;
        private long timestamp;
        
        public TestResult(String testName, boolean passed, String details) {
            this.testName = testName;
            this.passed = passed;
            this.details = details;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters
        public String getTestName() { return testName; }
        public boolean isPassed() { return passed; }
        public String getDetails() { return details; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Represents the overall test report
     */
    public static class TestReport {
        private int totalTests;
        private int passedTests;
        private int failedTests;
        private List<TestResult> results;
        private Map<String, Integer> categoryResults;
        private long timestamp;
        
        public TestReport(int totalTests, int passedTests, int failedTests, 
                         List<TestResult> results, Map<String, Integer> categoryResults) {
            this.totalTests = totalTests;
            this.passedTests = passedTests;
            this.failedTests = failedTests;
            this.results = results;
            this.categoryResults = categoryResults;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters
        public int getTotalTests() { return totalTests; }
        public int getPassedTests() { return passedTests; }
        public int getFailedTests() { return failedTests; }
        public double getPassRate() { return totalTests > 0 ? (double) passedTests / totalTests * 100 : 0; }
        public List<TestResult> getResults() { return results; }
        public Map<String, Integer> getCategoryResults() { return categoryResults; }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Egyptian Agent Test Report ===\n");
            sb.append("Total Tests: ").append(totalTests).append("\n");
            sb.append("Passed: ").append(passedTests).append("\n");
            sb.append("Failed: ").append(failedTests).append("\n");
            sb.append("Pass Rate: ").append(String.format("%.2f", getPassRate())).append("%\n");
            sb.append("Timestamp: ").append(new java.util.Date(timestamp)).append("\n");
            sb.append("================================\n");
            
            return sb.toString();
        }
    }
}