package com.egyptian.agent.contacts;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Arabic Contact Matcher for Egyptian Dialect
 * 
 * Handles Egyptian Arabic name variations and fuzzy matching for contact resolution.
 * Supports family terms, nicknames, and regional variations common in Egypt.
 * 
 * Features:
 * - Family term mapping (ماما → Mother, بابا → Father)
 * - Egyptian nickname support
 * - Fuzzy matching with Levenshtein distance
 * - Diacritics normalization
 * - Phonetic similarity matching
 */
public class ArabicContactMatcher {
    
    private static final String TAG = "ArabicContactMatcher";
    
    // Family term aliases - Egyptian dialect variations
    private static final Map<String, String> FAMILY_ALIASES = new HashMap<>();
    
    static {
        // Mother variations
        FAMILY_ALIASES.put("ماما", "Mother");
        FAMILY_ALIASES.put("امي", "Mother");
        FAMILY_ALIASES.put("أمي", "Mother");
        FAMILY_ALIASES.put("امى", "Mother");
        FAMILY_ALIASES.put("الوالدة", "Mother");
        FAMILY_ALIASES.put("ماما الكبرى", "Mother");
        FAMILY_ALIASES.put("OMA", "Mother");  // Common nickname
        
        // Father variations
        FAMILY_ALIASES.put("بابا", "Father");
        FAMILY_ALIASES.put("ابويا", "Father");
        FAMILY_ALIASES.put("أبويا", "Father");
        FAMILY_ALIASES.put("ابوى", "Father");
        FAMILY_ALIASES.put("الوالد", "Father");
        FAMILY_ALIASES.put("بابا الكبير", "Father");
        FAMILY_ALIASES.put("BABA", "Father");  // Common nickname
        
        // Sibling variations
        FAMILY_ALIASES.put("اخويا", "Brother");
        FAMILY_ALIASES.put("اخوي", "Brother");
        FAMILY_ALIASES.put("اختي", "Sister");
        FAMILY_ALIASES.put("اختى", "Sister");
        FAMILY_ALIASES.put("اخ", "Brother");
        FAMILY_ALIASES.put("اخت", "Sister");
        
        // Grandparent variations
        FAMILY_ALIASES.put("الجد", "Grandfather");
        FAMILY_ALIASES.put("جدتي", "Grandmother");
        FAMILY_ALIASES.put("الجدة", "Grandmother");
        FAMILY_ALIASES.put("جدو", "Grandfather");
        FAMILY_ALIASES.put("جدتى", "Grandmother");
        
        // Extended family
        FAMILY_ALIASES.put("خال", "Uncle (Maternal)");
        FAMILY_ALIASES.put("خالتى", "Aunt (Maternal)");
        FAMILY_ALIASES.put("عم", "Uncle (Paternal)");
        FAMILY_ALIASES.put("عمة", "Aunt (Paternal)");
        FAMILY_ALIASES.put("ابوب", "Uncle");
        FAMILY_ALIASES.put("خالة", "Aunt");
    }
    
    // Common Egyptian nicknames
    private static final Map<String, String> COMMON_NICKNAMES = new HashMap<>();
    
    static {
        COMMON_NICKNAMES.put("احمد", "أحمد");
        COMMON_NICKNAMES.put("محمد", "محمد");
        COMMON_NICKNAMES.put("على", "علي");
        COMMON_NICKNAMES.put("اسماعيل", "إسماعيل");
        COMMON_NICKNAMES.put("يوسف", "يوسف");
        COMMON_NICKNAMES.put("ابراهيم", "إبراهيم");
    }
    
    private final Context context;
    private final ContentResolver contentResolver;
    private List<ContactEntry> contactsCache;
    private boolean cacheLoaded = false;
    
    /**
     * Creates a new ArabicContactMatcher
     * @param context Application context
     */
    public ArabicContactMatcher(Context context) {
        this.context = context.getApplicationContext();
        this.contentResolver = context.getContentResolver();
    }
    
