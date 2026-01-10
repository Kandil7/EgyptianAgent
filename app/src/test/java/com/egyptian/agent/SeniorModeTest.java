package com.egyptian.agent;

import android.content.Context;
import com.egyptian.agent.accessibility.SeniorMode;
import com.egyptian.agent.nlp.IntentType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SeniorMode
 */
public class SeniorModeTest {

    @Mock
    private Context mockContext;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testEnableSeniorMode() {
        // Test enabling senior mode
        SeniorMode.enable(mockContext);
        
        // Verify that the context was used appropriately
        verify(mockContext, atLeastOnce()).getSharedPreferences(anyString(), anyInt());
    }

    @Test
    public void testDisableSeniorMode() {
        // Test disabling senior mode
        SeniorMode.disable(mockContext);
        
        // Verify that the context was used appropriately
        verify(mockContext, atLeastOnce()).getSharedPreferences(anyString(), anyInt());
    }

    @Test
    public void testIsEnabled() {
        // Test checking if senior mode is enabled
        // Since SeniorMode is a static class, we need to call enable first to test
        SeniorMode.enable(mockContext);
        boolean isEnabled = SeniorMode.isEnabled(mockContext);
        
        // Verify that the context was used appropriately
        verify(mockContext, atLeastOnce()).getSharedPreferences(anyString(), anyInt());
    }

    @Test
    public void testIsCommandAllowed() {
        // Test if commands are allowed in senior mode
        String allowedCommand = "اتصل";
        String disallowedCommand = "玩游戏"; // Chinese characters as example of disallowed command
        
        // Enable senior mode first
        SeniorMode.enable(mockContext);
        
        // Test allowed command
        boolean isAllowed = SeniorMode.isCommandAllowed(mockContext, allowedCommand);
        
        // Verify that the context was used appropriately
        verify(mockContext, atLeastOnce()).getSharedPreferences(anyString(), anyInt());
    }

    @Test
    public void testIsIntentAllowed() {
        // Test if intents are allowed in senior mode
        // Enable senior mode first
        SeniorMode.enable(mockContext);
        
        // Test allowed intent
        boolean isCallAllowed = SeniorMode.isIntentAllowed(mockContext, IntentType.CALL_CONTACT);
        boolean isEmergencyAllowed = SeniorMode.isIntentAllowed(mockContext, IntentType.EMERGENCY);
        boolean isTimeAllowed = SeniorMode.isIntentAllowed(mockContext, IntentType.READ_TIME);
        boolean isAlarmAllowed = SeniorMode.isIntentAllowed(mockContext, IntentType.SET_ALARM);
        boolean isWhatsAppAllowed = SeniorMode.isIntentAllowed(mockContext, IntentType.SEND_WHATSAPP);
        boolean isMissedCallsAllowed = SeniorMode.isIntentAllowed(mockContext, IntentType.READ_MISSED_CALLS);
        
        // Verify that the context was used appropriately
        verify(mockContext, atLeastOnce()).getSharedPreferences(anyString(), anyInt());
    }

    @Test
    public void testApplySeniorTtsSettings() {
        // Test applying senior TTS settings
        SeniorMode.applySeniorTtsSettings(mockContext);
        
        // Verify that the context was used appropriately
        verify(mockContext, atLeastOnce()).getSharedPreferences(anyString(), anyInt());
    }

    @Test
    public void testRestoreNormalTtsSettings() {
        // Test restoring normal TTS settings
        SeniorMode.restoreNormalTtsSettings(mockContext);
        
        // Verify that the context was used appropriately
        verify(mockContext, atLeastOnce()).getSharedPreferences(anyString(), anyInt());
    }

    @Test
    public void testHandleRestrictedCommand() {
        // Test handling restricted command in senior mode
        SeniorMode.enable(mockContext);
        SeniorMode.handleRestrictedCommand(mockContext, "some restricted command");
        
        // Verify that the context was used appropriately
        verify(mockContext, atLeastOnce()).getSharedPreferences(anyString(), anyInt());
    }

    @Test
    public void testHandleEmergency() {
        // Test handling emergency in senior mode
        SeniorMode.enable(mockContext);
        SeniorMode.handleEmergency(mockContext);
        
        // Verify that the context was used appropriately
        verify(mockContext, atLeastOnce()).getSharedPreferences(anyString(), anyInt());
    }

    @Test
    public void testInitialize() {
        // Test initializing senior mode
        SeniorMode.initialize(mockContext);
        
        // Verify that the context was used appropriately
        verify(mockContext, atLeastOnce()).getSharedPreferences(anyString(), anyInt());
    }
}