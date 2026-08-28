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

-keep class com.oslauncher.applauncher.themelauncher.App { *; }
-keep class com.oslauncher.applauncher.themelauncher.model.** { *; }
-keep class com.oslauncher.applauncher.themelauncher.database.** { *; }
-keep class com.oslauncher.applauncher.themelauncher.Features.** { *;}

-keep class org.sqlite.** { *; }
-keep class org.sqlite.database.** { *; }
-keep class org.litepal.** {*;}

# ===== Nâng AGP 8 / R8 mới: giữ các lớp nạp bằng REFLECTION =====
# TRIỆU CHỨNG khi thiếu (đo thật lúc nâng lên target 36):
#   java.lang.InstantiationException: Class<...ColorExtractionAlgorithm> cannot be instantiated
#     at Utilities.getOverrideObject(Utilities.java:930)
#     at WallpaperColorInfo.<init> -> LauncherBaseActivity.onCreate -> CRASH ngay khi mở launcher.
# NGUYÊN NHÂN: Utilities.getOverrideObject() nạp lớp bằng Class.forName(tên đọc từ string resource)
#   rồi gọi newInstance()/getDeclaredConstructor(Context). R8 không thấy ai gọi constructor trong mã
#   nên XOÁ constructor (hoặc cả lớp). R8 của AGP 8 tối ưu mạnh hơn AGP 7 nên lỗi mới lộ ra.
# Giữ nguyên lớp + constructor cho toàn bộ nhánh dùng cơ chế override này.
-keep class com.amz.ios.launcher.dynamicui.** { *; }
-keepclassmembers class com.amz.ios.launcher.dynamicui.** {
    <init>(...);
}
# Các lớp override khác cũng nạp qua getOverrideObject bằng tên trong res/values/*.xml.
-keep class com.amz.ios.launcher.**Callbacks { *; }
-keepclassmembers class com.amz.ios.launcher.** {
    public <init>(android.content.Context);
}

# ===== Gson + R8 (AGP 8): "TypeToken must be created with a type argument" =====
# TRIỆU CHỨNG (đo thật bằng logcat): app CRASH-LOOP ngay màn splash (ExceptionHandler bắt lỗi rồi
#   khởi động lại -> kẹt splash mãi):
#     java.lang.IllegalStateException: TypeToken must be created with a type argument ...
#       at com.google.gson.reflect.TypeToken.getTypeTokenTypeArgument
#       at ...YourWallpaperDataManager.getAllYourWallPaper(YourWallpaperDataManager.kt:63)
#       at ...SplashActivity.initView(SplashActivity.kt:40)
# NGUYÊN NHÂN: mã dùng `object : TypeToken<List<...>>(){}` (lớp con TypeToken ẩn danh). R8 của AGP 8
#   (>=3.0) XOÁ generic signature của lớp con này -> Gson 2.9.1 không lấy được type argument -> ném.
#   `-keepattributes Signature` (khối OkHttp phía trên) là ĐK CẦN nhưng CHƯA ĐỦ với R8 mới: phải giữ
#   riêng generic signature cho chính TypeToken và mọi lớp con của nó. Gson chỉ ship consumer-rule
#   này từ 2.10+, dự án đang ở 2.9.1 nên phải khai báo tay.
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
# Giữ field có @SerializedName để R8 không bỏ trống dữ liệu parse được.
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ===== ConstraintLayout / MotionLayout + R8 =====
# TRIỆU CHỨNG (logcat thật, Samsung A Android 9): app chết ngay ở setContentView khi mở launcher:
#     java.lang.NoSuchMethodException: androidx.constraintlayout.motion.widget.KeyCycle.<init> []
#       at java.lang.Class.getConstructor(Class.java:1725)
#       at androidx.constraintlayout.motion.widget.KeyFrames.<clinit>(KeyFrames.java:52)
#       at androidx.constraintlayout.motion.widget.MotionScene.load(MotionScene.java:1132)
#       at com.amz.ios.launcher.applibrary.AppsLibraryLayout.<init>(AppsLibraryLayout.java:76)
#       at com.amz.ios.launcher.Launcher.initApp(Launcher.java:905)
# NGUYÊN NHÂN: KeyFrames dựng các lớp Key* (KeyCycle, KeyAttributes, KeyPosition, KeyTrigger...) bằng
#   REFLECTION qua constructor RỖNG. R8 không thấy chỗ nào gọi trực tiếp nên xoá constructor ->
#   getConstructor() ném -> MotionScene không load -> AppsLibraryLayout (một MotionLayout) không
#   inflate được -> chết cả màn hình chính.
#   Rule "-keepclasseswithmembers class * { public <init>(Context, AttributeSet); }" KHÔNG cứu được
#   vì các lớp Key* dùng constructor RỖNG, không phải constructor có AttributeSet.
-keep class androidx.constraintlayout.** { *; }
-keep interface androidx.constraintlayout.** { *; }
-keepclassmembers class androidx.constraintlayout.motion.widget.** {
    public <init>(...);
}
-dontwarn androidx.constraintlayout.**
