package com.egyptian.agent.executor;

/**
 * Executor Result Container
 * 
 * Holds the result of command execution.
 */
public class ExecutorResult {
    private boolean success;
    private String message;
    private String action;
    private long executionTimeMs;
    
    /**
     * Create success result.
     */
    public ExecutorResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.action = "";
        this.executionTimeMs = 0;
    }
    
    /**
     * Create result with all fields.
     */
    public ExecutorResult(boolean success, String message, String action) {
        this.success = success;
        this.message = message;
        this.action = action;
        this.executionTimeMs = 0;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    /**
     * Create success result.
     */
    public static ExecutorResult success(String message) {
        return new ExecutorResult(true, message);
    }
    
    /**
     * Create success result with action.
     */
    public static ExecutorResult success(String message, String action) {
        return new ExecutorResult(true, message, action);
    }
    
    /**
     * Create error result.
     */
    public static ExecutorResult error(String message) {
        return new ExecutorResult(false, message);
    }
    
    @Override
    public String toString() {
        return "ExecutorResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", action='" + action + '\'' +
                '}';
    }
}
