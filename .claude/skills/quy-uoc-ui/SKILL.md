---
name: quy-uoc-ui
description: Quy ước dựng UI trong dự án - ảnh bo góc dùng ShapeableImageView (không dùng CardView), ảnh không bo góc dùng ImageView thường, ảnh mặc định scaleType centerCrop, danh sách dùng RecyclerView, khoảng cách giữa các vùng giao diện luôn đều, layout co giãn theo nhiều tỉ lệ màn hình, và luôn cập nhật localize (đủ bộ ngôn ngữ) mỗi khi thêm/sửa giao diện hoặc hoàn thành task. Dùng cho mọi task đụng tới layout/giao diện trong dự án này.
---

# Quy ước dựng UI

Áp dụng cho mọi task tạo/sửa layout, thêm giao diện mới.

## 1. Hiển thị ảnh (ImageView)

- **Ảnh KHÔNG bo góc**: dùng `ImageView` bình thường.
- **Ảnh CÓ bo tròn góc**: **ưu tiên `com.google.android.material.imageview.ShapeableImageView`**, bo góc qua `app:shapeAppearanceOverlay` (`cornerFamily=rounded` + `cornerSize`).
  - **KHÔNG dùng `CardView` / `MaterialCardView`** để bo góc ảnh — dễ gây lỗi giao diện (padding/elevation/clip thừa, viền lạ, tốn layer).

Ví dụ ảnh bo 28dp:
```xml
<com.google.android.material.imageview.ShapeableImageView
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:scaleType="centerCrop"
    app:shapeAppearanceOverlay="@style/RoundedImage28" />
```
```xml
<!-- res/values/styles.xml -->
<style name="RoundedImage28">
    <item name="cornerFamily">rounded</item>
    <item name="cornerSize">28dp</item>
</style>
```

### scaleType

- **Luôn để `android:scaleType="centerCrop"`** cho ảnh (thumbnail, wallpaper, icon, ảnh nền thẻ...).
- **`fitXY` chỉ dùng ở vài chỗ đặc biệt**, khi khung đã được dựng đúng tỉ lệ của ảnh nên kéo giãn không làm méo
  (vd carousel màn View theme: bề ngang trang tính theo tỉ lệ ảnh 960x2082). Dùng `fitXY` thì ghi comment lý do.

## 2. Danh sách (list)

- **Ưu tiên `RecyclerView`** cho mọi danh sách. Không dùng `ListView`/`GridView` cho code mới.
- `ViewPager2` (bản chất là RecyclerView) dùng cho carousel/trang vuốt ngang là hợp lệ.

## 3. Lưu ý khi thiết kế XML giao diện

- **Luôn giữ đều khoảng cách giữa các vùng giao diện**: lề màn, khe giữa các khối, khe giữa các ô trong
  lưới/hàng phải đồng nhất (lấy từ `dimens.xml`, vd `themes_screen_padding`, không rải số lẻ mỗi chỗ một kiểu).
  Lưới/hàng ngang dùng `ItemDecoration` hoặc padding chung để khe đều cả 2 bên, không để phần tử dính sát nhau.
- **Luôn thiết kế để giao diện co giãn theo nhiều tỉ lệ màn hình khác nhau**:
  - Dùng `match_parent` / `0dp` + `layout_weight` / constraint thay cho kích thước cố định cả màn.
  - Khung ảnh/thẻ giữ tỉ lệ bằng `app:layout_constraintDimensionRatio` thay vì cố định cả rộng lẫn cao.
  - Kích thước tính trong code (icon, ô lưới) phải tính theo kích thước THẬT sau khi view đã đo, có
    giới hạn min/max; không để phần tử tràn, chồng lên nhau hay dính sát khi màn hẹp / thấp.

## 4. Localize — luôn cập nhật

- **Mọi chuỗi text hiển thị phải nằm trong `strings.xml`**, không hardcode trong layout/code.
- Khi **thêm/sửa giao diện** hoặc **hoàn thành một task**: cập nhật **đủ bộ ngôn ngữ** của dự án, không chỉ bản mặc định.
  - Bản mặc định: `app/res/main/src/values/strings.xml`.
  - Các bản dịch: `values-de`, `values-es`, `values-fr`, `values-hi`, `values-ko`, `values-pt`, `values-in` (bộ ngôn ngữ khớp màn Language: en/hi/es/fr/de/ko/pt).
- Coi việc dịch đủ ngôn ngữ là **một phần của Definition of Done** cho mọi task đụng tới text.
