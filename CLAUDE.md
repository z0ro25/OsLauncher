# OsLauncher — quy tắc làm việc trong repo này

File này áp dụng cho **MỌI task** trong repo này, mọi phiên. Đọc trước khi làm bất cứ gì.

## 1. BẮT BUỘC gọi skill — không chờ người dùng nhắc

Không tự quyết định skill nào "có vẻ liên quan". Các skill dưới đây **phải gọi bằng Skill tool
trước khi bắt tay làm**, không phải sau khi làm xong:

| Loại việc | Skill phải gọi |
|---|---|
| Mọi task code / sửa lỗi / thêm tính năng | `quy-trinh-task` — 3 bước: phân tích yêu cầu → khảo sát + lên plan → code |
| Mọi task, suốt phiên | `khong-suy-dien` — TỐI THƯỢNG, chỗ mơ hồ thì DỪNG hỏi, không đoán ý |
| Mọi task | `khong-tu-build` — không tự build/cài/chạy app; sửa code rồi bàn giao |
| Task đụng layout / giao diện | `quy-uoc-ui` — ShapeableImageView cho ảnh bo góc, RecyclerView cho list, localize đủ bộ ngôn ngữ |
| Mọi task có viết comment | `comment-ngan-gon` — mỗi khối comment mới tối đa 3 dòng, ngắn nhưng đủ lý do/bất biến |
| Mọi câu trả lời cho người dùng | `tieng-viet` |

Vị trí skill: `khong-suy-dien` ở `~/.claude/skills/` (toàn cục, dùng cho mọi dự án — chi tiết
đầy đủ trong `SKILL.md` của nó); 5 skill còn lại ở `.claude/skills/` của repo này.

Quy tắc "hỏi cho rõ rồi mới làm" trong `khong-suy-dien` thắng mọi thứ khác khi mâu thuẫn.

## 2. Bản đồ module (kiểm chứng 21/09/2026)

Repo có **đúng một module application**: `:app`.

- `:app` — shell của launcher. `applicationId` = `com.ezla.oslauncher.beautylauncher`.
  Chứa splash, màn chọn ngôn ngữ, onboarding, HomeActivity, `BaseLauncherApplication`.
  Đây là **nơi duy nhất** khai Firebase (analytics / messaging / crashlytics / config) và
  plugin `google-services`.
- `:module:iOSLauncher` (`com.truongnt.ios.launcher`) — desktop/home của launcher, App Library,
  Search, widget, left page. Cùng các plugin `plugins/UserGuide`, `CleanWidget`, `AppManager`.
- `:module:iOSSearch`, `:module:iOSSettings`, `:module:iOSThemeClub` — các màn con.
- `:library:iOSLiteCommon` (`com.truongnt.ios.ioslite.common`) — thư viện nền dùng chung, chứa
  cả hạ tầng quảng cáo. Mọi module khác đều phụ thuộc nó.
- Các library khác: `BatterySave`, `cropper`, `iosdialogs4android`, `RealTimeBlurView`,
  `iOSAppsNewsProvider`, `iOSSwitchControl`, `ThemeResource:ThemeDefault`.

### Cấu trúc thư mục — KHÔNG theo chuẩn Android

Đừng tìm `src/main/java`, nó không tồn tại:

- `:app`: Java/Kotlin ở `app/src/main/java/...`, layout/resource ở `app/res/**`,
  manifest ở `app/AndroidManifest.xml` (khai tường minh qua `sourceSets`).
- `:module:iOSLauncher` và `:library:iOSLiteCommon`: source nằm thẳng trong `src/`, resource
  trong `res/`, manifest trong `AndroidManifest.xml` — đều khai qua `sourceSets`.
  `iOSLiteCommon` khai riêng cả `kotlin.srcDirs`, nên file `.kt` để chung cây `src/` với Java.

## 3. Bất biến kiến trúc — vi phạm là hỏng build hoặc hỏng hành vi

- **Chiều phụ thuộc một chiều**: `:app` → `:module:*` → `:library:iOSLiteCommon`.
  Library **không đọc được class nào của `:app`**. Muốn library dùng được một giá trị, phải
  đẩy xuống qua API của library.
- **Firebase chỉ có mặt ở `:app`**. `iOSLiteCommon` cố ý không phụ thuộc Firebase — đã có ghi
  chú tại `DailyNotiScheduler` và `BaseLauncherApplication.setupDailyNotification()`. Muốn dùng
  Remote Config ở library thì `:app` đọc rồi đẩy xuống, không thêm Firebase vào library.
- **SharedPreferences dùng chung giữa mọi module**: `PreferencesUtil` (iOSLiteCommon), file
  `com.ezla.oslauncher`. Đây là kênh truyền giá trị giữa `:app` và các module khi cần sống qua
  các lần mở app (tiền lệ: `DailyNotiStore`).
- `SharePrefUtils` (default prefs + file `data`, `dataLang`) nằm ở `:app`, chỉ `:app` dùng được.
- Hạ tầng quảng cáo ở `library/iOSLiteCommon/src/com/truongnt/ios/ioslite/common/ads/` (Kotlin).
  Ad unit ID gom ở `AdsIds`, slot ở `AdsSlot`, cờ bật/tắt + policy ở `Ads`.
- **ProGuard**: `:module:iOSLauncher` bảo vệ code của mình bằng `consumerProguardFiles`
  (`consumer-rules.pro`), không dùng `proguardFiles` — rule ở `proguardFiles` không bao giờ tới
  được `:app`.
- **Version compiler Kotlin là 2.1.x** → Room phải là 2.7.2 trở lên (2.6.1 dùng
  kotlinx-metadata-jvm cũ, kapt báo lỗi metadata 2.1.0). Xem comment trong các `build.gradle`.

## 4. Khi báo cáo lại cho người dùng

Phân biệt rành mạch **chỗ đã kiểm chứng trong code** với **chỗ mới là suy luận**; nói rõ giả
định đã dùng và phần chưa làm. Không tự mở rộng phạm vi task.
