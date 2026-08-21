---
name: quy-trinh-task
description: Quy trình 3 bước bắt buộc khi thực hiện một task - phân tích yêu cầu (hỏi lại chỗ mơ hồ, không đoán mò), khảo sát project và lên plan, rồi mới code (không sửa hàm dùng chung, tạo mới thay vì đổi cái cũ). Dùng cho mọi task code/sửa lỗi/thêm tính năng trong dự án này.
---

# Quy trình thực hiện một task

Áp dụng tuần tự 3 bước dưới đây cho MỌI task. Không nhảy cóc, không gộp bước.

## B1 — Phân tích yêu cầu

- Đọc kỹ yêu cầu, xác định rõ: **cái gì cần thay đổi**, **ở màn/luồng nào**, **kết quả mong đợi trông ra sao**.
- Chỗ nào mơ hồ thì **HỎI LẠI**. Tuyệt đối **không tự suy diễn, không đoán mò** ý người dùng.
- Ví dụ những thứ phải hỏi khi chưa rõ: phạm vi áp dụng (chỉ 1 màn hay toàn app), hành vi ở trạng thái biên (edit mode / popup đang mở / khi xoay màn), giữ hay bỏ hành vi cũ, thiết kế UI cụ thể (kích thước, màu, vị trí).
- Chỉ khi yêu cầu đã rõ ràng 100% mới sang B2.

## B2 — Khảo sát project + lên plan

- Xem **tình trạng hiện tại** của code liên quan trước khi nghĩ cách làm: file nào đang xử lý luồng đó, hàm nào đang được gọi, ai đang dùng chung.
- Xác định rõ **những chỗ sẽ đụng vào** và **những chỗ dùng chung có nguy cơ ảnh hưởng**.
- Trình bày **plan các bước thực hiện** trước khi code, để người dùng nắm và duyệt.

## B3 — Bắt đầu code

- Code theo đúng plan đã thống nhất ở B2.
- **Quy tắc quan trọng nhất**: khi task mới cần dùng tới hàm/chức năng **dùng chung**, KHÔNG được sửa hàm đó theo cách làm ảnh hưởng tới các function, tính năng cũ đang chạy.
  - Thay vào đó: **tạo hàm mới** (hoặc overload / nhánh riêng) cho luồng mới.
  - Ngoại lệ duy nhất: khi được **ra lệnh rõ ràng** là sửa lại cái cũ.
- Giữ nguyên hành vi baseline đã hoạt động; mọi thay đổi phải cộng thêm, không phá đi.
- Comment giải thích bằng tiếng Việt theo phong cách sẵn có của dự án (đặc biệt trong `module/iOSLauncher/`): nêu rõ **lý do** và **bất biến** cần giữ, không chỉ mô tả code làm gì.
