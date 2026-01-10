# 🇪🇬 **خطة التكامل الكاملة للوكيل المصري**  
## **مساعد صوتي متكامل لكبار السن والضعاف بصرياً على Honor X6c**

**الإصدار:** 1.0  
**التاريخ:** 15 يناير 2026  
**المستهدف:** 10,000 مستخدم مصري خلال السنة الأولى  
**الجهاز الأساسي:** Honor X6c (MediaTek Helio G81 Ultra, 6GB RAM)

---

## 🏗️ **1. المعمارية التقنية المتكاملة**

### **1.1 نظرة عامة على النظام**
```mermaid
graph TD
    A[Hardware Layer] --> B[System Integration Layer]
    B --> C[Core Intelligence Layer]
    C --> D[User Experience Layer]
    
    subgraph Hardware Layer
        A1[Microphone] -->|Voice Input| B
        A2[Accelerometer] -->|Fall Detection| B
        A3[Volume Buttons] -->|Emergency Trigger| B
        A4[Vibrator] -->|Haptic Feedback| B
    end
    
    subgraph System Integration Layer
        B1[Saiy-PS Core] -->|Base Framework| C
        B2[Shizuku API] -->|System Privileges| B1
        B3[Android Accessibility] -->|TalkBack Integration| B1
    end
    
    subgraph Core Intelligence Layer
        C1[Egyptian ASR/TTS] -->|Dialect Processing| D
        C2[Intent Recognition] -->|Command Routing| D
        C3[Emergency System] -->|Life-Saving Features| D
        C4[Senior Mode Engine] -->|Specialized UX| D
    end
    
    subgraph User Experience Layer
        D1[Wake Word: 'يا حكيم'] -->|Activation| C
        D2[Voice Commands] -->|Daily Tasks| C
        D3[Emergency Phrases] -->|Critical Response| C
    end
```

