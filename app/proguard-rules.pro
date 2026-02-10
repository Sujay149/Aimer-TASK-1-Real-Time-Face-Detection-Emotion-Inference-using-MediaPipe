# ProGuard rules for MediaPipe
-keep class com.google.mediapipe.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

# ProGuard rules for CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Preserve JNI method names
-keepclasseswithmembernames class * {
    native <methods>;
}
