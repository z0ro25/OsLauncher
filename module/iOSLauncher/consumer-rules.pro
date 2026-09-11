# ============================================================================
# CONSUMER PROGUARD RULES — module :iOSLauncher
#
# Khác với proguard-rules.pro (chỉ dùng khi build RIÊNG module này, mà library
# thực tế không bị minify nên gần như vô tác dụng), file này được AAR mang theo
# và TỰ ĐỘNG áp khi :app build release. Đây mới là nơi đặt rule bảo vệ code của
# launcher khỏi R8.
#
# Nguyên tắc: chỉ giữ những gì R8 KHÔNG THỂ tự suy ra — tức các lớp/thành viên
# được nạp bằng reflection, bằng tên chuỗi, hoặc do hệ thống Android dựng hộ.
# ============================================================================

# ===== Lớp nạp qua Utilities.getOverrideObject() =====
# Cơ chế: đọc TÊN LỚP từ string resource rồi Class.forName(...).getDeclaredConstructor(Context)
# .newInstance(). R8 không thấy lời gọi constructor nào nên xoá constructor (hoặc cả lớp).
# TRIỆU CHỨNG đã gặp thật khi nâng AGP 8:
#   java.lang.InstantiationException: Class<...ColorExtractionAlgorithm> cannot be instantiated
#     at Utilities.getOverrideObject -> WallpaperColorInfo.<init> -> CRASH khi mở launcher.
-keep class com.truongnt.ios.launcher.dynamicui.** { *; }
-keepclassmembers class com.truongnt.ios.launcher.dynamicui.** {
    <init>(...);
}
-keep class com.truongnt.ios.launcher.**Callbacks { *; }
-keepclassmembers class com.truongnt.ios.launcher.** {
    public <init>(android.content.Context);
}

# ===== AppFilter / BuildInfo: loadByName(String) =====
# Cả hai nhận tên lớp dạng CHUỖI rồi Class.forName(...).newInstance() với constructor RỖNG.
-keep class com.truongnt.ios.launcher.AppFilter { *; }
-keep class com.truongnt.ios.launcher.BuildInfo { *; }
-keep class * extends com.truongnt.ios.launcher.AppFilter { *; }
-keep class * extends com.truongnt.ios.launcher.BuildInfo { *; }
-keepclassmembers class * extends com.truongnt.ios.launcher.AppFilter {
    public <init>();
}
-keepclassmembers class * extends com.truongnt.ios.launcher.BuildInfo {
    public <init>();
}

# ===== AppWidgetProvider của launcher (đồng hồ / lịch / pin / thời tiết / ảnh) =====
# 21 provider khai trong AndroidManifest của module. Hệ thống dựng chúng bằng TÊN LỚP đọc từ
# manifest; R8 không thấy ai gọi constructor -> xoá/đổi tên -> widget không hiện, hoặc ném
# ClassNotFoundException khi hệ thống gửi broadcast onUpdate.
-keep class * extends android.appwidget.AppWidgetProvider { *; }
-keep class com.truongnt.ios.launcher.widget.widgetprovider.** { *; }

# Custom view của widget: inflate từ initialLayout (XML) nên cần constructor (Context, AttributeSet).
-keep class com.truongnt.ios.launcher.widget.view.** { *; }

# ===== Custom view dùng trong layout XML của launcher =====
# LayoutInflater dựng bằng tên lớp ghi trong XML -> phải giữ tên lớp + constructor.
-keep public class com.truongnt.ios.launcher.** extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keep public class com.truongnt.ios.launcher.** extends android.view.ViewGroup {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ===== Activity / Receiver / Service khai trong manifest module =====
-keep class * extends android.app.Activity { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.ContentProvider { *; }

# ===== TOÀN BỘ lõi launcher =====
# TRIỆU CHỨNG: build release -> vào launcher MÀN ĐEN HOÀN TOÀN (app không crash, chỉ không vẽ gì).
#
# Lõi launcher (kế thừa từ AOSP Launcher3) phụ thuộc dày đặc vào:
#   - LauncherProvider: ContentProvider giữ DB workspace; DatabaseHelper đọc/ghi bằng TÊN CỘT và
#     nạp layout mặc định từ XML -> R8 đổi tên/xoá là mất sạch dữ liệu màn hình -> không vẽ gì.
#   - default_workspace_*.xml khai TÊN LỚP dạng chuỗi cho widget đặt sẵn ở page 0
#     (AnalogClockWidgetProvider, PictureAppWidgetProvider): R8 đổi tên -> parse XML không tìm thấy
#     lớp -> bỏ qua item -> workspace rỗng.
#   - Hàng loạt callback/loader gọi chéo nhau qua interface và reflection nội bộ.
#
# Việc dò từng lớp một là không khả thi và dễ sót (đã sót 3 lần: dynamicui, ConstraintLayout,
# widget provider). Giữ nguyên cả package lõi: launcher là phần lớn code của app nên lợi ích rút
# gọn ở đây vốn không đáng kể, đổi lại tránh hẳn một lớp lỗi khó truy.
-keep class com.truongnt.ios.** { *; }
-keep interface com.truongnt.ios.** { *; }
-dontwarn com.truongnt.ios.**

# Các module/library mà launcher gọi tới, cũng có custom view + lớp nạp bằng tên chuỗi.
-keep class com.ios.theme.** { *; }
-keep class com.github.mmin18.** { *; }
-keep class com.zhuoyi.security.** { *; }
-keep class com.ezt.ios.** { *; }
-keep class com.ezt.varunjohn1990.** { *; }
-keep class com.theartofdev.edmodo.cropper.** { *; }
-dontwarn com.ios.**
-dontwarn com.ezt.**

# ===== Parcelable =====
# Framework đọc field CREATOR bằng reflection -> BadParcelableException nếu bị xoá.
# ItemInfo/ShortcutInfo/FolderInfo... truyền qua Intent/Bundle trong luồng kéo thả.
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ===== Room (HiddenApp / AppOverride) =====
# Lớp *_Impl do Room sinh lúc build, nạp bằng Class.forName từ tên database.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep class com.truongnt.ios.database.** { *; }
-keep class com.truongnt.ios.launcher.appoverride.** { *; }
-dontwarn androidx.room.paging.**

# ===== ConstraintLayout / MotionLayout =====
# KeyFrames dựng các lớp Key* (KeyCycle, KeyAttributes, KeyPosition...) bằng reflection qua
# constructor RỖNG. TRIỆU CHỨNG đã gặp thật (Samsung A, Android 9):
#   NoSuchMethodException: KeyCycle.<init> [] -> MotionScene.load -> AppsLibraryLayout không
#   inflate được -> chết cả màn hình chính.
-keep class androidx.constraintlayout.** { *; }
-keep interface androidx.constraintlayout.** { *; }
-keepclassmembers class androidx.constraintlayout.motion.widget.** {
    public <init>(...);
}
-dontwarn androidx.constraintlayout.**

# ===== Hidden API (LSPosed) =====
# Gọi ViewRootImpl.createBackgroundBlurDrawable (@hide) bằng reflection cho dock glass blur.
-keep class org.lsposed.hiddenapibypass.** { *; }
-dontwarn org.lsposed.hiddenapibypass.**

# ===== enum =====
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===== Giữ generic signature + annotation =====
# Cần cho Gson/Retrofit và mọi chỗ đọc kiểu generic lúc chạy.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# ===== Kotlin =====
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }
