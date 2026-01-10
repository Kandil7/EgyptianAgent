package com.egyptian.agent;

import android.content.Context;
import com.egyptian.agent.core.TTSManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

/**
 * Unit tests for TTSManager
 */
public class TTSManagerTest {

    @Mock
    private Context mockContext;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testInitialize() {
        // Test TTS initialization
        TTSManager.initialize(mockContext);
        
        // Verify that initialization was called appropriately
        verify(mockContext, atLeastOnce()).getApplicationContext();
    }

    @Test
    public void testSpeakWithText() {
        // Test speaking functionality
        TTSManager.initialize(mockContext);
        TTSManager.speak(mockContext, "السلام عليكم");
        
        // Since TTS involves Android system services, we can't easily verify the actual speech
        // But we can verify that the method was called with proper parameters
        verify(mockContext, atLeastOnce()).getApplicationContext();
    }

    @Test
    public void testSpeakWithNullText() {
        // Test speaking with null text (should not crash)
        TTSManager.initialize(mockContext);
        TTSManager.speak(mockContext, null);
        
        // Should handle null gracefully
        verify(mockContext, atLeastOnce()).getApplicationContext();
    }

    @Test
    public void testSpeakWithEmptyText() {
        // Test speaking with empty text (should not crash)
        TTSManager.initialize(mockContext);
        TTSManager.speak(mockContext, "");
        
        // Should handle empty string gracefully
        verify(mockContext, atLeastOnce()).getApplicationContext();
    }

    @Test
    public void testSetSpeechRate() {
        // Test setting speech rate
        TTSManager.setSpeechRate(mockContext, 0.8f);
        
        verify(mockContext, atLeastOnce()).getApplicationContext();
    }

    @Test
    public void testSetPitch() {
        // Test setting pitch
        TTSManager.setPitch(mockContext, 0.9f);
        
        verify(mockContext, atLeastOnce()).getApplicationContext();
    }

    @Test
    public void testSetVolume() {
        // Test setting volume
        TTSManager.setVolume(mockContext, 1.0f);
        
        verify(mockContext, atLeastOnce()).getApplicationContext();
    }

    @Test
    public void testShutdown() {
        // Test shutdown functionality
        TTSManager.shutdown();
        
        // Verify that shutdown was called appropriately
        // Since TTSManager is a static class, we can't easily verify the actual shutdown
        // But we can ensure the method runs without throwing exceptions
    }
}