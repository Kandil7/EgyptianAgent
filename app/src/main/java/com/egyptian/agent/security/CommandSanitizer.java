package com.egyptian.agent.security;

import java.util.Set;

/**
 * Command Sanitizer
 * 
 * Security component that validates and sanitizes system commands
 * to prevent command injection and unauthorized system modifications.
 * 
 * Implements defense-in-depth by:
 * - Allowlisting only safe commands
 * - Stripping dangerous shell metacharacters
 * - Rate limiting command execution
 */
public class CommandSanitizer {
    
    private static final String TAG = "CommandSanitizer";
    
    // Allowlist of safe commands
    private static final Set<String> ALLOWED_COMMANDS = Set.of(
        "pm grant", "pm revoke",
        "settings put", "settings get",
        "svc wifi", "svc bluetooth",
        "am start -a android.intent.action.CALL"
    );
    
    // Rate limiting: max commands per time window
    private static final int MAX_COMMANDS_PER_WINDOW = 5;
    private static final long RATE_LIMIT_WINDOW_MS = 5 * 60 * 1000; // 5 minutes
    
    private static int commandCount = 0;
    private static long windowStart = 0;
    
    /**
     * Sanitizes and validates a command before execution
     * 
     * @param cmd The command to sanitize
     * @return The sanitized command
     * @throws SecurityException if command is not allowed or rate limit exceeded
     */
    public static String sanitize(String cmd) throws SecurityException {
        if (cmd == null || cmd.trim().isEmpty()) {
            throw new SecurityException("Empty command not allowed");
        }
        
        // Check rate limiting
        checkRateLimit();
        
        // Extract the base command (first word)
        String baseCommand = cmd.split(" ")[0].toLowerCase();
        
        // Check if command is in allowlist
        boolean isAllowed = false;
        for (String allowed : ALLOWED_COMMANDS) {
            if (allowed.startsWith(baseCommand)) {
                isAllowed = true;
                break;
            }
        }
        
        if (!isAllowed) {
            throw new SecurityException("Command not allowed: " + baseCommand);
        }
        
        // Strip dangerous shell metacharacters to prevent injection
        String sanitized = cmd.replaceAll("[;&|`$(){}\\[\\\\]<>\"'\\\\]", "");
        
        return sanitized;
    }
    
    /**
     * Checks if a command is in the allowlist without sanitizing
     * 
     * @param cmd The command to check
     * @return true if command is allowed, false otherwise
     */
    public static boolean isCommandAllowed(String cmd) {
        if (cmd == null || cmd.trim().isEmpty()) {
            return false;
        }
        
        String baseCommand = cmd.split(" ")[0].toLowerCase();
        
        for (String allowed : ALLOWED_COMMANDS) {
            if (allowed.startsWith(baseCommand)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks rate limiting for command execution
     * 
     * @throws SecurityException if rate limit exceeded
     */
    private static synchronized void checkRateLimit() throws SecurityException {
        long currentTime = System.currentTimeMillis();
        
        // Reset window if expired
        if (currentTime - windowStart > RATE_LIMIT_WINDOW_MS) {
            commandCount = 0;
            windowStart = currentTime;
        }
        
        // Check if limit exceeded
        if (commandCount >= MAX_COMMANDS_PER_WINDOW) {
            throw new SecurityException(
                "Rate limit exceeded: max " + MAX_COMMANDS_PER_WINDOW + 
                " commands per " + (RATE_LIMIT_WINDOW_MS / 1000 / 60) + " minutes"
            );
        }
        
        commandCount++;
    }
    
    /**
     * Resets the rate limiter (for testing or manual reset)
     */
    public static synchronized void resetRateLimit() {
        commandCount = 0;
        windowStart = 0;
    }
    
    /**
     * Gets remaining commands allowed in current window
     * 
     * @return Number of commands remaining
     */
    public static synchronized int getRemainingCommands() {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - windowStart > RATE_LIMIT_WINDOW_MS) {
            return MAX_COMMANDS_PER_WINDOW;
        }
        
        return Math.max(0, MAX_COMMANDS_PER_WINDOW - commandCount);
    }
}