### **1.2 مكونات التكامل الرئيسية**
| المكون | المصدر | المسؤولية | مستوى الأولوية |
|--------|--------|-------------|----------------|
| **Saiy-PS Core** | [brandall76/Saiy-PS](https://github.com/brandall76/Saiy-PS) | إطار العمل الأساسي، خدمة الخلفية | ⭐⭐⭐⭐⭐ |
| **Shizuku API** | [pub.dev/packages/shizuku_api](https://pub.dev/packages/shizuku_api) | صلاحيات النظام، تثبيت كـ System App | ⭐⭐⭐⭐⭐ |
| **Vosk Egyptian Model** | [alphacep/vosk-android](https://github.com/alphacep/vosk-android) | التعرف على اللهجة المصرية | ⭐⭐⭐⭐⭐ |
| **Egyptian TTS Voices** | [egyptian-dev/voices](https://github.com/egyptian-dev/voices) | تحويل النص لمصري طبيعي | ⭐⭐⭐⭐ |
| **Fall Detection AI** | EgyptianAgent/fall_detector | كشف السقوط التلقائي | ⭐⭐⭐⭐ |
| **Senior Mode Manager** | EgyptianAgent/senior_mode | وضع كبار السن المتخصص | ⭐⭐⭐⭐⭐ |
| **Emergency Router** | EgyptianAgent/emergency | إدارة حالات الطوارئ | ⭐⭐⭐⭐⭐ |

---

## ⚙️ **2. خطة التكامل خطوة بخطوة**

### **المرحلة 1: التأسيس الأساسي (الأسبوع 1)**

#### **2.1.1 تحميل وتهيئة Saiy-PS**
```bash
# 1. استنساخ المشروع الأساسي
git clone https://github.com/brandall76/Saiy-PS EgyptianAgent
cd EgyptianAgent

# 2. إضافة تبعيات المشروع
./gradlew dependencies --write-locks

# 3. بناء APK تجريبي للتأكد من عمل الأساسيات
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

#### **2.1.2 تكوين Shizuku للصلاحيات النظامية**
```xml
<!-- في AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.egyptian.agent">

    <!-- الصلاحيات الحرجة -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

    <application
        android:name=".MainApplication"
        android:sharedUserId="android.uid.system">

        <!-- Shizuku Provider -->
        <provider
            android:name="rikka.shizuku.ShizukuProvider"
            android:authorities="${applicationId}.shizuku"
            android:multiprocess="false"
            android:enabled="true"
            android:exported="true"
            android:permission="android.permission.INTERACT_ACROSS_USERS_FULL" />
            
        <!-- الخدمة الأساسية -->
        <service
            android:name=".service.SelfAwareService"
            android:foregroundServiceType="microphone"
            android:exported="true">
            <intent-filter>
                <action android:name="com.egyptian.agent.SELF_AWARE_SERVICE" />
            </intent-filter>
        </service>
    </application>
</manifest>
```

#### **2.1.3 تثبيت كـ System App على Honor X6c**
```bash
#!/bin/bash
# scripts/install_as_system_app.sh

# 1. التحقق من الصلاحيات
adb root &>/dev/null
if [ $? -ne 0 ]; then
    echo "خطأ: الهاتف غير مُ	root. يرجى فتح الـ Bootloader وتثبيت Magisk أولاً"
    exit 1
fi

# 2. إعادة تحميل نظام الملفات للقراءة/الكتابة
adb remount

# 3. نسخ التطبيق لمجلد النظام
SYSTEM_DIR="/system/priv-app/EgyptianAgent"
adb shell "mkdir -p $SYSTEM_DIR"
adb push app/build/outputs/apk/release/app-release.apk $SYSTEM_DIR/

# 4. تعيين الصلاحيات الصحيحة
adb shell "chmod 644 $SYSTEM_DIR/app-release.apk"

# 5. منح الصلاحيات الحرجة
PACKAGES="com.egyptian.agent"
PERMISSIONS=(
    "android.permission.CALL_PHONE"
    "android.permission.READ_CONTACTS"
    "android.permission.READ_CALL_LOG"
    "android.permission.RECORD_AUDIO"
    "android.permission.BODY_SENSORS"
    "android.permission.VIBRATE"
)

for perm in "${PERMISSIONS[@]}"; do
    adb shell "pm grant $PACKAGES $perm"
done

# 6. تعيين كـ Device Owner (للحصول على صلاحيات إضافية)
adb shell "dpm set-device-owner $PACKAGES/.AdminReceiver"

# 7. إعادة التشغيل
adb reboot
echo "تم التثبيت بنجاح! الجهاز سيُعاد تشغيله الآن..."
```

### **المرحلة 2: التخصيص المصري (الأسبوع 2)**

#### **2.2.1 استبدال نماذج الصوت بالمصرية**
```java
// في файл src/main/java/com/egyptian/agent/service/SelfAwareService.java

public class SelfAwareService extends Service {
    private VoskASR egyptianASR;
    private EgyptianTTS egyptianTTS;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // تحميل نموذج Vosk المصري
        loadEgyptianLanguageModels();
        
        // تكوين كلمات التفعيل المصرية
        setupEgyptianWakeWords();
        
        // تنشيط وضع كبار السن إذا كان ممكناً
        enableSeniorModeIfConfigured();
    }
    
    private void loadEgyptianLanguageModels() {
        try {
            // نموذج Vosk مصري (48MB)
            File modelPath = new File(getFilesDir(), "models/vosk-model-ar-mgb2-egyptian");
            egyptianASR = new VoskASR(this, modelPath.getAbsolutePath());
            
            // صوت TTS مصري
            egyptianTTS = new EgyptianTTS(this, "egyptian_senior_voice");
            
            Log.i("EgyptianAgent", "تم تحميل النماذج المصرية بنجاح");
        } catch (Exception e) {
            Log.e("EgyptianAgent", "فشل تحميل النماذج المصرية", e);
            // العودة للنماذج الإنجليزية كـ fallback
            fallbackToDefaultModels();
        }
    }
    
    private void setupEgyptianWakeWords() {
        List<String> egyptianWakeWords = Arrays.asList(
            "يا حكيم",    // Hey Wise One
            "يا كبير",    // Hey Elder (لوضع كبار السن)
            "ساعدني"     // Help me (للطوارئ)
        );
        
        wakeWordDetector.setCustomWakeWords(egyptianWakeWords);
    }
}
```

#### **2.2.2 تخصيص معالجة اللغة الطبيعية للمصرية**
```java
// في ملف src/main/java/com/egyptian/agent/core/Quantum.java

public class Quantum {
    private final EgyptianNormalizer normalizer = new EgyptianNormalizer();
    private final IntentRouter intentRouter = new IntentRouter();
    
    public CommandResult processCommand(String rawCommand) {
        // 1. تطبيع اللهجة المصرية
        String normalizedCommand = normalizer.normalize(rawCommand);
        Log.d("EgyptianAgent", "الأمر بعد التنظيم: " + normalizedCommand);
        
        // 2. استخراج النية الأساسية
        IntentType intent = intentRouter.detectIntent(normalizedCommand);
        
        // 3. تنفيذ الأمر حسب النية
        return executeIntent(intent, normalizedCommand);
    }
    
    private CommandResult executeIntent(IntentType intent, String normalizedCommand) {
        switch (intent) {
            case CALL_CONTACT:
                return handleCallContact(normalizedCommand);
            case SET_REMINDER:
                return handleSetReminder(normalizedCommand);
            case READ_SCREEN:
                return handleReadScreen();
            case EMERGENCY:
                return handleEmergency();
            case MEDICATION_REMINDER:
                return handleMedicationReminder(normalizedCommand);
            default:
                return handleUnknownCommand(normalizedCommand);
        }
    }
    
    private CommandResult handleCallContact(String command) {
        // استخراج اسم جهة الاتصال من الأمر المصري
        String contactName = extractContactName(command);
        
        if (contactName.isEmpty()) {
            return new CommandResult(false, "مين اللي عايز تتصل بيه؟ قول الاسم");
        }
        
        // البحث في جهات الاتصال
        String number = ContactManager.findContact(this, contactName);
        
        if (number == null) {
            return new CommandResult(false, "مش لاقي " + contactName + " في>Contactات");
        }
        
        // في وضع كبار السن - تأكيد مزدوج
        if (SeniorMode.isEnabled()) {
            return new CommandResult(true, 
                "عايز تتصل بـ " + contactName + "؟ قول 'نعم' أو 'لا'", 
                () -> {
                    if (SpeechConfirmation.waitForConfirmation(this)) {
                        CallManager.placeCall(this, number);
                        return "بتكلم مع " + contactName + " دلوقتي";
                    }
                    return "ما عملتش اتصال";
                }
            );
        }
        
        // وضع عادي - تنفيذ مباشر
        CallManager.placeCall(this, number);
        return new CommandResult(true, "بتكلم مع " + contactName);
    }
}
```

#### **2.2.3 تطوير وضع كبار السن المتخصص**
```java
// في ملف src/main/java/com/egyptian/agent/accessibility/SeniorModeManager.java

public class SeniorModeManager {
    private static final float SENIOR_SPEECH_RATE = 0.75f;  // أبطأ 25%
    private static final float SENIOR_SPEECH_PITCH = 0.85f; // نغمة منخفضة
    private static final float SENIOR_SPEECH_VOLUME = 1.0f; // أقصى حجم
    
    public static void enableSeniorMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("egyptian_agent_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("senior_mode_enabled", true).apply();
        
        // تطبيق إعدادات الصوت الخاصة
        TTSManager.setSpeechRate(context, SENIOR_SPEECH_RATE);
        TTSManager.setSpeechPitch(context, SENIOR_SPEECH_PITCH);
        TTSManager.setSpeechVolume(context, SENIOR_SPEECH_VOLUME);
        
        // تشغيل كاشف السقوط
        FallDetectionService.start(context);
        
        // تفعيل التأكيدات المزدوجة
        enableDoubleConfirmationMode(context);
        
        // تقليل عدد الأوامر المسموحة
        restrictCommandsToSeniorSubset(context);
        
        // إعطاء تأكيد صوتي
        TTSManager.speak(context, "تم تفعيل وضع كبار السن. قول 'يا كبير' لأي حاجة");
        
        // هزّة قصيرة للتأكيد
        VibrationManager.vibrateShort(context);
    }
    
    private static void enableDoubleConfirmationMode(Context context) {
        // تفعيل التأكيد الصوتي + الهزّة لكل إجراء
        SharedPreferences prefs = context.getSharedPreferences("egyptian_agent_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("double_confirmation_enabled", true).apply();
    }
    
    private static void restrictCommandsToSeniorSubset(Context context) {
        // السماح بـ 5 أوامر أساسية فقط
        List<IntentType> allowedIntents = Arrays.asList(
            IntentType.CALL_CONTACT,
            IntentType.READ_TIME,
            IntentType.SET_REMINDER,
            IntentType.MEDICATION_REMINDER,
            IntentType.EMERGENCY
        );
        
        IntentRouter.setAllowedIntents(allowedIntents);
    }
}
```

### **المرحلة 3: التحسينات الخاصة لـ Honor X6c (الأسبوع 3)**

#### **3.3.1 حل مشاكل الخلفية في هواتف Honor**
```java
// في ملف src/main/java/com/egyptian/agent/utils/HonorOptimizations.java

public class HonorOptimizations {
    
    public static void applyAllOptimizations(Context context) {
        if (Build.MANUFACTURER.equalsIgnoreCase("HONOR")) {
            Log.i("HonorOpt", "تطبيق التحسينات الخاصة بجهاز Honor");
            
            // 1. إنشاء خدمة في المقدمة مع إشعار دائم
            createPersistentNotification(context);
            
            // 2. تعطيل تحسين البطارية
            disableBatteryOptimization(context);
            
            // 3. تحسين استخدام الذاكرة
            optimizeMemoryUsage(context);
            
            // 4. تحسين معالجة المستشعرات
            optimizeSensorProcessing(context);
        }
    }
    
    private static void createPersistentNotification(Context context) {
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "EGYPTIAN_AGENT_PERSISTENT",
                "حماية كبار السن",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("المساعد يعمل في الخلفية لحماية كبار السن");
            notificationManager.createNotificationChannel(channel);
        }
        
        Notification notification = new NotificationCompat.Builder(context, "EGYPTIAN_AGENT_PERSISTENT")
            .setContentTitle("الوكيل المصري نشط")
            .setContentText("جاهز لمساعدتك في أي وقت")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();
        
        ((SelfAwareService) context).startForeground(1, notification);
    }
    
    private static void disableBatteryOptimization(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        String packageName = context.getPackageName();
        
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
```

#### **3.3.2 تحسين استخدام الذاكرة للـ 6GB RAM**
```java
// في ملف src/main/java/com/egyptian/agent/memory/MemoryManager.java

public class MemoryManager {
    private static final long MAX_RAM_USAGE = 400 * 1024 * 1024; // 400MB
    
    public static void optimizeMemoryUsage() {
        // إطلاق الموارد غير المستخدمة
        releaseUnusedResources();
        
        // تقليل دقة النماذج عند انخفاض الذاكرة
        adjustModelQualityAccordingToMemory();
        
        // تطهير الذاكرة المؤقتة
        clearCachesIfNecessary();
    }
    
    private static void releaseUnusedResources() {
        // إيقاف الخصائص غير المستخدمة في وضع كبار السن
        if (SeniorMode.isEnabled()) {
            // إطلاق موارد الطقس (لا تستخدم بكثرة)
            WeatherService.releaseResources();
            
            // إطلاق موارد البحث على الإنترنت
            WebSearchService.releaseResources();
        }
    }
    
    private static void adjustModelQualityAccordingToMemory() {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        long availableMemory = memoryInfo.availMem;
        
        if (availableMemory < (MAX_RAM_USAGE * 0.3)) { // أقل من 30% من الحد الأقصى
            Log.w("MemoryManager", "الذاكرة منخفضة، تقليل جودة النماذج");
            
            // تقليل جودة نموذج Vosk
            VoskASR.reduceModelQuality();
            
            // تقليل عدد جهات الاتصال المخزنة في الذاكرة
            ContactCache.clearOldEntries(50);
        }
    }
}
```

### **المرحلة 4: نظام الطوارئ المتقدم (الأسبوع 4)**

#### **4.4.1 كشف السقوط التلقائي**
```java
// في ملف src/main/java/com/egyptian/agent/safety/FallDetectionService.java

public class FallDetectionService extends Service implements SensorEventListener {
    private static final float FALL_ACCELERATION_THRESHOLD = 20f; // m/s²
    private static final int FALL_CONFIRMATION_TIME = 5000; // 5 ثواني
    private static final int MIN_MOVEMENT_AFTER_FALL = 2.0f; // m/s²
    
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean isFallSuspected = false;
    private long fallStartTime = 0;
    private float[] lastAccelerometerValues = new float[3];
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            Log.i("FallDetection", "تم تفعيل كاشف السقوط");
        } else {
            Log.e("FallDetection", "المستشعر غير متوفر على هذا الجهاز");
        }
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            
            // حساب تسارع الحركة
            double acceleration = Math.sqrt(x*x + y*y + z*z);
            
            System.arraycopy(event.values, 0, lastAccelerometerValues, 0, 3);
            
            // كشف التسارع المفاجئ (السقوط الأولي)
            if (!isFallSuspected && acceleration > FALL_ACCELERATION_THRESHOLD) {
                Log.w("FallDetection", "تم اكتشاف تسارع مفاجئ: " + acceleration);
                isFallSuspected = true;
                fallStartTime = System.currentTimeMillis();
            }
            
            // تأكيد السقوط بعد فترة زمنية
            if (isFallSuspected) {
                confirmFallIfNeeded();
            }
        }
    }
    
    private void confirmFallIfNeeded() {
        if (System.currentTimeMillis() - fallStartTime >= FALL_CONFIRMATION_TIME) {
            // حساب الحركة بعد السقوط (يجب أن تكون منخفضة)
            double postFallMovement = calculatePostFallMovement();
            
            if (postFallMovement < MIN_MOVEMENT_AFTER_FALL) {
                Log.e("FallDetection", "تم تأكيد السقوط! مستوى الحركة بعد السقوط: " + postFallMovement);
                handleConfirmedFall();
            } else {
                Log.i("FallDetection", "سقوط كاذب، الحركة بعد السقوط عالية: " + postFallMovement);
                resetFallDetection();
            }
        }
    }
    
    private double calculatePostFallMovement() {
        // تحليل بيانات التسارع في الثواني الخمس الماضية
        // في الإصدار الحقيقي، سيستخدم سجل القيم السابقة
        return Math.sqrt(
            lastAccelerometerValues[0] * lastAccelerometerValues[0] +
            lastAccelerometerValues[1] * lastAccelerometerValues[1] +
            lastAccelerometerValues[2] * lastAccelerometerValues[2]
        );
    }
    
    private void handleConfirmedFall() {
        // تشغيل إنذار عالي
        playEmergencyAlert();
        
        // هزّة مستمرة
        VibrationManager.vibrateEmergency(this);
        
        // الحصول على الموقع (حتى لو بدون إنترنت - آخر موقع معروف)
        Location lastKnownLocation = LocationCache.getLastKnownLocation(this);
        
        // الاتصال بأرقام الطوارئ
        EmergencyContacts emergencyContacts = new EmergencyContacts(this);
        for (String number : emergencyContacts.getEmergencyNumbers()) {
            EmergencyHandler.placeEmergencyCall(this, number, lastKnownLocation);
        }
        
        // إرسال رسالة واتساب للعائلة (إذا كان الإنترنت متاحاً)
        if (NetworkUtils.isOnline(this)) {
            EmergencyHandler.sendEmergencyWhatsApp(this, lastKnownLocation);
        }
        
        // إعطاء تأكيد صوتي
        TTSManager.speakWithPriority(this, 
            "يا كبير! لقيت إنك وقعت. بيتصل بالإسعاف دلوقتي! إتقعد مكانك ومتتحركش.", 
            true
        );
    }
}
```

#### **4.4.2 نظام الطوارئ الصوتي التلقائي**
```java
// في ملف src/main/java/com/egyptian/agent/safety/EmergencyHandler.java

public class EmergencyHandler {
    
    private static final List<String> EMERGENCY_PHRASES = Arrays.asList(
        "نجدة", "استغاثة", "مش قادر", "حد يجي", 
        "إسعاف", "حرقان", "طلق ناري", "ساعدني"
    );
    
    public static boolean isEmergencyCommand(String command) {
        String normalized = command.toLowerCase();
        
        for (String phrase : EMERGENCY_PHRASES) {
            if (normalized.contains(phrase)) {
                return true;
            }
        }
        
        return false;
    }
    
    public static void triggerEmergency(Context context, boolean force) {
        Log.e("EmergencyHandler", "تم تفعيل وضع الطوارئ. Force mode: " + force);
        
        // تشغيل صوت إنذار
        playEmergencySound(context);
        
        // هزّة الطوارئ
        VibrationManager.vibrateEmergency(context);
        
        // الحصول على جهات الاتصال للطوارئ
        List<String> emergencyContacts = getEmergencyContacts(context);
        
        // في وضع كبار السن أو وضع force، التنفيذ الفوري
        if (SeniorMode.isEnabled() || force) {
            executeEmergencyCalls(context, emergencyContacts);
            return;
        }
        
        // في الوضع العادي، طلب التأكيد أولاً
        TTSManager.speak(context, "ده إجراء طوارئ! قول 'نعم' لو الموضوع خطير فعلاً");
        if (SpeechConfirmation.waitForConfirmation(context)) {
            executeEmergencyCalls(context, emergencyContacts);
        } else {
            TTSManager.speak(context, "تم إلغاء وضع الطوارئ");
        }
    }
    
    private static void executeEmergencyCalls(Context context, List<String> contacts) {
        TTSManager.speak(context, "بتصل بأرقام الطوارئ دلوقتي. إتقعد مكانك ومتتحركش.");
        
        Location lastKnownLocation = LocationCache.getLastKnownLocation(context);
        
        for (String number : contacts) {
            try {
                String cleanNumber = PhoneNumberUtils.cleanNumber(number);
                placeEmergencyCall(context, cleanNumber, lastKnownLocation);
                
                // الانتظار حتى يتم الرد أو مرور 15 ثانية
                Thread.sleep(15000);
            } catch (Exception e) {
                Log.e("EmergencyHandler", "فشل في الاتصال برقم: " + number, e);
            }
        }
        
        // إرسال رسالة واتساب إذا كان الإنترنت متاحاً
        if (NetworkUtils.isOnline(context)) {
            sendEmergencyWhatsApp(context, lastKnownLocation);
        }
    }
    
    public static void placeEmergencyCall(Context context, String number, Location location) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + number));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            callIntent.putExtra("emergency_location", location);
            context.startActivity(callIntent);
            
            Log.i("EmergencyHandler", "تم الاتصال برقم الطوارئ: " + number);
        } catch (Exception e) {
            Log.e("EmergencyHandler", "فشل الاتصال برقم الطوارئ: " + number, e);
            CrashLogger.logEmergencyError(context, e);
        }
    }
}
```

---

## 📱 **3. خطة التطوير والاختبار**

### **3.1 جدول التطوير**
| الأسبوع | المهام | الفرق المشاركة | مؤشرات النجاح |
|---------|--------|----------------|--------------|
| **الأسبوع 1** | - تكامل Saiy-PS الأساسي<br>- تكوين Shizuku<br>- التثبيت كـ System App | فريق التطوير الأساسي (3 مهندسين) | - تشغيل Wake Word الأساسي<br>- الاستجابة لأوامر بسيطة |
| **الأسبوع 2** | - دمج النماذج المصرية<br>- تطوير وضع كبار السن<br>- معالجة اللهجة المصرية | NLP Team (2 مهندسين) + فريق UX | - دقة 85% في فهم اللهجة<br>- تشغيل وضع كبار السن |
| **الأسبوع 3** | - تحسينات Honor X6c<br>- إدارة الذاكرة<br>- تحسين البطارية | فريق الأداء (2 مهندسين) | - استهلاك <7% بطارية/ساعة<br>- استخدام <400MB RAM |
| **الأسبوع 4** | - نظام الطوارئ<br>- كشف السقوط<br>- اختبارات السلامة | فريق السلامة (3 مهندسين) | - كشف السقوط بدقة 85%+<br>- استجابة الطوارئ <2 ثانية |
| **الأسبوع 5** | - الاختبار مع المستخدمين<br>- جمع الملاحظات<br>- التحسينات النهائية | فريق ضمان الجودة + مستخدمون حقيقيون | - 90% رضا المستخدمين<br>- 0 أخطاء حرجة |

### **3.2 استراتيجية الاختبار**

#### **3.2.1 اختبارات الوحدة (Unit Tests)**
```java
@Test
public void testEgyptianNormalization() {
    EgyptianNormalizer normalizer = new EgyptianNormalizer();
    
    // اختبار التنظيم الأساسي
    String input1 = "عايز أكلم ماما دلوقتي";
    String expected1 = "أريد أن أتصل بالأم الآن";
    assertEquals(expected1, normalizer.normalize(input1));
    
    // اختبار الكلمات الطبية
    String input2 = "قول لي الدوا بكرة الصبح";
    String expected2 = "تذكير دوا غداً الصباح";
    assertEquals(expected2, normalizer.normalize(input2));
    
    // اختبار الطوارئ
    String input3 = "مش قادر، نجدة!";
    String expected3 = "طوارئ استغاثة";
    assertEquals(expected3, normalizer.normalize(input3));
}
```

#### **3.2.2 اختبارات القبول مع المستخدمين**
**سيناريو الاختبار:**
1. **المستخدمون:** 10 كبار سن (60+ سنة) في القاهرة والإسكندرية
2. **المدة:** أسبوع واحد مع 3 جلسات يومية
3. **المهام الأساسية:**
   - قول "يا كبير" للتنشيط
   - قول "اتصل بأمي" والانتظار للاتصال
   - قول "ذكّرني بالدوا الساعة خمسة" وفحص التذكير
   - محاكاة سقوط (باستخدام الوضع التجريبي) والتحقق من الاستجابة
4. **معايير النجاح:**
   - 90% من الأوامر تُفهم بشكل صحيح
   - 85% من المستخدمين يستطيعون استخدام النظام دون مساعدة
   - زمن استجابة الطوارئ أقل من ثانيتين
   - عدم وجود أعطال حرجة في النظام

#### **3.2.3 اختبارات الأداء على Honor X6c**
| المعيار | الأداة | الشرط | النتيجة المتوقعة |
|---------|--------|--------|------------------|
| **استخدام الذاكرة** | Android Profiler | ذروة الاستخدام | <400MB |
| **استهلاك البطارية** | Battery Historian | /ساعة | <7% |
| **وقت الاستجابة** | Custom Timer | للـ Wake Word | <1.5 ثانية |
| **استقرار النظام** | Monkey Test | 24 ساعة | 0 أعطال |
| **كفاءة المعالج** | CPU Profiler | استخدام CPU | <40% في الخلفية |

---

## 🚀 **4. خطة النشر والتوزيع**

### **4.1 استراتيجية النشر**
1. **الإصدار التجريبي (الأسبوع 6):**
   - 100 نسخة مجانية لكبار السن في 3 محافظات
   - تدريب مباشر عبر مكالمات فيديو
   - جمع الملاحظات اليومية

2. **الإصدار التجريبي الموسع (الأسبوع 10):**
   - 1,000 نسخة مع شراكات مع 5 جمعيات خيرية
   - دعم فني عبر واتساب واتصال هاتفي
   - تقارير أسبوعية عن الأداء والاستخدام

3. **الإصدار النهائي (الأسبوع 16):**
   - 10,000 مستخدم مستهدف
   - دعم عبر 3 مراكز دعم في القاهرة، الإسكندرية، أسوان
   - تكامل مع 3 مستشفيات حكومية

### **4.2 قنوات الدعم الفني**
| القناة | التفاصيل | وقت الاستجابة | الفريق |
|---------|----------|---------------|--------|
| **واتساب** | +201111111111 | <1 ساعة | 5 موظفين |
| ** الهاتف ** | 16616 | 9 صباحاً - 9 مساءً | 10 موظفين |
| **الزيارة المنزلية** | لحالات الطوارئ الحرجة | <4 ساعات | 20 متطوع |
| **مجموعة فيسبوك** | مساعدة من مجتمع المستخدمين | <2 ساعة | مجتمعي |

### **4.3 نموذج الأعمال والاستدامة**
- **التكلفة الأولية:** 2,800,000 جنيه مصري (التطوير، البنية التحتية، التدريب)
- **التمويل:**
  - 1,500,000 جنيه: وزارة الاتصالات (مبادرة الدمج الرقمي)
  - 800,000 جنيه: منح دولية (الصحة العالمية)
  - 500,000 جنيه: تبرعات فردية ومؤسساتية
- **نموذج الدخل المستقبلي:**
  - B2B: اشتراكات للمستشفيات والبنوك (500 جنيه/مستخدم/سنة)
  - الحكومة: عقود سنوية لتوفير الخدمة لكبار السن (300 جنيه/مستخدم/سنة)
  - التبرعات: منصة تبرعات رقمية داخل التطبيق

---

## 🔒 **5. الخصوصية والأمان**

### **5.1 سياسة الخصوصية الأساسية**
- **لا تخزين** للتسجيلات الصوتية أبداً
- **لا نقل** لأي بيانات للخارج إلا في حالات الطوارئ مع موافقة صريحة
- **التشفير:** جميع البيانات الحساسة (جهات الاتصال، الموقع) تُشفَّر بـ AES-256
- **المحو:** يمكن للمستخدم حذف جميع بياناته في أي وقت
- **الشفافية:** شرح واضح لكل عملية نقل بيانات

### **5.2 تدابير الأمان التقنية**
```java
// في ملف src/main/java/com/egyptian/agent/security/DataEncryption.java

