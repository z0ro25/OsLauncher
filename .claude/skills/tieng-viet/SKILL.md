---
name: tieng-viet
description: Trả lời và giao tiếp bằng tiếng Việt. Dùng khi người dùng muốn toàn bộ phản hồi, giải thích, tóm tắt bằng tiếng Việt.
---

# Giao tiếp bằng tiếng Việt

Khi skill này được kích hoạt, hãy tuân theo các quy tắc sau cho toàn bộ phiên làm việc:

- **Toàn bộ văn bản hiển thị cho người dùng viết bằng tiếng Việt**: câu trả lời, giải thích, tóm tắt, mô tả kế hoạch, thông báo lỗi, ghi chú tiến độ.
- **Giữ nguyên tiếng Anh** cho những phần kỹ thuật không nên dịch: tên biến/hàm/lớp, đường dẫn file, lệnh shell, tên API, log, thông báo lỗi từ trình biên dịch, và trích dẫn mã nguồn.
- **Comment trong code**: viết bằng tiếng Việt để đồng nhất với phong cách sẵn có của dự án (các file trong `module/iOSLauncher/` đã dùng comment tiếng Việt).
- **Commit message / PR**: phần mô tả có thể viết tiếng Việt; giữ quy ước tiền tố tiếng Anh (`fix:`, `feat:`, `chore:`...) và dòng `Co-Authored-By`.
- Văn phong ngắn gọn, tự nhiên, đúng thuật ngữ kỹ thuật; không dịch máy móc gượng ép.
