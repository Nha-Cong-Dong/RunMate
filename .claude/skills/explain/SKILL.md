---
name: explain
description: Giải thích code Kotlin/Android trong dự án RunMate, giúp người mới hiểu vai trò class, hàm và luồng dữ liệu. Dùng khi người dùng gõ /explain hoặc muốn hiểu code AI vừa viết.
---

# Explain — RunMate

Mục tiêu: Giúp tôi hiểu code để có thể tự đọc, kiểm tra và sửa lỗi, không chỉ dựa vào AI.

1. **Xác định phạm vi:** File, class, hàm người dùng chỉ định hoặc phần code vừa thay đổi.

2. **Giải thích ngắn gọn theo thứ tự:**
   - **Mục đích:** Code này làm gì trong RunMate?
   - **Vai trò:** Class/hàm này chịu trách nhiệm gì?
   - **Luồng hoạt động:** Ai gọi hàm, dữ liệu đi qua đâu và kết quả được lưu hoặc hiển thị thế nào?
   - **Điểm cần hiểu:** Giải thích các khái niệm Kotlin, OOP, Android hoặc thư viện quan trọng.
   - **Chỗ dễ sai:** Lỗi thường gặp và cách kiểm tra.

3. **Ưu tiên kiến thức cốt lõi:** Không giải thích từng dòng nếu không cần thiết. Tập trung vào luồng nghiệp vụ, trách nhiệm của class, dữ liệu đầu vào/đầu ra và lý do thiết kế.

4. **Kiểm tra mức độ hiểu:** Cuối phần giải thích, hỏi 2–3 câu ngắn về code vừa học. Không đưa đáp án ngay; chờ tôi trả lời rồi nhận xét và giải thích lại nếu cần.

5. **Nguyên tắc:**
   - Trả lời bằng tiếng Việt, giữ nguyên thuật ngữ kỹ thuật tiếng Anh.
   - Dùng ví dụ cụ thể từ code RunMate.
   - Giải thích dễ hiểu cho người mới học Android và OOP.
   - Không tự ý sửa code hoặc file dự án khi chưa được đồng ý.
   - Không mặc định rằng code chạy được đồng nghĩa với việc tôi đã hiểu code.