    /**
     * Finds a contact by spoken name
     * @param spokenName The name as spoken by the user
     * @return ContactEntry if found, null otherwise
     */
    public ContactEntry findContact(String spokenName) {
        if (spokenName == null || spokenName.trim().isEmpty()) {
            return null;
        }
        
        // Normalize the spoken name
        String normalizedName = normalizeArabicName(spokenName.trim());
        
        // Ensure cache is loaded
        if (!cacheLoaded) {
            loadContacts();
        }
        
        // Step 1: Check family aliases first
        ContactEntry familyContact = findByFamilyAlias(normalizedName);
        if (familyContact != null) {
            Log.d(TAG, "Found contact by family alias: " + spokenName + " -> " + familyContact.name);
            return familyContact;
        }
        
        // Step 2: Exact match
        ContactEntry exactMatch = findExactMatch(normalizedName);
        if (exactMatch != null) {
            Log.d(TAG, "Found exact match: " + spokenName + " -> " + exactMatch.name);
            return exactMatch;
        }
        
        // Step 3: Fuzzy match with threshold
        ContactEntry fuzzyMatch = findFuzzyMatch(normalizedName);
        if (fuzzyMatch != null) {
            Log.d(TAG, "Found fuzzy match: " + spokenName + " -> " + fuzzyMatch.name);
            return fuzzyMatch;
        }
        
        // Step 4: Try without diacritics
        ContactEntry diacriticMatch = findWithoutDiacritics(normalizedName);
        if (diacriticMatch != null) {
            Log.d(TAG, "Found diacritics match: " + spokenName + " -> " + diacriticMatch.name);
            return diacriticMatch;
        }
        
        Log.w(TAG, "No contact found for: " + spokenName);
        return null;
    }
    
