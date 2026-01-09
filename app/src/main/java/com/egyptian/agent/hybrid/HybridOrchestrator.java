package com.egyptian.agent.hybrid;

import android.content.Context;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.nlp.IntentResult;
import com.egyptian.agent.utils.SystemAppHelper;
import com.egyptian.agent.accessibility.SeniorMode;
import java.util.HashMap;
import java.util.Map;

public class HybridOrchestrator {
    private static final String TAG = "HybridOrchestrator";
    private static final Map<String, Float> TASK_COMPLEXITY_SCORES = new HashMap<>();
    private static final float LOCAL_MODEL_THRESHOLD = 0.7f;
    private static final float CLOUD_MODEL_THRESHOLD = 0.9f;

    static {
        // درجات تعقيد المهام المختلفة
        TASK_COMPLEXITY_SCORES.put("CALL_CONTACT", 0.4f);     // بسيطة
        TASK_COMPLEXITY_SCORES.put("READ_TIME", 0.3f);        // بسيطة جداً
        TASK_COMPLEXITY_SCORES.put("SET_ALARM", 0.5f);        // متوسطة البساطة
        TASK_COMPLEXITY_SCORES.put("SEND_WHATSAPP", 0.6f);    // متوسطة
        TASK_COMPLEXITY_SCORES.put("READ_EMAILS", 0.8f);      // معقدة
        TASK_COMPLEXITY_SCORES.put("SEARCH_INFO", 0.95f);     // معقدة جداً
    }

    private final Context context;
    private final OpenPhoneIntegration localModel;
    private final CloudFallback cloudFallback;
    private int localModelSuccessCount = 0;
    private int localModelFailureCount = 0;
    private boolean isLocalModelPreferred = true;

    public HybridOrchestrator(Context context) {
        this.context = context;
        this.localModel = new OpenPhoneIntegration(context);
        this.cloudFallback = new CloudFallback(context);
    }

    /**
     * يحدد ما إذا كان يجب استخدام النموذج المحلي أو السحابي
     * @param normalizedText النص المُوحد
     * @param callback رد الاتصال بالنية
     */
    public void determineIntent(String normalizedText, IntentCallback callback) {
        // التحقق من حالة النموذج المحلي
        if (!localModel.isReady()) {
            Log.w(TAG, "Local model not ready, using cloud fallback");
            cloudFallback.analyzeText(normalizedText, callback);
            return;
        }

        // تحليل تعقيد النص
        float complexityScore = calculateTextComplexity(normalizedText);

        // التحقق من أداء النموذج المحلي الأخير
        updateModelPreference();

        // تحديد المسار بناءً على القواعد
        if (shouldUseLocalModel(normalizedText, complexityScore)) {
            processWithLocalModel(normalizedText, complexityScore, callback);
        } else {
            processWithCloudModel(normalizedText, callback);
        }
    }

    private boolean shouldUseLocalModel(String text, float complexityScore) {
        // قواعد التوجيه الذكية
        if (text.contains("نجدة") || text.contains("استغاثة") || text.contains("طوارئ")) {
            // في حالات الطوارئ، نستخدم النموذج الأسرع (المحلي)
            return true;
        }

        if (SeniorMode.isEnabled()) {
            // في وضع كبار السن، نفضل النموذج المحلي للخصوصية
            return true;
        }

        if (!isLocalModelPreferred && complexityScore > LOCAL_MODEL_THRESHOLD) {
            // إذا كان النموذج المحلي غير مفضل والمهمة معقدة
            return false;
        }

        // بشكل افتراضي، نستخدم النموذج المحلي إذا كان التعقيد مقبول
        return complexityScore <= LOCAL_MODEL_THRESHOLD || isLocalModelPreferred;
    }

    private float calculateTextComplexity(String text) {
        // حساب تعقيد النص بناءً على عدة عوامل
        float lengthScore = Math.min(text.length() / 100.0f, 1.0f);
        float questionScore = text.contains("?") || text.contains("فين") || text.contains("ازاي") ? 0.3f : 0.0f;
        float unknownWordsScore = 0.0f;

        // التحقق من الكلمات غير المعروفة
        for (String word : text.split("\\s+")) {
            if (word.length() > 10 && !EgyptianNormalizer.isKnownWord(word)) {
                unknownWordsScore += 0.2f;
            }
        }

        return Math.min(lengthScore + questionScore + unknownWordsScore, 1.0f);
    }

    private void processWithLocalModel(String text, float complexityScore, IntentCallback callback) {
        Log.i(TAG, String.format("Using local model for text (complexity: %.2f): %s", complexityScore, text));

        localModel.analyzeText(text, new OpenPhoneIntegration.AnalysisCallback() {
            @Override
            public void onResult(IntentResult result) {
                localModelSuccessCount++;
                callback.onIntentDetected(result);

                // إرسال ملاحظات لأداء النموذج
                sendModelFeedback(text, result, true);
            }

            @Override
            public void onFallbackRequired(String reason) {
                localModelFailureCount++;
                Log.w(TAG, "Local model fallback required: " + reason);

                // المحاولة مع النموذج السحابي
                processWithCloudModel(text, callback);
            }
        });
    }

    private void processWithCloudModel(String text, IntentCallback callback) {
        Log.i(TAG, "Using cloud model for text: " + text);

        cloudFallback.analyzeText(text, result -> {
            // في وضع كبار السن، نقوم بتصفية الأوامر الحساسة
            if (SeniorMode.isEnabled() && !SeniorMode.isIntentAllowed(result.getIntentType())) {
                TTSManager.speak(context, "في وضع كبار السن، أنا بس أعرف أوامر محدودة. قول 'يا كبير' وأعلمك إياهم");
                result.setIntentType(IntentType.UNKNOWN);
            }

            callback.onIntentDetected(result);
        });
    }

    private void updateModelPreference() {
        int totalAttempts = localModelSuccessCount + localModelFailureCount;

        if (totalAttempts > 10) {
            float successRate = (float) localModelSuccessCount / totalAttempts;
            isLocalModelPreferred = successRate > 0.7f;
            Log.i(TAG, String.format("Model preference updated. Success rate: %.2f, Local preferred: %b",
                successRate, isLocalModelPreferred));

            // إعادة ضبط العدادات
            localModelSuccessCount = 0;
            localModelFailureCount = 0;
        }
    }

    private void sendModelFeedback(String text, IntentResult result, boolean wasSuccessful) {
        // في الإصدار الإنتاجي، هذه الملاحظات ستُستخدم لتحسين النموذج
        Log.d(TAG, String.format("Feedback: text='%s', intent=%s, success=%b",
            text, result.getIntentType(), wasSuccessful));
    }

    public interface IntentCallback {
        void onIntentDetected(IntentResult result);
    }

    public void destroy() {
        if (localModel != null) {
            localModel.destroy();
        }
        if (cloudFallback != null) {
            cloudFallback.destroy();
        }
    }
}