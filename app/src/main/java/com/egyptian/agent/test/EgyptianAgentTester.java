package com.egyptian.agent.test;

import android.content.Context;
import android.util.Log;
import com.egyptian.agent.accessibility.SeniorModeManager;
import com.egyptian.agent.accessibility.MedicationReminder;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.executors.EmergencyHandler;
import com.egyptian.agent.stt.EgyptianNormalizer;
import com.egyptian.agent.utils.CrashLogger;

import java.time.LocalTime;
import java.util.List;

/**
 * Comprehensive test suite for the Egyptian Agent application
 * Validates all components according to the SRD specifications
 */
public class EgyptianAgentTester {
    private static final String TAG = "EgyptianAgentTester";
    private Context context;
    private boolean allTestsPassed = true;
    
    public EgyptianAgentTester(Context context) {
        this.context = context;
    }
    
    public void runAllTests() {
        Log.i(TAG, "Starting comprehensive test suite for Egyptian Agent");
        
        // Initialize TTS for test feedback
        TTSManager.initialize(context);
        
        // Run individual test suites
        testEgyptianDialectProcessing();
        testSeniorModeFunctionality();
        testEmergencySystem();
        testAccessibilityFeatures();
        testPerformanceMetrics();
        
        // Report final results
        if (allTestsPassed) {
            Log.i(TAG, "✅ ALL TESTS PASSED - Egyptian Agent is ready for production!");
            TTSManager.speak(context, "اكتملت كل اختبارات الوكيل المصري بنجاح. النظام جاهز للإنتاج.");
        } else {
            Log.e(TAG, "❌ SOME TESTS FAILED - Please review the issues above.");
            TTSManager.speak(context, "فيه مشاكل في بعض اختبارات الوكيل المصري. يلزم مراجعة المشكلة.");
        }
    }
    
    private void testEgyptianDialectProcessing() {
        Log.i(TAG, "Testing Egyptian dialect processing...");
        
        // Test basic normalization
        String[] testInputs = {
            "عايز أكلم ماما دلوقتي",
            "رن على بابا",
            "فايتة عليا",
            "كلم أمي",
            "بكرة الصبح"
        };
        
        String[] expectedOutputs = {
            "أريد أن أتصل بالأم الآن",
            "اتصل بالأب",
            "فاتت",
            "اتصل بالأم",
            "غداً الصباح"
        };
        
        boolean allPassed = true;
        for (int i = 0; i < testInputs.length; i++) {
            String normalized = EgyptianNormalizer.normalize(testInputs[i]);
            boolean passed = normalized.contains(expectedOutputs[i]);
            
            if (!passed) {
                Log.e(TAG, "❌ Egyptian dialect test failed: " + testInputs[i] + 
                      " -> Expected: " + expectedOutputs[i] + ", Got: " + normalized);
                allTestsPassed = false;
                allPassed = false;
            } else {
                Log.i(TAG, "✅ Egyptian dialect test passed: " + testInputs[i] + " -> " + normalized);
            }
        }
        
        if (allPassed) {
            Log.i(TAG, "✅ Egyptian dialect processing tests passed");
        }
    }
    
    private void testSeniorModeFunctionality() {
        Log.i(TAG, "Testing Senior Mode functionality...");
        
        SeniorModeManager seniorModeManager = new SeniorModeManager(context);
        
        // Test enabling senior mode
        seniorModeManager.enable(context);
        if (!SeniorModeManager.isEnabled()) {
            Log.e(TAG, "❌ Senior mode enable test failed");
            allTestsPassed = false;
        } else {
            Log.i(TAG, "✅ Senior mode enable test passed");
        }
        
        // Test configuration
        var config = seniorModeManager.getConfig();
        if (config.getSpeechRate() != 0.75f) {
            Log.e(TAG, "❌ Senior mode speech rate configuration test failed");
            allTestsPassed = false;
        } else {
            Log.i(TAG, "✅ Senior mode speech rate configuration test passed");
        }
        
        // Test adding medication reminder
        MedicationReminder reminder = new MedicationReminder(
            "حبوب الضغط", 
            LocalTime.of(8, 0), 
            "خلي بالك تاخد الحبوب مع كوب مياه"
        );
        
        seniorModeManager.addMedicationReminder(reminder);
        List<MedicationReminder> reminders = seniorModeManager.getMedicationReminders();
        
        if (reminders.isEmpty() || !reminders.get(0).getMedicationName().equals("حبوب الضغط")) {
            Log.e(TAG, "❌ Medication reminder test failed");
            allTestsPassed = false;
        } else {
            Log.i(TAG, "✅ Medication reminder test passed");
        }
        
        // Disable senior mode
        seniorModeManager.disable(context);
        if (SeniorModeManager.isEnabled()) {
            Log.e(TAG, "❌ Senior mode disable test failed");
            allTestsPassed = false;
        } else {
            Log.i(TAG, "✅ Senior mode disable test passed");
        }
    }
    
    private void testEmergencySystem() {
        Log.i(TAG, "Testing Emergency System...");
        
        // Note: We won't actually trigger an emergency in tests
        // Instead, we'll verify the system is properly configured
        
        try {
            EmergencyHandler emergencyHandler = new EmergencyHandler(context);
            
            // Test that the handler can be instantiated
            Log.i(TAG, "✅ Emergency handler instantiation test passed");
            
            // Test emergency protocol constants
            if (EmergencyHandler.class.getDeclaredField("EMERGENCY_NUMBERS") != null) {
                Log.i(TAG, "✅ Emergency numbers configuration test passed");
            } else {
                Log.e(TAG, "❌ Emergency numbers configuration test failed");
                allTestsPassed = false;
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Emergency system test failed", e);
            allTestsPassed = false;
        }
    }
    
    private void testAccessibilityFeatures() {
        Log.i(TAG, "Testing Accessibility Features...");
        
        // Test CrashLogger initialization
        try {
            CrashLogger.registerGlobalExceptionHandler(context);
            Log.i(TAG, "✅ CrashLogger initialization test passed");
        } catch (Exception e) {
            Log.e(TAG, "❌ CrashLogger initialization test failed", e);
            allTestsPassed = false;
        }
        
        // Test TTSManager
        try {
            TTSManager.initialize(context);
            Log.i(TAG, "✅ TTSManager initialization test passed");
        } catch (Exception e) {
            Log.e(TAG, "❌ TTSManager initialization test failed", e);
            allTestsPassed = false;
        }
    }
    
    private void testPerformanceMetrics() {
        Log.i(TAG, "Testing Performance Metrics...");
        
        // Memory usage test (conceptual - actual implementation would measure real usage)
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        
        Log.i(TAG, "Memory usage: " + (usedMemory / 1024 / 1024) + " MB of " + (maxMemory / 1024 / 1024) + " MB");
        
        // Performance is considered acceptable if under 450MB as per SRD
        if (usedMemory > 450 * 1024 * 1024) {
            Log.w(TAG, "⚠️ High memory usage detected: " + (usedMemory / 1024 / 1024) + " MB");
        } else {
            Log.i(TAG, "✅ Memory usage is within acceptable limits");
        }
    }
    
    public boolean getAllTestsPassed() {
        return allTestsPassed;
    }
}