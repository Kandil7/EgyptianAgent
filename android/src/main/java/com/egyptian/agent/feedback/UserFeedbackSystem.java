package com.egyptian.agent.feedback;

import android.content.Context;
import android.util.Log;

import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.utils.CrashLogger;
import com.egyptian.agent.executors.GuardianNotificationSystem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User feedback system for collecting user experiences and suggestions
 * Allows users to provide feedback about the application
 */
public class UserFeedbackSystem {
    private static final String TAG = "UserFeedbackSystem";
    private static final String FEEDBACK_DIRECTORY = "egyptian_agent_feedback";
    private static final String FEEDBACK_FILE = "user_feedback.txt";
    
    private static UserFeedbackSystem instance;
    private Context context;
    private ExecutorService executor;
    
    private UserFeedbackSystem(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    public static synchronized UserFeedbackSystem getInstance(Context context) {
        if (instance == null) {
            instance = new UserFeedbackSystem(context);
        }
        return instance;
    }
    
    /**
     * Submit user feedback
     */
    public void submitFeedback(String feedback, FeedbackCategory category) {
        executor.execute(() -> {
            try {
                // Save feedback to file
                saveFeedbackToFile(feedback, category);
                
                // Log the feedback
                Log.i(TAG, "User feedback submitted: " + feedback);
                
                // Provide positive feedback to user
                TTSManager.speak(context, "تم استلام ملاحظاتك. شكراً لمساعدتنا.");
                
                // For critical feedback, notify guardians
                if (category == FeedbackCategory.CRITICAL_ISSUE || category == FeedbackCategory.EMERGENCY_FEATURE) {
                    GuardianNotificationSystem.notifyGuardiansOfIssue(context, 
                        "User reported critical issue: " + feedback);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to submit feedback", e);
                CrashLogger.logError(context, e);
                
                // Inform user about failure
                TTSManager.speak(context, "فشل في إرسال الملاحظات. حاول تاني.");
            }
        });
    }
    
    /**
     * Save feedback to file
     */
    private void saveFeedbackToFile(String feedback, FeedbackCategory category) throws IOException {
        // Create feedback directory
        File feedbackDir = new File(context.getExternalFilesDir(null), FEEDBACK_DIRECTORY);
        if (!feedbackDir.exists()) {
            feedbackDir.mkdirs();
        }
        
        // Create or append to feedback file
        File feedbackFile = new File(feedbackDir, FEEDBACK_FILE);
        
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String feedbackEntry = String.format("[%s] [%s] %s%n", timestamp, category.name(), feedback);
        
        try (FileWriter writer = new FileWriter(feedbackFile, true)) { // Append mode
            writer.write(feedbackEntry);
        }
    }
    
    /**
     * Submit satisfaction rating
     */
    public void submitSatisfactionRating(int rating) {
        if (rating < 1 || rating > 5) {
            Log.w(TAG, "Invalid satisfaction rating: " + rating);
            return;
        }
        
        executor.execute(() -> {
            try {
                String feedback = "Satisfaction Rating: " + rating + "/5";
                saveFeedbackToFile(feedback, FeedbackCategory.USER_SATISFACTION);
                
                Log.i(TAG, "Satisfaction rating submitted: " + rating);
                
                // Respond based on rating
                if (rating >= 4) {
                    TTSManager.speak(context, "بشكرك على التقييم العالي. بنحاول نكون أحسن دايمًا.");
                } else {
                    TTSManager.speak(context, "بشكرك على التقييم. بنعتبره فرصة لتحسين نفسنا.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to submit satisfaction rating", e);
                CrashLogger.logError(context, e);
            }
        });
    }
    
    /**
     * Report a problem with the application
     */
    public void reportProblem(String problemDescription) {
        submitFeedback(problemDescription, FeedbackCategory.PROBLEM_REPORT);
    }
    
    /**
     * Suggest a new feature
     */
    public void suggestFeature(String featureSuggestion) {
        submitFeedback(featureSuggestion, FeedbackCategory.FEATURE_SUGGESTION);
    }
    
    /**
     * Report an issue with emergency features
     */
    public void reportEmergencyIssue(String issueDescription) {
        submitFeedback(issueDescription, FeedbackCategory.EMERGENCY_FEATURE);
    }
    
    /**
     * Provide general feedback
     */
    public void provideGeneralFeedback(String feedback) {
        submitFeedback(feedback, FeedbackCategory.GENERAL_FEEDBACK);
    }
    
    /**
     * Get feedback statistics
     */
    public FeedbackStats getFeedbackStats() {
        // This would normally read from the feedback file and calculate stats
        // For now, returning dummy stats
        return new FeedbackStats(0, 0, 0, 0);
    }
    
    /**
     * Feedback category enum
     */
    public enum FeedbackCategory {
        GENERAL_FEEDBACK,
        PROBLEM_REPORT,
        FEATURE_SUGGESTION,
        USER_SATISFACTION,
        CRITICAL_ISSUE,
        EMERGENCY_FEATURE
    }
    
    /**
     * Feedback statistics data class
     */
    public static class FeedbackStats {
        public final int totalFeedback;
        public final int problemReports;
        public final int featureSuggestions;
        public final int satisfactionCount;
        
        public FeedbackStats(int totalFeedback, int problemReports, int featureSuggestions, int satisfactionCount) {
            this.totalFeedback = totalFeedback;
            this.problemReports = problemReports;
            this.featureSuggestions = featureSuggestions;
            this.satisfactionCount = satisfactionCount;
        }
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}