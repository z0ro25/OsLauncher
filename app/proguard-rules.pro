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

-keep class com.ezla.oslauncher.App { *; }
-keep class com.ezla.oslauncher.model.** { *; }
-keep class com.ezla.oslauncher.database.** { *; }
-keep class com.ezla.oslauncher.Features.** { *;}

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
-keep class com.truongnt.ios.launcher.dynamicui.** { *; }
-keepclassmembers class com.truongnt.ios.launcher.dynamicui.** {
    <init>(...);
}
# Các lớp override khác cũng nạp qua getOverrideObject bằng tên trong res/values/*.xml.
-keep class com.truongnt.ios.launcher.**Callbacks { *; }
-keepclassmembers class com.truongnt.ios.launcher.** {
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
#       at com.truongnt.ios.launcher.applibrary.AppsLibraryLayout.<init>(AppsLibraryLayout.java:76)
#       at com.truongnt.ios.launcher.Launcher.initApp(Launcher.java:905)
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

# ===== TOÀN BỘ lõi launcher + các module phụ thuộc =====
# TRIỆU CHỨNG: build release -> vào launcher MÀN ĐEN HOÀN TOÀN (không crash, chỉ không vẽ gì).
# Lõi launcher (nền AOSP Launcher3) phụ thuộc dày đặc vào tên lớp dạng CHUỖI và reflection:
#   - LauncherProvider giữ DB workspace, DatabaseHelper đọc/ghi theo TÊN CỘT;
#   - default_workspace_*.xml khai TÊN LỚP widget đặt sẵn ở page 0 -> R8 đổi tên là workspace rỗng;
#   - hàng loạt callback/loader gọi chéo qua interface.
# Dò từng lớp đã sót 3 lần (dynamicui, ConstraintLayout, widget provider) nên giữ cả package.
# Khai ở ĐÂY (ngoài consumer-rules.pro của module) để có hiệu lực ngay, không phụ thuộc AAR.
-keep class com.truongnt.ios.** { *; }
-keep interface com.truongnt.ios.** { *; }
-dontwarn com.truongnt.ios.**
-keep class com.ios.theme.** { *; }
-keep class com.github.mmin18.** { *; }
-keep class com.zhuoyi.security.** { *; }
-keep class com.ezt.ios.** { *; }
-keep class com.ezt.varunjohn1990.** { *; }
-keep class com.theartofdev.edmodo.cropper.** { *; }
-dontwarn com.ios.**
-dontwarn com.ezt.**

# ===== AppWidgetProvider của launcher (đồng hồ / lịch / pin / thời tiết / ảnh) =====
# Hệ thống dựng provider bằng TÊN LỚP đọc từ AndroidManifest (reflection), R8 không thấy ai gọi
# constructor nên có thể xoá/đổi tên -> widget nội bộ không hiện, hoặc ném ClassNotFoundException
# khi hệ thống gửi broadcast onUpdate. Giữ cả lớp + constructor rỗng.
-keep class * extends android.appwidget.AppWidgetProvider { *; }
-keep class com.truongnt.ios.launcher.widget.widgetprovider.** { *; }
# Custom view của widget được inflate từ initialLayout (XML) -> cần constructor (Context, AttributeSet).
-keep class com.truongnt.ios.launcher.widget.view.** { *; }

# ===== BroadcastReceiver / Service / ContentProvider khai trong manifest =====
# Cùng lý do: hệ thống nạp bằng tên lớp, không có lời gọi trực tiếp trong mã.
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.ContentProvider { *; }

# ===== Lớp nạp qua Class.forName bằng tên đọc từ string resource =====
# AppFilter.loadByName() và BuildInfo.loadByName() (module iOSLauncher) nhận tên lớp dạng CHUỖI
# rồi Class.forName(...).newInstance(). Không giữ thì R8 xoá lớp/constructor -> loadByName trả null
# hoặc ném ClassNotFoundException. Cùng cơ chế với getOverrideObject đã gây crash ở phần trên.
-keep class * extends com.truongnt.ios.launcher.AppFilter { *; }
-keep class * extends com.truongnt.ios.launcher.BuildInfo { *; }
-keepclassmembers class * extends com.truongnt.ios.launcher.AppFilter {
    public <init>();
}
-keepclassmembers class * extends com.truongnt.ios.launcher.BuildInfo {
    public <init>();
}

# ===== Parcelable =====
# Framework đọc field CREATOR bằng reflection; R8 xoá field không được tham chiếu trực tiếp
# -> BadParcelableException khi truyền object qua Intent/Bundle.
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ===== Room =====
# Lớp *_Impl (Room sinh lúc build) được nạp bằng Class.forName từ tên entity/dao.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ===== Retrofit + model JSON =====
# Retrofit dựng implementation của interface service bằng dynamic proxy -> cần giữ interface + annotation.
-keep,allowobfuscation interface com.ezla.oslauncher.api.** { *; }
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ===== Kotlin =====
# Metadata cần cho reflection của Kotlin (data class, default args...); Continuation dùng cho coroutine.
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }

# ===== Giữ số dòng trong stacktrace của bản release =====
# Không ảnh hưởng kích thước đáng kể nhưng giúp đọc được crash log từ Play Console.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
