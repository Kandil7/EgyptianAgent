package com.egyptian.agent.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * Floating Listening View
 * 
 * A system overlay that appears when the agent is listening.
 * Designed for seniors: Large, high contrast, pulsing animation.
 */
public class FloatingListeningView {
    private final Context context;
    private final WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private boolean isShowing = false;
    private ObjectAnimator pulseAnimator;
    private Handler mainHandler;

    public FloatingListeningView(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
        createView();
    }

    private void createView() {
        // Create root layout
        FrameLayout rootLayout = new FrameLayout(context);
        rootLayout.setBackgroundColor(Color.TRANSPARENT);

        // Create the microphone bubble
        ImageView micIcon = new ImageView(context);
        micIcon.setImageResource(android.R.drawable.ic_btn_speak_now); // Standard mic icon
        micIcon.setBackgroundColor(0xFFFF6B35); // Egyptian warm orange
        micIcon.setPadding(30, 30, 30, 30);
        
        // Circular shape (programmatic)
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        shape.setColor(0xFFFF6B35);
        shape.setStroke(5, Color.WHITE);
        micIcon.setBackground(shape);

        // Layout params for the icon (Huge size: 96dp)
        int size = (int) (96 * context.getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(size, size);
        iconParams.gravity = Gravity.CENTER;
        rootLayout.addView(micIcon, iconParams);

        floatingView = rootLayout;

        // Setup pulsing animation
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                micIcon,
                PropertyValuesHolder.ofFloat("scaleX", 1.2f),
                PropertyValuesHolder.ofFloat("scaleY", 1.2f));
        pulseAnimator.setDuration(1000);
        pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        // Window Manager Params
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | 
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.y = 100; // Margin from bottom
    }

    public void show() {
        if (isShowing) return;
        
        mainHandler.post(() -> {
            try {
                windowManager.addView(floatingView, params);
                pulseAnimator.start();
                isShowing = true;
            } catch (Exception e) {
                // Permission might be denied
                e.printStackTrace();
            }
        });
    }

    public void hide() {
        if (!isShowing) return;

        mainHandler.post(() -> {
            try {
                pulseAnimator.cancel();
                windowManager.removeView(floatingView);
                isShowing = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    public void setState(boolean isProcessing) {
        // Change color or animation speed based on state
        // TODO: Implement processing state visual
    }
}
