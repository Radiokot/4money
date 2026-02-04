# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# The app is opensource, no need to obsfucate
-dontobfuscate

# R8 optimization for Kotlin null checks
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
  public static void checkNotNull(...);
}

# Keep reflection access to the drawables
-keepclassmembers class **.R$* {
    public static <fields>;
}
-keep class **.R$*

# Logging stack
-keep class ch.qos.logback.** { *; }
-dontwarn javax.mail.**
-keep class org.slf4j.** { *; }
-keep class io.github.oshai.** { *; }
-dontwarn com.oracle.svm.core.annotate.**
