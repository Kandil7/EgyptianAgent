package com.egyptian.agent.test;

import android.content.Context;
import android.util.Log;
import com.egyptian.agent.core.Quantum;
import com.egyptian.agent.core.OfflineGrammarProcessor;
import com.egyptian.agent.core.ContextMemory;

/**
 * EgyptianAgentTester - Validates core functionality of the Egyptian Agent
 * Tests the main components to ensure they work correctly
 */
public class EgyptianAgentTester {
    private static final String TAG = "EgyptianAgentTester";
    
    private Context context;
    private Quantum quantum;
    private OfflineGrammarProcessor grammarProcessor;
    private ContextMemory contextMemory;
    
    public EgyptianAgentTester(Context context) {
        this.context = context;
        this.quantum = new Quantum(context);
        this.grammarProcessor = new OfflineGrammarProcessor(context);
        this.contextMemory = ContextMemory.getInstance(context);
    }
    
    /**
     * Runs all tests to validate the Egyptian Agent functionality
     */
    public void runAllTests() {
        Log.i(TAG, "Starting Egyptian Agent functionality tests...");
        
        // Test intent classification
        testIntentClassification();
        
        // Test context memory
        testContextMemory();
        
        // Test offline grammar processing
        testOfflineGrammarProcessing();
        
        // Test command processing
        testCommandProcessing();
        
        Log.i(TAG, "Egyptian Agent functionality tests completed!");
    }
    
    /**
     * Tests intent classification functionality
     */
    private void testIntentClassification() {
        Log.i(TAG, "Testing intent classification...");
        
        String[] testCommands = {
            "اتصل بماما",
            "ابعت رسالة لجدو",
            "شغّل قرآن",
            "علّي الصوت",
            "افتح واتساب",
            "قولي المكالمات الفايتة"
        };
        
        for (String command : testCommands) {
            String intent = quantum.classifyIntent(command);
            Log.i(TAG, "Command: '" + command + "' -> Intent: " + intent);
        }
        
        Log.i(TAG, "Intent classification tests completed");
    }
    
    /**
     * Tests context memory functionality
     */
    private void testContextMemory() {
        Log.i(TAG, "Testing context memory...");
        
        // Test setting and getting values
        contextMemory.setLastContact("أمي");
        String lastContact = contextMemory.getLastContact();
        Log.i(TAG, "Last contact: " + lastContact);
        
        contextMemory.setLastApp("واتساب");
        String lastApp = contextMemory.getLastApp();
        Log.i(TAG, "Last app: " + lastApp);
        
        // Test context update
        contextMemory.updateContext("call_person", "contact", "بابا");
        Log.i(TAG, "Updated context for call to بابا");
        
        Log.i(TAG, "Context memory tests completed");
    }
    
    /**
     * Tests offline grammar processing
     */
    private void testOfflineGrammarProcessing() {
        Log.i(TAG, "Testing offline grammar processing...");
        
        String[] testCommands = {
            "اتصل بماما",
            "ابعت واتساب لجدو",
            "شغّل قرآن كريم",
            "علّي الصوت شوية",
            "افتح التليفون",
            "إيه الوقت دلوقتي"
        };
        
        for (String command : testCommands) {
            String intent = grammarProcessor.processCommand(command);
            Log.i(TAG, "Grammar processing - Command: '" + command + "' -> Intent: " + intent);
        }
        
        Log.i(TAG, "Offline grammar processing tests completed");
    }
    
    /**
     * Tests command processing
     */
    private void testCommandProcessing() {
        Log.i(TAG, "Testing command processing...");
        
        String[] testCommands = {
            "اتصل بماما",
            "ابعت فويس لأخويا",
            "شغّل أغاني محمد منير",
            "هدّي الصوت",
            "قول لتيتا إني جايلك بكرة"
        };
        
        for (String command : testCommands) {
            Log.i(TAG, "Processing command: '" + command + "'");
            // Note: We won't actually execute these commands in testing mode
            // Just validate that they would be processed
            String intent = quantum.classifyIntent(command);
            Log.i(TAG, "Classified as: " + intent);
        }
        
        Log.i(TAG, "Command processing tests completed");
    }
    
    /**
     * Runs a specific test scenario
     */
    public void runScenarioTest(String scenario) {
        Log.i(TAG, "Running scenario test: " + scenario);
        
        switch(scenario) {
            case "call_scenario":
                testCallScenario();
                break;
            case "message_scenario":
                testMessageScenario();
                break;
            case "media_scenario":
                testMediaScenario();
                break;
            default:
                Log.w(TAG, "Unknown scenario: " + scenario);
        }
    }
    
    /**
     * Tests a call scenario
     */
    private void testCallScenario() {
        Log.i(TAG, "Testing call scenario...");
        
        // Simulate: "اتصل بماما"
        String command1 = "اتصل بماما";
        String intent1 = quantum.classifyIntent(command1);
        Log.i(TAG, "Command: '" + command1 + "' -> Intent: " + intent1);
        
        // Simulate: "كلمه" (referring to last contact)
        String command2 = "كلمه";
        String intent2 = quantum.classifyIntent(command2);
        Log.i(TAG, "Command: '" + command2 + "' -> Intent: " + intent2);
        
        Log.i(TAG, "Call scenario test completed");
    }
    
    /**
     * Tests a message scenario
     */
    private void testMessageScenario() {
        Log.i(TAG, "Testing message scenario...");
        
        String command = "ابعت فويس لجدو";
        String intent = quantum.classifyIntent(command);
        Log.i(TAG, "Command: '" + command + "' -> Intent: " + intent);
        
        Log.i(TAG, "Message scenario test completed");
    }
    
    /**
     * Tests a media scenario
     */
    private void testMediaScenario() {
        Log.i(TAG, "Testing media scenario...");
        
        String command = "شغّل قرآن هادي";
        String intent = quantum.classifyIntent(command);
        Log.i(TAG, "Command: '" + command + "' -> Intent: " + intent);
        
        Log.i(TAG, "Media scenario test completed");
    }
}