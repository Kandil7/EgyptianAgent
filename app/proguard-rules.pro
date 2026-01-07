# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:/path/to/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep Vosk classes
-keep class org.vosk.** { *; }
-dontwarn org.vosk.**

# Keep Egyptian Agent classes
-keep class com.egyptian.agent.** { *; }
-keep class com.egyptian.agent.core.** { *; }
-keep class com.egyptian.agent.stt.** { *; }
-keep class com.egyptian.agent.executors.** { *; }
-keep class com.egyptian.agent.accessibility.** { *; }
-keep class com.egyptian.agent.utils.** { *; }
-keep class com.egyptian.agent.ui.** { *; }
-keep class com.egyptian.agent.receivers.** { *; }

# Keep TTS and speech related classes
-keep class android.speech.** { *; }
-keep class android.speech.tts.** { *; }

# Keep sensor related classes
-keep class android.hardware.Sensor* { *; }
-keep class android.hardware.SensorEvent { *; }
-keep class android.hardware.SensorEventListener { *; }