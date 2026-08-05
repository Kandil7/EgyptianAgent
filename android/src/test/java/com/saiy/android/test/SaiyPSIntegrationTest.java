package com.saiy.android.test;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.saiy.android.core.Quantum;
import com.saiy.android.configuration.EgyptianConfiguration;

/**
 * Integration tests for Saiy-PS with Egyptian Agent
 */
public class SaiyPSIntegrationTest {

    @Mock
    private Context mockContext;

    private Quantum quantum;
    private EgyptianConfiguration egyptianConfig;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Use the test application context
        Context context = ApplicationProvider.getApplicationContext();
        
        quantum = new Quantum(context);
        egyptianConfig = new EgyptianConfiguration(context);
    }

    @Test
    public void testEgyptianConfigurationInitialization() {
        assertNotNull("EgyptianConfiguration should be initialized", egyptianConfig);
        assertTrue("Egyptian mode should be enabled by default", egyptianConfig.isEgyptianModeEnabled());
    }

    @Test
    public void testEgyptianWakeWordsExist() {
        assertNotNull("Egyptian wake words should exist", EgyptianConfiguration.EGYPTIAN_WAKE_WORDS);
        assertFalse("Egyptian wake words should not be empty", EgyptianConfiguration.EGYPTIAN_WAKE_WORDS.isEmpty());
        
        // Verify specific wake words
        assertTrue("Should contain 'يا حكيم'", EgyptianConfiguration.EGYPTIAN_WAKE_WORDS.contains("يا حكيم"));
        assertTrue("Should contain 'يا كبير'", EgyptianConfiguration.EGYPTIAN_WAKE_WORDS.contains("يا كبير"));
        assertTrue("Should contain 'ساعدني'", EgyptianConfiguration.EGYPTIAN_WAKE_WORDS.contains("ساعدني"));
    }

    @Test
    public void testEgyptianEmergencyPhrasesExist() {
        assertNotNull("Egyptian emergency phrases should exist", EgyptianConfiguration.EGYPTIAN_EMERGENCY_PHRASES);
        assertFalse("Egyptian emergency phrases should not be empty", EgyptianConfiguration.EGYPTIAN_EMERGENCY_PHRASES.isEmpty());
        
        // Verify specific emergency phrases
        assertTrue("Should contain 'نجدة'", EgyptianConfiguration.EGYPTIAN_EMERGENCY_PHRASES.contains("نجدة"));
        assertTrue("Should contain 'استغاثة'", EgyptianConfiguration.EGYPTIAN_EMERGENCY_PHRASES.contains("استغاثة"));
    }

    @Test
    public void testQuantumInitialization() {
        assertNotNull("Quantum should be initialized", quantum);
    }

    @Test
    public void testSeniorModeToggle() {
        // Initially should be disabled
        assertFalse("Senior mode should be disabled by default", egyptianConfig.isSeniorModeEnabled());
        
        // Enable senior mode
        egyptianConfig.setSeniorModeEnabled(true);
        assertTrue("Senior mode should be enabled after setting", egyptianConfig.isSeniorModeEnabled());
        
        // Verify senior settings are applied
        assertTrue("Double confirmation should be enabled in senior mode", 
                  egyptianConfig.isDoubleConfirmationEnabled());
        
        // Disable senior mode
        egyptianConfig.setSeniorModeEnabled(false);
        assertFalse("Senior mode should be disabled after setting", egyptianConfig.isSeniorModeEnabled());
    }

    @Test
    public void testEgyptianSpeechRate() {
        // Test default speech rate
        float defaultRate = egyptianConfig.getEgyptianSpeechRate();
        assertEquals(1.0f, defaultRate, 0.01f);
        
        // Enable senior mode and test speech rate
        egyptianConfig.setSeniorModeEnabled(true);
        float seniorRate = egyptianConfig.getEgyptianSpeechRate();
        assertEquals(0.75f, seniorRate, 0.01f);
    }
}