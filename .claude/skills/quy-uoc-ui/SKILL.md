---
name: quy-uoc-ui
description: Quy ước dựng UI trong dự án - ảnh bo góc dùng ShapeableImageView (không dùng CardView), ảnh không bo góc dùng ImageView thường, danh sách dùng RecyclerView, và luôn cập nhật localize (đủ bộ ngôn ngữ) mỗi khi thêm/sửa giao diện hoặc hoàn thành task. Dùng cho mọi task đụng tới layout/giao diện trong dự án này.
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

## 2. Danh sách (list)

- **Ưu tiên `RecyclerView`** cho mọi danh sách. Không dùng `ListView`/`GridView` cho code mới.
- `ViewPager2` (bản chất là RecyclerView) dùng cho carousel/trang vuốt ngang là hợp lệ.

## 3. Localize — luôn cập nhật

- **Mọi chuỗi text hiển thị phải nằm trong `strings.xml`**, không hardcode trong layout/code.
- Khi **thêm/sửa giao diện** hoặc **hoàn thành một task**: cập nhật **đủ bộ ngôn ngữ** của dự án, không chỉ bản mặc định.
  - Bản mặc định: `app/res/main/src/values/strings.xml`.
  - Các bản dịch: `values-de`, `values-es`, `values-fr`, `values-hi`, `values-ko`, `values-pt`, `values-in` (bộ ngôn ngữ khớp màn Language: en/hi/es/fr/de/ko/pt).
- Coi việc dịch đủ ngôn ngữ là **một phần của Definition of Done** cho mọi task đụng tới text.
