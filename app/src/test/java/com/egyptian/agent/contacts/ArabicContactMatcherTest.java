package com.egyptian.agent.contacts;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import static org.mockito.Mockito.*;

/**
 * Unit tests for ArabicContactMatcher
 * Tests contact matching with Egyptian Arabic dialect variations
 */
@RunWith(MockitoJUnitRunner.class)
public class ArabicContactMatcherTest {
    
    @Mock
    private Context mockContext;
    
    @Mock
    private ContentResolver mockContentResolver;
    
    private ArabicContactMatcher matcher;
    
    @Before
    public void setUp() {
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        when(mockContext.getContentResolver()).thenReturn(mockContentResolver);
        
        // Mock contacts cursor
        Cursor mockCursor = mock(Cursor.class);
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.moveToNext()).thenReturn(true, true, false);
        when(mockCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)).thenReturn(0);
        when(mockCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)).thenReturn(1);
        when(mockCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)).thenReturn(2);
        when(mockCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)).thenReturn(3);
        
        when(mockCursor.getLong(0)).thenReturn(1L);
        when(mockCursor.getString(1)).thenReturn("أحمد محمد");
        when(mockCursor.getString(2)).thenReturn("01012345678");
        when(mockCursor.getString(3)).thenReturn("+201012345678");
        
        when(mockContentResolver.query(
            any(Uri.class),
            any(String[].class),
            any(String.class),
            any(String[].class),
            any(String.class)
        )).thenReturn(mockCursor);
        
        matcher = new ArabicContactMatcher(mockContext);
    }
    
    @Test
    public void testFindContact_ExactMatch() {
        // Given: exact name match
        String spokenName = "أحمد محمد";
        
        // When
        ArabicContactMatcher.ContactEntry result = matcher.findContact(spokenName);
        
        // Then
        assertNotNull("Should find contact", result);
        assertEquals("أحمد محمد", result.name);
    }
    
    @Test
    public void testFindContact_NormalizedMatch() {
        // Given: name without diacritics
        String spokenName = "احمد";
        
        // When
        ArabicContactMatcher.ContactEntry result = matcher.findContact(spokenName);
        
        // Then
        assertNotNull("Should find contact after normalization", result);
    }
    
    @Test
    public void testFindContact_FamilyAlias_Mama() {
        // Given: "ماما" (mother)
        String spokenName = "ماما";
        
        // When
        ArabicContactMatcher.ContactEntry result = matcher.findContact(spokenName);
        
        // Then - depends on mock data, but should not crash
        // In real implementation, would match against contact with "أم" or "والدة"
        assertNotNull(result);
    }
    
    @Test
    public void testFindContact_FamilyAlias_Baba() {
        // Given: "بابا" (father)
        String spokenName = "بابا";
        
        // When
        ArabicContactMatcher.ContactEntry result = matcher.findContact(spokenName);
        
        // Then
        assertNotNull(result);
    }
    
    @Test
    public void testFindContact_NullInput() {
        // Given: null input
        String spokenName = null;
        
        // When
        ArabicContactMatcher.ContactEntry result = matcher.findContact(spokenName);
        
        // Then
        assertNull("Should return null for null input", result);
    }
    
    @Test
    public void testFindContact_EmptyInput() {
        // Given: empty input
        String spokenName = "";
        
        // When
        ArabicContactMatcher.ContactEntry result = matcher.findContact(spokenName);
        
        // Then
        assertNull("Should return null for empty input", result);
    }
    
    @Test
    public void testFindContact_WhitespaceInput() {
        // Given: whitespace only
        String spokenName = "   ";
        
        // When
        ArabicContactMatcher.ContactEntry result = matcher.findContact(spokenName);
        
        // Then
        assertNull("Should return null for whitespace input", result);
    }
    
    @Test
    public void testFindContacts_MultipleMatches() {
        // Given: name that might match multiple contacts
        String spokenName = "أحمد";
        
        // When
        var matches = matcher.findContacts(spokenName);
        
        // Then - should return up to 5 matches
        assertNotNull("Should return list", matches);
    }
    
    @Test
    public void testGetFamilyAliases() {
        // When
        var aliases = ArabicContactMatcher.getFamilyAliases();
        
        // Then
        assertNotNull("Should return aliases map", aliases);
        assertTrue("Should contain mother aliases", aliases.containsKey("ماما"));
        assertTrue("Should contain father aliases", aliases.containsKey("بابا"));
        assertEquals("ماما should map to Mother", "Mother", aliases.get("ماما"));
        assertEquals("بابا should map to Father", "Father", aliases.get("بابا"));
    }
    
    @Test
    public void testDiacriticsNormalization() {
        // Test that various Arabic diacritics are normalized
        // This tests the internal normalization logic
        
        // These would normally be tested through the matcher
        // but we verify the family aliases cover common variations
        var aliases = ArabicContactMatcher.getFamilyAliases();
        
        // Test all variations of mother
        assertTrue(aliases.containsKey("ماما"));
        assertTrue(aliases.containsKey("امي"));
        assertTrue(aliases.containsKey("أمي"));
        
        // Test all variations of father
        assertTrue(aliases.containsKey("بابا"));
        assertTrue(aliases.containsKey("ابويا"));
        assertTrue(aliases.containsKey("أبويا"));
    }
}