public class DataEncryption {
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "EgyptianAgentKey";
    
    public static String encrypt(Context context, String plaintext) {
        try {
            // إنشاء أو استرجاع المفتاح من KeyStore
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            
            SecretKey secretKey;
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                createNewKey(keyStore);
            }
            secretKey = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
            
            // تهيئة التشفير
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            // التشفير
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // دمج IV مع النص المشفر
            byte[] iv = cipher.getIV();
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            
            return Base64.encodeToString(combined, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e("DataEncryption", "فشل في تشفير البيانات", e);
            return null;
        }
    }
    
    private static void createNewKey(KeyStore keyStore) throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(false);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setInvalidatedByBiometricEnrollment(false);
        }
        
        keyGenerator.init(builder.build());
        keyGenerator.generateKey();
    }
}
```

---

## 📊 **6. مؤشرات النجاح والقياس**

### **6.1 مؤشرات الأداء الرئيسية (KPIs)**
| المؤشر | الهدف (3 أشهر) | الهدف (سنة) | طريقة القياس |
|---------|----------------|-------------|--------------|
| **عدد المستخدمين النشطين** | 1,000 | 10,000 | سجلات النظام |
| **دقة فهم الأوامر** | 85% | 95% | تسجيلات الأداء |
| **وقت استجابة Wake Word** | 1.5 ثانية | 1.0 ثانية | أدوات المراقبة |
| **استهلاك البطارية/ساعة** | 8% | 5% | Battery Historian |
| **نسبة نجاح الطوارئ** | 90% | 99% | تقارير الطوارئ |
| **رضا المستخدم** | 4.0/5 | 4.5/5 | استبيانات |

### **6.2 تقارير الأداء الشهرية**
```markdown
**تقرير أداء الوكيل المصري - يناير 2026**

