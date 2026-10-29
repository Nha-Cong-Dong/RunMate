# CLAUDE.md — RunMate

Hướng dẫn cho Claude khi làm việc trong dự án này.

## Dự án

- **RunMate**: ứng dụng Android đồng hành cho người chạy bộ (ghi lộ trình GPS, quãng đường, pace, lịch sử).
- Ngôn ngữ: Kotlin · Build: Gradle · JDK 17
- Tài liệu: `README.md` (giới thiệu), `docs/SPEC.md` (đặc tả sản phẩm: app làm gì),
  `docs/DESIGN.md` (thiết kế giao diện: app trông thế nào), `docs/PLAN.md` (kế hoạch và tiến độ)
- SPEC và DESIGN mâu thuẫn thì **SPEC thắng** (DESIGN đã ghi ở đầu file)
- Trả lời bằng **tiếng Việt**.

## Quy tắc 1: Hỏi trước khi sửa

- **Không được tạo, sửa, xoá hay đổi tên file nào khi chưa được tôi đồng ý.**
- Trước khi thay đổi, trình bày ngắn gọn:
  1. Sẽ sửa **file nào**
  2. Sửa **cái gì** (nêu ý chính hoặc đoạn diff)
  3. **Vì sao** cần sửa
- Chỉ làm sau khi tôi trả lời "OK" hoặc "đồng ý". Nếu tôi chỉ đặt câu hỏi, chỉ trả lời câu hỏi, không tự sửa code.
- Mỗi lần chỉ làm **một việc nhỏ**, không tự ý làm thêm việc ngoài yêu cầu.
- Không tự cài thư viện, đổi cấu hình Gradle hay commit/push Git khi chưa hỏi.

## Quy tắc 2: Nêu rõ nguồn, kỹ thuật và lý do

Mỗi khi đề xuất một thư viện, công nghệ, ý tưởng hay cách làm, ghi theo mẫu:

| Mục                     | Nội dung                                                          |
| ----------------------- | ----------------------------------------------------------------- |
| **Kỹ thuật / thư viện** | Tên và phiên bản (ví dụ: Jetpack Compose, Room 2.x)               |
| **Nguồn tài liệu**      | Link tài liệu chính thức (developer.android.com, kotlinlang.org…) |
| **Lý do chọn**          | Vì sao phù hợp với RunMate                                        |
| **Phương án khác**      | 1–2 lựa chọn thay thế và vì sao không chọn                        |

Phần này dùng để tôi đưa vào `README.md` (mục Công nghệ) và `docs/SPEC.md` (mục 7: Kiến trúc & công nghệ).

- Ưu tiên tài liệu **chính thức**; nếu không chắc về phiên bản hay API, nói rõ là chưa chắc, không tự bịa.

## Quy tắc 3: Giao diện theo DESIGN.md

- Trước khi viết hoặc sửa màn hình, thành phần Compose, màu, chữ: đọc phần liên quan trong `docs/DESIGN.md`
  (mục 4 Màu, 5 Chữ, 6 Khoảng cách, 8 Thành phần dùng chung, 9 Từng màn hình, 13 Cách làm trong Compose).
- Dùng đúng tên thành phần như DESIGN (ví dụ `WeekCalendar`, `PlanCard`, `ResultCard`, `RunRing`).
- Không tự thêm màu, cỡ chữ, khoảng cách hay hiệu ứng ngoài DESIGN. Nếu cần, đề xuất sửa DESIGN trước, chờ đồng ý rồi mới viết code.
- Khi đề xuất một màn hình, ghi rõ đang làm theo mục nào của DESIGN.

## Thứ tự công việc

1. **README.md**: điền giới thiệu, tính năng, công nghệ
2. **docs/SPEC.md**: vấn đề, người dùng, tính năng MVP, user stories, luồng màn hình, mô hình dữ liệu, kiến trúc.
   **docs/DESIGN.md**: màu, chữ, thành phần, bố cục từng màn hình
3. **Khung code ban đầu**: tạo project Android tối thiểu theo SPEC đã chốt. Kế hoạch chi tiết và tiến độ: xem `docs/PLAN.md`
4. **Xây dựng MVP**: theo giai đoạn A–G trong `docs/PLAN.md`

Chỉ chuyển sang bước tiếp theo khi tôi xác nhận bước trước đã xong.

## Quy ước

- Commit message theo dạng: `feat: ...`, `fix: ...`, `docs: ...`
- Không commit file bí mật: `google-services.json`, `local.properties`, API key (đã có trong `.gitignore`)

## Skill có sẵn
- /explain — giải thích đoạn code vừa viết và hỏi lại mình vài câu để kiểm tra hiểu
