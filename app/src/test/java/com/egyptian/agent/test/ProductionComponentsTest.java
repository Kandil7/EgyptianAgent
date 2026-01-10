package com.egyptian.agent.test;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.egyptian.agent.security.DataEncryptionManager;
import com.egyptian.agent.performance.PerformanceMonitor;
import com.egyptian.agent.backup.BackupRestoreManager;
import com.egyptian.agent.feedback.UserFeedbackSystem;

/**
 * Automated testing suite for production components
 */
public class ProductionComponentsTest {

    @Mock
    private Context mockContext;

    private DataEncryptionManager encryptionManager;
    private PerformanceMonitor performanceMonitor;
    private BackupRestoreManager backupRestoreManager;
    private UserFeedbackSystem feedbackSystem;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Use the test application context
        Context context = ApplicationProvider.getApplicationContext();
        
        encryptionManager = DataEncryptionManager.getInstance(context);
        performanceMonitor = PerformanceMonitor.getInstance(context);
        backupRestoreManager = BackupRestoreManager.getInstance(context);
        feedbackSystem = UserFeedbackSystem.getInstance(context);
    }

    @Test
    public void testDataEncryptionManagerInitialization() {
        assertNotNull("DataEncryptionManager should be initialized", encryptionManager);
        assertTrue("Encryption should be ready", encryptionManager.isEncryptionReady());
    }

    @Test
    public void testDataEncryptionAndDecryption() {
        String testKey = "test_key";
        String testValue = "sensitive_data";
        
        // Store data
        encryptionManager.storeSensitiveData(testKey, testValue);
        
        // Retrieve data
        String retrievedValue = encryptionManager.retrieveSensitiveData(testKey);
        
        assertEquals("Retrieved value should match stored value", testValue, retrievedValue);
    }

    @Test
    public void testEmergencyContactStorage() {
        String name = "Emergency Contact";
        String number = "1234567890";
        
        // Store emergency contact
        encryptionManager.storeEmergencyContact(name, number);
        
        // Retrieve emergency contact
        String[] contact = encryptionManager.retrieveEmergencyContact();
        
        assertEquals("Name should match", name, contact[0]);
        assertEquals("Number should match", number, contact[1]);
    }

    @Test
    public void testGuardianInfoStorage() {
        String name = "Guardian Name";
        String number = "0987654321";
        
        // Store guardian info
        encryptionManager.storeGuardianInfo(name, number);
        
        // Retrieve guardian info
        String[] guardian = encryptionManager.retrieveGuardianInfo();
        
        assertEquals("Name should match", name, guardian[0]);
        assertEquals("Number should match", number, guardian[1]);
    }

    @Test
    public void testPerformanceMonitorInitialization() {
        assertNotNull("PerformanceMonitor should be initialized", performanceMonitor);
    }

    @Test
    public void testPerformanceMonitorStatus() {
        // Initially not monitoring
        assertFalse("Performance monitor should not be active initially", performanceMonitor.isMonitoringActive());
        
        // Start monitoring
        performanceMonitor.startMonitoring();
        
        // Should be active now
        assertTrue("Performance monitor should be active after starting", performanceMonitor.isMonitoringActive());
        
        // Stop monitoring
        performanceMonitor.stopMonitoring();
        
        // Should not be active anymore
        assertFalse("Performance monitor should not be active after stopping", performanceMonitor.isMonitoringActive());
    }

    @Test
    public void testPerformanceMonitorMemoryStats() {
        PerformanceMonitor.MemoryStats stats = performanceMonitor.getMemoryStats();
        
        // Verify stats are properly populated
        assertTrue("Total memory should be positive", stats.totalMemory > 0);
        assertTrue("Available memory should be positive", stats.availableMemory >= 0);
        assertTrue("Used memory should be positive", stats.usedMemory >= 0);
        assertTrue("Usage percentage should be between 0 and 100", 
                  stats.usagePercentage >= 0.0 && stats.usagePercentage <= 100.0);
    }

    @Test
    public void testBackupRestoreManagerInitialization() {
        assertNotNull("BackupRestoreManager should be initialized", backupRestoreManager);
    }

    @Test
    public void testUserFeedbackSystemInitialization() {
        assertNotNull("UserFeedbackSystem should be initialized", feedbackSystem);
    }

    @Test
    public void testUserFeedbackSubmission() {
        // Test that feedback can be submitted without throwing exceptions
        feedbackSystem.submitFeedback("Test feedback", UserFeedbackSystem.FeedbackCategory.GENERAL_FEEDBACK);
        
        // Test that satisfaction rating can be submitted
        feedbackSystem.submitSatisfactionRating(4);
    }

    @Test
    public void testBackupRestoreFlow() {
        // Test that backup can be created
        boolean backupResult = backupRestoreManager.createBackup();
        assertTrue("Backup should be created successfully", backupResult);
        
        // Get available backups
        java.io.File[] backups = backupRestoreManager.getAvailableBackups();
        assertTrue("Should have at least one backup", backups.length > 0);
        
        // Test file size formatting
        String fileSize = backupRestoreManager.getBackupFileSize(backups[0].getAbsolutePath());
        assertNotNull("File size should not be null", fileSize);
        assertFalse("File size should not be N/A for existing file", "N/A".equals(fileSize));
    }
}