package com.egyptian.agent;

import android.content.Context;
import android.content.SharedPreferences;
import com.egyptian.agent.accessibility.SeniorMode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SeniorMode
 */
public class SeniorModeTest {

    @Mock
    private Context mockContext;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Stub preferences so SeniorMode can persist state without Android runtime
        sharedPreferences = mock(SharedPreferences.class);
        editor = mock(SharedPreferences.Editor.class);
        when(sharedPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(false);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor);
        when(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences);
    }

    @Test
    public void testInitialize() {
        // Test initializing senior mode
        SeniorMode.initialize(mockContext);

        // Verify that the context was used appropriately
        verify(mockContext, atLeastOnce()).getSharedPreferences(anyString(), anyInt());
    }

    @Test
    public void testEnableSeniorMode() {
        // Test enabling senior mode
        SeniorMode.initialize(mockContext);
        SeniorMode.enable(mockContext);

        assertTrue("Senior mode should be enabled after enable()", SeniorMode.isEnabled());
        verify(sharedPreferences).edit();
    }

    @Test
    public void testDisableSeniorMode() {
        // Test disabling senior mode
        SeniorMode.initialize(mockContext);
        SeniorMode.enable(mockContext);
        SeniorMode.disable(mockContext);

        assertFalse("Senior mode should be disabled after disable()", SeniorMode.isEnabled());
    }

    @Test
    public void testIsEnabled() {
        // Test checking if senior mode is enabled
        SeniorMode.initialize(mockContext);

        assertFalse("Senior mode should be disabled initially", SeniorMode.isEnabled());

        SeniorMode.enable(mockContext);
        assertTrue("Senior mode should be enabled after enable()", SeniorMode.isEnabled());

        SeniorMode.disable(mockContext);
        assertFalse("Senior mode should be disabled after disable()", SeniorMode.isEnabled());
    }

    @Test
    public void testIsCommandAllowed() {
        // Test if commands are allowed in senior mode
        assertTrue("Commands should be allowed in senior mode", SeniorMode.isCommandAllowed("اتصل"));
        assertTrue("Empty command should be allowed", SeniorMode.isCommandAllowed(""));
    }

    @Test
    public void testHandleRestrictedCommand() {
        // Test handling restricted command in senior mode
        SeniorMode.initialize(mockContext);
        SeniorMode.handleRestrictedCommand(mockContext, "some restricted command");

        // Should not throw exceptions
        assertTrue("handleRestrictedCommand should complete without exceptions", true);
    }
}
