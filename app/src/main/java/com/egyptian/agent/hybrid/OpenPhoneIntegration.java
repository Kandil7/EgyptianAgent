package com.egyptian.agent.hybrid;

import android.content.Context;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.nlp.IntentResult;
import com.egyptian.agent.stt.EgyptianNormalizer;
import com.egyptian.agent.utils.MemoryOptimizer;
import com.egyptian.agent.utils.CrashLogger;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenPhoneIntegration {
    private static final String TAG = "OpenPhoneIntegration";
    private static final int MODEL_LOAD_TIMEOUT = 30000; // 30 ثانية
    private static final float MIN_CONFIDENCE_THRESHOLD = 0.65f;

    private OpenPhoneModel localModel;
    private ExecutorService inferenceExecutor;
    private boolean isModelLoaded = false;
    private long lastInferenceTime = 0;

    public OpenPhoneIntegration(Context context) {
        inferenceExecutor = Executors.newSingleThreadExecutor();

        // تحميل النموذج في الخلفية
        new Thread(() -> {
            try {
                Log.i(TAG, "Loading OpenPhone-3B model...");
                localModel = new OpenPhoneModel(context, "openphone-3b");
                isModelLoaded = localModel.isReady();
                Log.i(TAG, "OpenPhone-3B model loaded successfully: " + isModelLoaded);

                // التحقق من ذاكرة الجهاز
                MemoryOptimizer.checkMemoryConstraints(context);

            } catch (Exception e) {
                Log.e(TAG, "Failed to load OpenPhone model", e);
                CrashLogger.logError(context, e);
                TTSManager.speak(context, "حصل مشكلة في تشغيل الذكاء المتقدم. المزايا الأساسية شغالة");
            }
        }).start();
    }

    /**
     * يحلل النص باستخدام نموذج OpenPhone المحلي
     * @param normalizedText النص المُوحد بعد التطبيع
     * @param callback رد الاتصال بالنتيجة
     */
    public void analyzeText(String normalizedText, AnalysisCallback callback) {
        if (!isModelLoaded) {
            Log.w(TAG, "Model not loaded yet, using fallback");
            callback.onFallbackRequired("Model still loading");
            return;
        }

        // التحقق من حدود الاستخدام
        if (System.currentTimeMillis() - lastInferenceTime < 1000) {
            Log.w(TAG, "Rate limiting active");
            callback.onFallbackRequired("High request frequency");
            return;
        }

        inferenceExecutor.execute(() -> {
            try {
                lastInferenceTime = System.currentTimeMillis();

                // قبل إرسال النص للنموذج، ن.apply قواعد مصرية إضافية
                String enhancedText = applyEgyptianEnhancements(normalizedText);

                // تشغيل النموذج
                long startTime = System.currentTimeMillis();
                Map<String, Object> result = localModel.analyze(enhancedText);
                long endTime = System.currentTimeMillis();

                Log.i(TAG, String.format("Inference completed in %d ms", endTime - startTime));

                // التحقق من ثقة النتيجة
                Float confidence = (Float) result.get("confidence");
                if (confidence == null || confidence < MIN_CONFIDENCE_THRESHOLD) {
                    callback.onFallbackRequired("Low confidence: " + (confidence != null ? confidence : 0.0f));
                    return;
                }

                // تحويل النتيجة لتنسيق موحد
                IntentResult intentResult = parseModelResult(result);

                // تطبيق قواعد مصرية على النتيجة
                applyEgyptianPostProcessing(intentResult);

                callback.onResult(intentResult);

            } catch (OutOfMemoryError e) {
                Log.e(TAG, "Out of memory during inference", e);
                MemoryOptimizer.freeMemory();
                callback.onFallbackRequired("Memory constraints");
            } catch (Exception e) {
                Log.e(TAG, "Error during inference", e);
                callback.onFallbackRequired("Processing error");
            }
        });
    }

    private String applyEgyptianEnhancements(String text) {
        // تعزيز النص بقواعد مصرية قبل إرساله للنموذج
        return EgyptianNormalizer.enhanceWithEgyptianContext(text);
    }

    private void applyEgyptianPostProcessing(IntentResult result) {
        // تعديل النتائج بناءً على القواعد المصرية
        EgyptianNormalizer.applyPostProcessingRules(result);

        // معالجة خاصة لأسماء الأشخاص المصريين
        if (result.getIntentType() == IntentType.CALL_CONTACT ||
            result.getIntentType() == IntentType.SEND_WHATSAPP) {
            String contactName = result.getEntity("contact", "");
            if (!contactName.isEmpty()) {
                result.setEntity("contact", EgyptianNormalizer.normalizeContactName(contactName));
            }
        }
    }

    private IntentResult parseModelResult(Map<String, Object> modelResult) {
        // تحويل نتيجة OpenPhone لنظام النوايا الخاص بنا
        IntentResult result = new IntentResult();

        // تحديد نوع النية
        String intentStr = (String) modelResult.get("intent");
        if (intentStr == null) intentStr = "UNKNOWN";
        result.setIntentType(IntentType.fromOpenPhoneString(intentStr));

        // استخراج الكيانات
        @SuppressWarnings("unchecked")
        Map<String, String> entities = (Map<String, String>) modelResult.get("entities");
        if (entities != null) {
            for (Map.Entry<String, String> entry : entities.entrySet()) {
                result.setEntity(entry.getKey(), entry.getValue());
            }
        }

        // تحديد مستوى الثقة
        Float confidence = (Float) modelResult.get("confidence");
        if (confidence != null) {
            result.setConfidence(confidence);
        } else {
            result.setConfidence(0.7f);
        }

        return result;
    }

    public boolean isReady() {
        return isModelLoaded;
    }

    public void destroy() {
        if (localModel != null) {
            localModel.unload();
        }
        if (inferenceExecutor != null) {
            inferenceExecutor.shutdownNow();
        }
    }

    public interface AnalysisCallback {
        void onResult(IntentResult result);
        void onFallbackRequired(String reason);
    }
}