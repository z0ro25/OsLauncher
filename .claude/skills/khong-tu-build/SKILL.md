---
name: khong-tu-build
description: Không tự ý build/cài/chạy app lên máy để test. Chỉ sửa code rồi bàn giao; người dùng tự build và test. Dùng cho mọi task sửa lỗi hoặc thêm tính năng trong dự án này.
---

# Không tự build / cài / test

Mặc định trong dự án này: **KHÔNG tự chạy build hay test trên thiết bị** — mất nhiều thời gian chờ.

## Không làm (trừ khi được yêu cầu rõ ràng)

- `./gradlew assembleDebug` / `assembleRelease` / bất kỳ lệnh build nào
- `adb install`, `adb shell am start`, `input tap/swipe`, `screencap`
- Cài APK lên máy rồi thao tác mô phỏng để tái hiện lỗi

## Làm thay vào đó

- Đọc code, dựa vào ảnh chụp / mô tả / log **do người dùng cung cấp** để xác định nguyên nhân.
- Nếu bí và cần số liệu thật từ máy: **hỏi trước**, nêu rõ cần đo gì và vì sao, để người dùng quyết định.
- Sửa xong thì bàn giao, nói rõ **cần test lại những gì** — người dùng tự build và kiểm chứng.
- Khi báo cáo, phân biệt rành mạch: chỗ nào **đã kiểm chứng**, chỗ nào mới chỉ là **suy luận từ code**. Không nói "đã fix xong" cho phần chưa được chạy thật.

## Ngoại lệ

Chỉ build/cài/test khi người dùng nói thẳng: "build đi", "cài lên máy", "test thử", "chạy thử xem".
