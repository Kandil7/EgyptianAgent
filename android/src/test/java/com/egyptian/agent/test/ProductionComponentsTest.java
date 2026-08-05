package com.egyptian.agent.test;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
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
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
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
        // NOTE: isEncryptionReady() requires AndroidKeyStore, which Robolectric
        // does not provide (KeyStoreException: AndroidKeyStore not found), so
        // the manager runs in fallback mode in unit tests. Readiness can only be
        // verified on a device/emulator; the method's boolean contract is covered
        // by DataEncryptionManagerTest.
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
        assertFalse("Performance monitor should not be active initially", performanceMonitor.isMonitoring());
        
        // Start monitoring
        performanceMonitor.startMonitoring();
        
        // Should be active now
        assertTrue("Performance monitor should be active after starting", performanceMonitor.isMonitoring());
        
        // Stop monitoring
        performanceMonitor.stopMonitoring();
        
        // Should not be active anymore
        assertFalse("Performance monitor should not be active after stopping", performanceMonitor.isMonitoring());
    }

    @Test
    public void testPerformanceMonitorCleanup() {
        // Start and stop monitoring, then clean up (should not throw)
        performanceMonitor.startMonitoring();
        performanceMonitor.stopMonitoring();
        performanceMonitor.cleanup();

        assertFalse("Performance monitor should not be monitoring after cleanup", performanceMonitor.isMonitoring());
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