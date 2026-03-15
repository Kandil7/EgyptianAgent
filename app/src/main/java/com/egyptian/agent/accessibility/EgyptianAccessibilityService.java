package com.egyptian.agent.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.content.Intent;
import android.os.Bundle;

import java.util.List;

/**
 * Egyptian Agent Accessibility Service.
 * Used for automating interactions with third-party apps like WhatsApp.
 * Specifically handles the "Click Send" action.
 */
public class EgyptianAccessibilityService extends AccessibilityService {
    private static final String TAG = "EgyptianAccessService";
    public static final String ACTION_CLICK_SEND = "com.egyptian.agent.action.CLICK_SEND";
    
    private static EgyptianAccessibilityService instance;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "Accessibility Service Connected");
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // We can monitor events here if needed, but primarily we react to our own intents
        // or specific window state changes if we want to be proactive.
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence packageName = event.getPackageName();
            if (packageName != null && packageName.toString().equals("com.whatsapp")) {
                Log.d(TAG, "WhatsApp window opened");
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "Accessibility Service Interrupted");
        instance = null;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_CLICK_SEND.equals(intent.getAction())) {
            String appPackage = intent.getStringExtra("package");
            if ("com.whatsapp".equals(appPackage)) {
                performWhatsAppSend();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void performWhatsAppSend() {
        Log.i(TAG, "Attempting to click WhatsApp send button...");
        
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "Root node is null");
            return;
        }

        // Try to find the send button by ID
        // WhatsApp Send button ID usually contains "send"
        List<AccessibilityNodeInfo> sendButtons = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send");
        
        if (sendButtons != null && !sendButtons.isEmpty()) {
            for (AccessibilityNodeInfo node : sendButtons) {
                if (node.isClickable()) {
                    Log.i(TAG, "Found send button by ID, clicking...");
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return;
                }
            }
        }
        
        // Fallback: Look by content description (often "Send")
        List<AccessibilityNodeInfo> nodesByDesc = rootNode.findAccessibilityNodeInfosByText("Send");
        // Also try Arabic "إرسال"
        nodesByDesc.addAll(rootNode.findAccessibilityNodeInfosByText("إرسال"));
        
        if (nodesByDesc != null && !nodesByDesc.isEmpty()) {
            for (AccessibilityNodeInfo node : nodesByDesc) {
                if (node.isClickable() || (node.getParent() != null && node.getParent().isClickable())) {
                    Log.i(TAG, "Found send button by description, clicking...");
                    if (node.isClickable()) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    } else {
                        node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                    return;
                }
            }
        }
        
        Log.w(TAG, "Could not find WhatsApp send button");
    }
    
    public static boolean isServiceEnabled() {
        return instance != null;
    }
    
    public static void triggerSendClick(Context context) {
        if (instance != null) {
            instance.performWhatsAppSend();
        } else {
            Log.w(TAG, "Service not running, cannot click send");
        }
    }
}