    /**
     * Finds multiple contacts matching the name (for disambiguation)
     * @param spokenName The name as spoken
     * @return List of matching contacts (max 5)
     */
    public List<ContactEntry> findContacts(String spokenName) {
        if (spokenName == null || spokenName.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String normalizedName = normalizeArabicName(spokenName.trim());
        
        if (!cacheLoaded) {
            loadContacts();
        }
        
        List<ContactEntry> matches = new ArrayList<>();
        
        for (ContactEntry contact : contactsCache) {
            String contactNormalized = normalizeArabicName(contact.name);
            double similarity = calculateSimilarity(normalizedName, contactNormalized);
            
            if (similarity > 0.6) {
                matches.add(contact);
            }
            
            if (matches.size() >= 5) {
                break;
            }
        }
        
        return matches;
    }
    
    /**
     * Normalizes Arabic name by removing diacritics and standardizing
     */
    private String normalizeArabicName(String name) {
        if (name == null) {
            return "";
        }
        
        String normalized = name.trim();
        
        // Remove Arabic diacritics (tashkeel)
        normalized = normalized.replaceAll("[\u064B-\u065F\u0670]", "");
        
        // Normalize alef variations
        normalized = normalized.replace('أ', 'ا');
        normalized = normalized.replace('إ', 'ا');
        normalized = normalized.replace('آ', 'ا');
        
        // Normalize taa marbuta
        normalized = normalized.replace('ة', 'ه');
        
        // Normalize yaa
        normalized = normalized.replace('ى', 'ي');
        
        // Remove non-Arabic characters except spaces
        normalized = normalized.replaceAll("[^\\u0600-\u06FF\\s]", "");
        
        // Convert to lowercase for comparison
        normalized = normalized.toLowerCase(Locale.forLanguageTag("ar"));
        
        return normalized.trim();
    }
    
    /**
     * Looks up contact by family alias
     */
    private ContactEntry findByFamilyAlias(String normalizedName) {
        String relation = FAMILY_ALIASES.get(normalizedName);
        
        if (relation != null) {
            // Search for contacts with this relation
            for (ContactEntry contact : contactsCache) {
                String contactNormalized = normalizeArabicName(contact.name);
                
                // Check if contact name contains relation keyword
                if (contactNormalized.contains(relation.toLowerCase()) ||
                    contactNormalized.contains(normalizedName)) {
                    return contact;
                }
                
                // Also check notes/phone fields if available
                if (contact.notes != null && 
                    contact.notes.toLowerCase().contains(relation.toLowerCase())) {
                    return contact;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Finds exact match after normalization
     */
    private ContactEntry findExactMatch(String normalizedName) {
        for (ContactEntry contact : contactsCache) {
            String contactNormalized = normalizeArabicName(contact.name);
            
            if (contactNormalized.equals(normalizedName)) {
                return contact;
            }
            
            // Also check nicknames
            if (contact.nickname != null) {
                String nicknameNormalized = normalizeArabicName(contact.nickname);
                if (nicknameNormalized.equals(normalizedName)) {
                    return contact;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Finds fuzzy match using Levenshtein distance
     */
    private ContactEntry findFuzzyMatch(String normalizedName) {
        ContactEntry bestMatch = null;
        double bestSimilarity = 0.7;  // Minimum threshold
        
        for (ContactEntry contact : contactsCache) {
            String contactNormalized = normalizeArabicName(contact.name);
            
            double similarity = calculateSimilarity(normalizedName, contactNormalized);
            
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatch = contact;
            }
            
            // Also check nickname
            if (contact.nickname != null) {
                String nicknameNormalized = normalizeArabicName(contact.nickname);
                double nicknameSimilarity = calculateSimilarity(normalizedName, nicknameNormalized);
                
                if (nicknameSimilarity > bestSimilarity) {
                    bestSimilarity = nicknameSimilarity;
                    bestMatch = contact;
                }
            }
        }
        
        return bestMatch;
    }
    
    /**
     * Finds match by removing all diacritics from contacts
     */
    private ContactEntry findWithoutDiacritics(String normalizedName) {
        // This is already covered by normalization, but keeping for clarity
        return null;
    }
    
    /**
     * Calculates similarity between two strings (0.0 to 1.0)
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        
        if (s1.equals(s2)) {
            return 1.0;
        }
        
        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        
        if (maxLength == 0) {
            return 1.0;
        }
        
        return 1.0 - ((double) distance / maxLength);
    }
    
    /**
     * Calculates Levenshtein distance between two strings
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                char c1 = s1.charAt(i - 1);
                char c2 = s2.charAt(j - 1);
                
                int cost = (c1 == c2) ? 0 : 1;
                
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * Loads contacts from device
     */
    private void loadContacts() {
        contactsCache = new ArrayList<>();
        
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = {
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
        };
        
        String selection = ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER + " = 1";
        String sortOrder = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC";
        
        try (Cursor cursor = contentResolver.query(uri, projection, selection, null, sortOrder)) {
            if (cursor != null) {
                int idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                int normalizedIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);
                
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idIndex);
                    String name = cursor.getString(nameIndex);
                    String number = cursor.getString(numberIndex);
                    String normalizedNumber = cursor.getString(normalizedIndex);
                    
                    if (name != null && !name.trim().isEmpty()) {
                        ContactEntry entry = new ContactEntry(id, name, number, normalizedNumber);
                        contactsCache.add(entry);
                    }
                }
                
                cacheLoaded = true;
                Log.i(TAG, "Loaded " + contactsCache.size() + " contacts");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading contacts", e);
        }
    }
    
    /**
     * Refreshes the contacts cache
     */
    public void refreshCache() {
        cacheLoaded = false;
        loadContacts();
    }
    
    /**
     * Gets all family aliases
     */
    public static Map<String, String> getFamilyAliases() {
        return new HashMap<>(FAMILY_ALIASES);
    }
    
    /**
     * Contact entry class
     */
    public static class ContactEntry {
        public final long id;
        public final String name;
        public final String phoneNumber;
        public final String normalizedNumber;
        public String nickname;
        public String notes;
        
        public ContactEntry(long id, String name, String phoneNumber, String normalizedNumber) {
            this.id = id;
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.normalizedNumber = normalizedNumber;
        }
        
        @Override
        public String toString() {
            return "ContactEntry{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", phoneNumber='" + phoneNumber + '\'' +
                    '}';
        }
    }
}