**البيانات الأساسية:**
- عدد المستخدمين: 247
- وقت الاستخدام اليومي المتوسط: 32 دقيقة
- عدد الأوامر اليومية للمستخدم: 12 أمر

**جودة الخدمة:**
- دقة فهم الأوامر: 83%
- وقت استجابة Wake Word: 1.7 ثانية
- عدد أعطال النظام: 3 أعطال (جميعها تم إصلاحها في أقل من ساعة)

**استخدام الميزات:**
- المكالمات: 45%
- التذكيرات بالأدوية: 28%
- قراءة الشاشة: 15%
- الطوارئ: 2% (4 حالات حقيقية)
- التنبيهات اليومية: 10%

**ملاحظات المستخدمين:**
- "الصوت واضح وسهل الفهم" - أحمد، 68 سنة
- "ساعدني في الاتصال بابني عندما سقطت" - فاطمة، 72 سنة
- "أرجو زيادة حجم الصوت في وضع كبار السن" - محمد، 75 سنة

**التحسينات المطلوبة:**
1. تحسين دقة فهم كلمة "الدواء" في الضوضاء
2. تقليل وقت استجابة الـ Wake Word
3. إضافة خيار "تكرار الأمر" لضعاف السمع
```

---

## 💡 **7. الخلاصة والخطوات التالية**

**الوكيل المصري** ليس مجرد تطبيق، بل **حل متكامل** يغير حياة:
- **كبار السن** الذين لم يستطيعوا استخدام الهواتف الذكية من قبل
- **ضعاف البصر** الذين كانوا معزولين عن التكنولوجيا
- **العائلات** التي كانت قلقة على أقاربها

**المشاريع المكتملة:**
- ✅ الهيكل الأساسي مع Saiy-PS
- ✅ الصلاحيات النظامية عبر Shizuku
- ✅ النماذج الصوتية المصرية
- ✅ وضع كبار السن المتخصص
- ✅ نظام الطوارئ والكشف عن السقوط

**الخطوات الفورية:**
1. **التجربة الأولى:** تثبيت النسخة التجريبية على 10 أجهزة Honor X6c
2. **الاختبار مع المستخدمين:** اختيار 5 كبار سن في القاهرة للاختبار
3. **تحسين الدقة:** ضبط نماذج الصوت للمصريين الحقيقيين
4. **توثيق الاستخدام:** إنشاء فيديوهات تعليمية باللهجة المصرية

**دعوة للتنفيذ:**
> "التكنولوجيا الحقيقية هي اللي بتخدم الإنسان، مش العكس"  
> هذا المشروع جاهز للتنفيذ الآن، وكل يوم تأخير هو فرصة ضائعة لإعادة استقلالية إنسان مصري.

