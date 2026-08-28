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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn org.litepal.**
-keep class org.litepal.** {*; }

-dontwarn android.support.design.**
-keep class android.support.design.** { *; }
-keep interface android.support.design.** { *; }
-keep public class android.support.design.R$* { *; }
-ignorewarnings

# support v4
-keep class android.support.v4.** { *; }
-keep interface android.support.v4.** { *; }


# support-v7-appcompat
-keep public class android.support.v7.widget.** { *; }
-keep public class android.support.v7.internal.widget.** { *; }
-keep public class android.support.v7.internal.view.menu.** { *; }


# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule


# butter knife
-keep public class * implements butterknife.Unbinder { public <init>(**, android.view.View); }
-keep class butterknife.*
-keepclasseswithmembernames class * { @butterknife.* <methods>; }
-keepclasseswithmembernames class * { @butterknife.* <fields>; }

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**


# google ads
-keep class com.google.android.gms.internal.** { *; }

# ---------- TÙY TỪNG PROJECT CÓ DÙNG KO NẾU DÙNG THÌ ADD OR NHỮNG CLASS STATIC ----------

#custom views
-keep public class * extends android.view.View {
      public <init>(android.content.Context);
      public <init>(android.content.Context, android.util.AttributeSet);
      public <init>(android.content.Context, android.util.AttributeSet, int);
      public void set*(...);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class * extends android.content.Context {
    public void *(android.view.View);
    public void *(android.view.MenuItem);
}

# enum
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-dontnote java.nio.file.Files, java.nio.file.Path
-dontnote **.ILicensingService

-keep class retrofit2.** { *; }
-keepattributes *Annotation*
-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class com.amazic.ads.**{ *; }

-keep class com.amz.ios.launcher.** { *; }
-keep class com.amz.ios.launcher.searchlauncher.** { *; }
-keep class com.amz.ios.** { *; }
-keep class com.ios.boot.** { *; }
-keep class com.ios.cleanwidget.** { *; }
-keep class com.ios.theme.** { *; }

-keep class org.sqlite.** { *; }
-keep class org.sqlite.database.** { *; }

# ---------- ConstraintLayout / MotionLayout ----------
# [BUG FIX] Bản release crash lúc khởi động:
#     java.lang.NoSuchMethodException: androidx.constraintlayout.motion.widget.KeyCycle.<init> []
#         at androidx.constraintlayout.motion.widget.KeyFrames.<clinit>(KeyFrames.java:52)
#         at com.amz.ios.launcher.applibrary.AppsLibraryLayout.<init>
#
# NGUYÊN NHÂN: KeyFrames dựng các lớp Key* (KeyCycle, KeyAttributes, KeyPosition, KeyTrigger...)
# bằng REFLECTION với constructor rỗng. ProGuard không thấy chỗ nào gọi trực tiếp nên xoá constructor
# -> getConstructor() ném NoSuchMethodException, MotionScene không load được, AppsLibraryLayout
# (MotionLayout) không inflate được -> chết ngay ở setContentView.
#
# Rule cũ "-keepclasseswithmembers class * { public <init>(Context, AttributeSet); }" ở trên KHÔNG
# cứu được vì các lớp Key* dùng constructor RỖNG, không phải constructor có AttributeSet.
-keep class androidx.constraintlayout.** { *; }
-keep interface androidx.constraintlayout.** { *; }
-keepclassmembers class androidx.constraintlayout.motion.widget.** {
    public <init>(...);
}
-dontwarn androidx.constraintlayout.**

# Giữ constructor rỗng của MỌI lớp được tạo bằng reflection kiểu này (phòng thư viện khác cùng cách).
-keepclassmembers class * extends androidx.constraintlayout.motion.widget.Key {
    public <init>();
}
