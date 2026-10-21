---
name: comment-ngan-gon
description: Quy ước viết comment trong code - MỖI KHỐI comment tối đa 3 dòng, ngắn gọn nhưng đầy đủ ý (nêu lý do / bất biến, không mô tả lại code). Áp dụng cho mọi comment mới viết trong repo này; comment cũ đã có giữ nguyên.
---

# Comment ngắn gọn — tối đa 3 dòng mỗi khối

Áp dụng cho **mọi đoạn code mới viết/sửa** trong repo này.

## Quy tắc

1. **Mỗi khối comment tối đa 3 dòng.** Một khối = comment gắn với 1 hàm / 1 biến /
   1 nhánh code. Vượt 3 dòng thì phải cắt, không được nới.
2. **Ngắn nhưng đủ ý.** Giữ lại: **lý do** làm vậy và **bất biến** phải giữ.
   Không mô tả lại code đang làm gì (đọc code là hiểu), không kể lể lịch sử bug.
3. **Tiếng Việt**, cùng phong cách với các file trong `module/iOSLauncher/`.
4. **Chỉ áp cho comment mới.** Comment dài có sẵn trong repo giữ nguyên, không gọt lại
   và không sửa hàng loạt (2 chỗ ngoại lệ duy nhất: người dùng yêu cầu rõ, hoặc comment đó
   đang sửa sai/nói sai về code).

## Cách viết trong 3 dòng

Ưu tiên 1–2 dòng; chỉ dùng dòng thứ 3 khi thật cần. Gộp ý thay vì xuống dòng.

## Ví dụ

Sai — 5 dòng, kể lại quá trình bug và mô tả code:

```java
// Trước đây nhánh này bị lỗi, khi giữ app lần 2 thì overlay còn kẹt lại
// và nuốt mọi touch. Đã đo trên Samsung A50 thấy soCon=8, RAC[6] là
// BlurScreenLayout 1080x2340 clickable=true... Vì thế phải dọn cờ trước
// khi mở popup mới, nếu không popup sẽ không hiện. Hàm này dọn overlay.
removeFloatingMenuOverlay();
```

Đúng — 2 dòng, nêu lý do + bất biến:

```java
// Phải dọn cờ + overlay TRƯỚC khi add popup mới: cờ còn bật thì popup vừa add
// bị view rác MATCH_PARENT che mất (xem removeFloatingMenuOverlay()).
removeFloatingMenuOverlay();
```

## Kiểm tra trước khi bàn giao

- Đếm số dòng của từng khối comment mới — khối nào > 3 dòng là chưa đạt.
- Comment còn mô tả lại code (kiểu "gán x = 1", "gọi hàm A") → xoá.
