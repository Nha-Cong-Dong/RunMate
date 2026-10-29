# RunMate: Thiết kế giao diện (DESIGN)

> Tài liệu này đi kèm `SPEC.md`. SPEC nói app **làm gì**, DESIGN nói app **trông và cảm thấy thế nào**. Khi hai tài liệu mâu thuẫn, SPEC thắng.
>
> Mọi mã màn hình (S1–S8), mã quy tắc (BR-xx) và trạng thái (`PLANNED`…) lấy từ SPEC.

## Mục lục

1. [Tinh thần thiết kế](#1-tinh-thần-thiết-kế)
2. [Dải gradient "Nhịp"](#2-dải-gradient-nhịp)
3. [Vòng chạy: làm buổi đã hoàn thành trở nên đặc biệt](#3-vòng-chạy-làm-buổi-đã-hoàn-thành-trở-nên-đặc-biệt)
4. [Màu sắc](#4-màu-sắc)
5. [Chữ](#5-chữ)
6. [Khoảng cách, bo góc, viền](#6-khoảng-cách-bo-góc-viền)
7. [Biểu tượng](#7-biểu-tượng)
8. [Thành phần dùng chung](#8-thành-phần-dùng-chung)
9. [Từng màn hình](#9-từng-màn-hình)
10. [Hộp thoại](#10-hộp-thoại)
11. [Chuyển động và rung](#11-chuyển-động-và-rung)
12. [Khả năng truy cập](#12-khả-năng-truy-cập)
13. [Cách làm trong Compose](#13-cách-làm-trong-compose)
14. [Nên và không nên](#14-nên-và-không-nên)
15. [Quyết định đã chốt](#15-quyết-định-đã-chốt)

---

## 1. Tinh thần thiết kế

**Một câu:** tối giản như Instagram, chỉ có **một dải gradient** làm chữ ký, và gradient chỉ có một nghĩa: **chạy** (đã chạy, đang chạy, hoặc bấm để chạy).

| Nguyên tắc | Nghĩa là |
|---|---|
| Nền trung tính | Đen, trắng, xám chiếm hơn 90% giao diện. Không tô màu cho đẹp |
| Số liệu là nhân vật chính | Quãng đường, thời gian, pace dùng chữ to, đậm, số đều độ rộng |
| Gradient = chạy | Kế hoạch chưa chạy thì xám. Chạy rồi thì có màu. Nhìn lịch tuần là thấy ngay mình đã chạy những ngày nào |
| Ít lớp, ít bóng | Không đổ bóng. Phân tách bằng khoảng trắng và đường kẻ mảnh 1 dp |
| Liếc là hiểu | S6 phải đọc được trong 1 giây khi đang chạy (SPEC 10.6) |

**Lấy gì từ Instagram:**

| Instagram | RunMate |
|---|---|
| Vòng màu quanh avatar: có story để xem | **Vòng chạy** quanh một ngày: ngày đó đã chạy, có kết quả để xem (mục 3) |
| Avatar bên trong vòng | **Hình dạng quỹ đạo** của buổi chạy bên trong vòng |
| Vòng chia đoạn khi có nhiều story | Vòng chia đoạn khi một ngày có nhiều buổi chạy |
| Hàng stories ngang ở đầu trang | Lịch tuần T2–CN ở đầu S1 |
| Nút Tạo bài (+) ở giữa thanh dưới | Nút **Chạy** ở giữa thanh dưới (SPEC 9.3) |

MVP không có mạng xã hội (SPEC 3), nên không có bạn bè hay stories thật. Chỉ mượn **ngôn ngữ hình ảnh**.

---

## 2. Dải gradient "Nhịp"

### 2.1 Định nghĩa

Đi từ lạnh sang nóng, tượng trưng cho cường độ tăng dần.

| Điểm dừng | Vị trí | Màu | Tên token |
|---|---|---|---|
| 1 | 0,0 | `#3B82F6` | `nhipBlue` |
| 2 | 0,2 | `#22D3EE` | `nhipCyan` |
| 3 | 0,4 | `#A3E635` | `nhipLime` |
| 4 | 0,6 | `#FACC15` | `nhipYellow` |
| 5 | 0,8 | `#F97316` | `nhipOrange` |
| 6 | 1,0 | `#EC4899` | `nhipPink` |

- Giống nhau ở chế độ sáng và tối.
- Hướng mặc định: chéo từ **dưới-trái lên trên-phải** (hình chữ nhật), hoặc **xoay theo chiều kim đồng hồ bắt đầu từ 12 giờ** (vòng tròn).

### 2.2 Gradient được dùng ở đâu

| Chỗ dùng | Màn hình | Nhóm nghĩa |
|---|---|---|
| **Vòng chạy** quanh ngày đã chạy | S1 (lịch tuần) | Đã chạy |
| Vòng chạy trên thẻ kết quả | S1 (danh sách ngày) | Đã chạy |
| Quỹ đạo tô theo pace | S7 | Đã chạy |
| Vòng tiến độ mục tiêu | S6, S7 | Đang chạy / đã chạy |
| Vòng tỷ lệ giữ lịch | S8 | Đã chạy |
| Chấm "đang ghi", vòng xoay khi đang chạy | S1, S6 | Đang chạy |
| Nút **Chạy** ở thanh dưới | S1, S8 | Bấm để chạy |
| Nút **Bắt đầu chạy** / **Tiếp tục** | S4, S5, S6 | Bấm để chạy |

### 2.3 Gradient KHÔNG được dùng ở đâu

- Kế hoạch **chưa chạy** (`PLANNED`, `MISSED`, `CANCELLED`). Chúng dùng nét xám (mục 3.2).
- Nền màn hình, nền thẻ, thanh trên cùng.
- Chữ (lime và vàng không đủ tương phản trên nền trắng).
- Cảnh báo thời tiết và nút xoá: dùng màu ngữ nghĩa (mục 4.3).

**Kiểm tra nhanh:** mỗi chỗ có gradient phải trả lời được câu "nó nói gì về việc chạy?". Không trả lời được thì bỏ gradient.

### 2.4 Tô quỹ đạo theo pace

Dùng ở S7.

1. Tính pace cho từng đoạn nhỏ giữa hai điểm liên tiếp **trong cùng `segmentIndex`**.
2. Làm mượt bằng trung bình trượt 5 điểm, để GPS nhiễu không làm đường loang lổ.
3. Lấy phân vị 10% và 90% của pace trong **chính buổi chạy đó** làm hai đầu thang. Đoạn chậm nhất ứng với `t = 0` (xanh dương), nhanh nhất ứng với `t = 1` (hồng). Giá trị ngoài khoảng thì kẹp lại.
4. Màu của đoạn = nội suy trên dải gradient tại vị trí `t`.
5. Không đủ điểm để tính pace (nhưng vẫn vẽ được): tô cả đường bằng gradient chéo mặc định.

Thang là **tương đối theo từng buổi chạy**, nên luôn có chú thích "chậm → nhanh", không ghi số pace lên thang.

---

## 3. Vòng chạy: làm buổi đã hoàn thành trở nên đặc biệt

Đây là chữ ký của app. Trên Instagram, vòng màu quanh avatar nghĩa là "có story để xem". Trên RunMate, **vòng gradient quanh một ngày nghĩa là "hôm đó bạn đã chạy, có kết quả để xem"**, và bên trong vòng là hình dạng đường bạn đã chạy.

Một tuần chạy đều sẽ trông như một hàng stories đầy màu. Một tuần bỏ lỡ nhiều thì xám. Không cần biểu đồ hay lời nhắc, người dùng tự thấy và tự muốn "tô màu" cho các ngày còn lại.

"Buổi chạy" ở đây gồm cả **kế hoạch `COMPLETED`** và **buổi chạy tự do** (SPEC US12: cả chạy tự do cũng hiện trên lịch).

### 3.1 Cấu tạo `RunRing`

```
   ╭─ vòng ngoài (gradient) ─╮
   │  ╭─ khe hở (nền) ─╮      │
   │  │  lõi: quỹ đạo   │  ✓ ← huy hiệu
   │  ╰────────────────╯      │
   ╰──────────────────────────╯
```

| Phần | Cỡ S (lịch tuần) | Cỡ M (thẻ ở S1) |
|---|---|---|
| Đường kính ngoài | 44 dp | 52 dp |
| Vòng ngoài | 2,5 dp | 3 dp |
| Khe hở (màu `background`) | 2 dp | 2,5 dp |
| Lõi (nền `surfaceContainer`) | 35 dp | 41 dp |
| Nét quỹ đạo trong lõi | 1,75 dp, màu `textPrimary` | 2 dp, màu `textPrimary` |
| Huy hiệu | 16 dp | 18 dp |

- **Lõi** vẽ quỹ đạo thu nhỏ của buổi chạy (giữ tỷ lệ, căn giữa, đệm 6 dp). Một ngày có nhiều buổi thì vẽ buổi **dài nhất**. Quá ít điểm để vẽ thì thay bằng biểu tượng `directions_run`.
- Quỹ đạo trong lõi dùng màu `textPrimary` chứ không dùng gradient, để vòng ngoài nổi bật và đường vẫn rõ ở cỡ nhỏ.

### 3.2 Các trạng thái của một ngày trên lịch tuần

| Ngày có gì | Vòng ngoài | Lõi | Huy hiệu |
|---|---|---|---|
| Một buổi chạy đã lưu | **Gradient đầy** | Quỹ đạo | ✓ nếu đạt mục tiêu |
| Nhiều buổi chạy | **Gradient chia đoạn**, mỗi buổi một đoạn, khe 6° giữa các đoạn, tối đa 4 đoạn (giống Insta khi có nhiều story) | Quỹ đạo dài nhất | Số buổi ("2") ở góc trên phải; ✓ ở góc dưới phải nếu **mọi** buổi đều đạt mục tiêu |
| Buổi chạy đang diễn ra | Gradient **xoay chậm** | Chấm gradient nhấp nháy | |
| Chỉ có kế hoạch `PLANNED` (chưa chạy) | **Nét đứt** 1,5 dp, màu `textSecondary` | Số ngày, `textPrimary` | |
| Chỉ có kế hoạch `MISSED` | Nét liền 1,5 dp, màu `outlineStrong` | Số ngày, `textSecondary` | `!` nền `error` |
| Không có gì, hoặc chỉ có `CANCELLED` | Không có vòng | Số ngày, `textSecondary` | |

**Thứ tự ưu tiên** khi một ngày có nhiều thứ: đang chạy > đã chạy > `PLANNED` > `MISSED`. Ví dụ ngày có một buổi đã chạy và một kế hoạch `PLANNED` buổi chiều thì hiện vòng gradient (thành tích được ưu tiên). Kế hoạch còn lại vẫn thấy trong danh sách ngày bên dưới.

**Đạt mục tiêu** (BR-37): tỷ lệ ≥ 100%. Mọi buổi chạy đều có mục tiêu (BR-03, BR-42) nên luôn tính được.

**Huy hiệu ✓:** tròn, nền `inverse`, biểu tượng `check` màu `onInverse`, viền 2 dp màu `background` để tách khỏi vòng. Giống dấu "+" trên avatar của Instagram.

### 3.3 Vì sao không làm xám vòng sau khi xem

Instagram làm xám vòng sau khi xem vì story đã "cũ". Buổi chạy thì khác: thành tích không mất giá trị sau khi xem. Vòng gradient **giữ màu mãi mãi**. Cũng nhờ vậy không cần thêm cột "đã xem" vào cơ sở dữ liệu (SPEC 7).

### 3.4 Khoảnh khắc hoàn thành

Lúc vòng chạy xuất hiện lần đầu là lúc đáng ăn mừng nhất. Ba nhịp:

1. **S6 → S7** (SPEC 9.3, sau khi bấm Kết thúc và buổi chạy được lưu):
   - Quỹ đạo tự vẽ dần theo đúng hướng chạy, từ điểm đầu tới điểm cuối, trong 1 000 ms.
   - Cùng lúc, `GoalRing` quét từ 0 tới tỷ lệ thật trong 800 ms.
   - Đạt mục tiêu: nhãn "Đạt mục tiêu" có dấu ✓ hiện lên, rung 2 lần.
2. **Quay về S1:** vòng của ngày hôm đó **vẽ vào** (gradient quét 0° → 360° trong 600 ms), rồi huy hiệu ✓ bật lên (phóng 0 → 1,15 → 1,0 trong 300 ms). Chỉ chạy **một lần**, cho buổi vừa kết thúc. App nhớ điều này trong bộ nhớ của phiên đang mở, không lưu vào DB.
3. **Thẻ kết quả** trong danh sách ngày hiện ngay, có vòng chạy cỡ M (mục 8.6).

Khi người dùng tắt hoạt ảnh hệ thống: bỏ hết các hiệu ứng trên, hiện thẳng trạng thái cuối.

### 3.5 Vòng chạy xuất hiện ở đâu

| Nơi | Cỡ | Ghi chú |
|---|---|---|
| Lịch tuần S1 | S | Theo bảng 3.2 |
| Thẻ kế hoạch Hoàn thành, thẻ chạy tự do (S1) | M | Như avatar bên trái thẻ |
| S8 Thống kê | | **Không dùng.** S8 dùng `GoalRing` cho tỷ lệ giữ lịch |

---

## 4. Màu sắc

App theo chế độ sáng/tối của hệ thống (SPEC 10.5). **Tắt màu động (Material You)** để giữ nhận diện.

### 4.1 Màu trung tính

| Token | Sáng | Tối | Dùng cho |
|---|---|---|---|
| `background` | `#FFFFFF` | `#0A0A0A` | Nền màn hình, khe hở của vòng chạy |
| `surface` | `#FFFFFF` | `#0A0A0A` | Thanh trên, thanh dưới, hộp thoại |
| `surfaceContainer` | `#F5F5F5` | `#1A1A1A` | Thẻ, ô nhập, khối thời tiết, lõi vòng chạy |
| `surfaceContainerHigh` | `#EDEDED` | `#262626` | Nút phụ, nền vòng tiến độ |
| `outline` | `#E5E5E5` | `#262626` | Đường kẻ mảnh 1 dp |
| `outlineStrong` | `#D4D4D4` | `#404040` | Viền ô nhập, viền chip, vòng ngày `MISSED` |
| `textPrimary` | `#111111` | `#FAFAFA` | Chữ chính, số liệu, quỹ đạo trong lõi |
| `textSecondary` | `#6B6B6B` | `#A3A3A3` | Nhãn, chú thích, vòng nét đứt `PLANNED` |
| `textDisabled` | `#A3A3A3` | `#525252` | Chỉ cho chữ bị vô hiệu hoá |
| `inverse` | `#111111` | `#FAFAFA` | Nút chính, chip `COMPLETED`, huy hiệu ✓, ngày đang chọn |
| `onInverse` | `#FFFFFF` | `#0A0A0A` | Chữ trên `inverse` |

`textSecondary` đạt tương phản ≥ 4,5:1 trên `background` và `surfaceContainer` ở cả hai chế độ. `textDisabled` thì không, nên chỉ dùng cho chữ bị vô hiệu hoá.

### 4.2 Gradient

Xem mục 2.1. Có thêm một token:

| Token | Sáng | Tối | Dùng cho |
|---|---|---|---|
| `ringTrack` | `#EDEDED` | `#262626` | Phần chưa đạt của `GoalRing` |

### 4.3 Màu ngữ nghĩa

| Token | Sáng | Tối | Dùng cho |
|---|---|---|---|
| `warning` | `#B45309` | `#FBBF24` | Biểu tượng và chữ cảnh báo thời tiết (BR-10 đến BR-13), cảnh báo tiết kiệm pin (BR-61) |
| `warningContainer` | `#FFF7ED` | `#2A1A0C` | Nền khối cảnh báo |
| `error` | `#B91C1C` | `#F87171` | Nút xoá, nhãn "Bỏ lỡ", huy hiệu `!`, lỗi nhập liệu |
| `errorContainer` | `#FEF2F2` | `#2A1010` | Nền khối lỗi mạng |
| `success` | `#15803D` | `#4ADE80` | Chữ "Đạt mục tiêu", tỷ lệ ≥ 100% |

Màu ngữ nghĩa luôn đi kèm biểu tượng và chữ, không bao giờ dùng màu một mình để báo hiệu.

### 4.4 Ánh xạ sang Material 3

| Vai trò Material 3 | Token RunMate |
|---|---|
| `primary` | `inverse` |
| `onPrimary` | `onInverse` |
| `secondary`, `tertiary` | `textSecondary` (hầu như không dùng) |
| `background`, `surface` | `background`, `surface` |
| `surfaceContainer*` | `surfaceContainer`, `surfaceContainerHigh` |
| `onBackground`, `onSurface` | `textPrimary` |
| `onSurfaceVariant` | `textSecondary` |
| `outline`, `outlineVariant` | `outlineStrong`, `outline` |
| `error`, `errorContainer` | `error`, `errorContainer` |

Gradient, `ringTrack`, `warning`, `success` không có vai trò sẵn trong Material 3, nên để trong `RunMateColors` riêng (mục 13).

---

## 5. Chữ

### 5.1 Font

- **Inter**, giấy phép SIL OFL, có đủ dấu tiếng Việt.
- **Đóng gói trong `res/font/`**, không dùng Downloadable Fonts. Lý do: app phải chạy không mạng (SPEC 10.3) và không gửi gì ra ngoài ngoài những thứ SPEC 10.4 cho phép.
- Chỉ cần 4 độ đậm: 400, 500, 600, 800.
- **Mọi con số** (quãng đường, thời gian, pace, %, giờ, ngày trên lịch) bật `fontFeatureSettings = "tnum"` để chữ số rộng bằng nhau, số không bị nhảy khi cập nhật.

### 5.2 Thang chữ

| Token | Cỡ / dòng | Đậm | Giãn chữ | Dùng cho |
|---|---|---|---|---|
| `displayLarge` | 96 / 96 sp | 800 | −4% | Số chính ở S6 |
| `displayMedium` | 56 / 60 sp | 800 | −3% | Quãng đường ở S7, tổng quãng đường ở S8, giờ ở S4 |
| `headlineMedium` | 28 / 34 sp | 700 | −2% | Số phụ ở S6, số trong `GoalPicker` |
| `headlineSmall` | 22 / 28 sp | 700 | −2% | Số trong ô thống kê, giờ trên thẻ |
| `titleLarge` | 22 / 28 sp | 800 | −3% | Tiêu đề màn hình, chữ "RunMate" |
| `titleMedium` | 16 / 22 sp | 600 | 0 | Tiêu đề thẻ, tên địa điểm, tháng trên lịch |
| `bodyLarge` | 16 / 24 sp | 400 | 0 | Nội dung chính, ô nhập |
| `bodyMedium` | 14 / 20 sp | 400 | 0 | Nội dung phụ |
| `labelLarge` | 14 / 20 sp | 600 | 0 | Chữ trên nút |
| `labelMedium` | 12 / 16 sp | 500 | 0 | Nhãn dưới số liệu, thứ trên lịch tuần |
| `labelSmall` | 11 / 14 sp | 600 | +2% | Chip trạng thái, huy hiệu |

Đơn vị đi kèm số (km, /km, °C) dùng cỡ nhỏ hơn số khoảng 40%, đậm 500, màu `textSecondary`. Ví dụ: **10,2** km.

### 5.3 Định dạng số

Theo SPEC 10.5: dấu phẩy thập phân, đơn vị km, phút/km, °C, km/h.

| Loại | Định dạng | Ví dụ |
|---|---|---|
| Quãng đường | 2 chữ số thập phân dưới 10 km, 1 chữ số từ 10 km | `5,24` · `12,4` |
| Thời gian | `m:ss` dưới 1 giờ, `h:mm:ss` từ 1 giờ | `26:55` · `1:04:10` |
| Tổng thời gian (S8) | `h giờ mm phút` | `4 giờ 05 phút` |
| Pace | `m:ss` + "/km", chưa đủ 100 m thì `--` (BR-35) | `5:08 /km` |
| Tỷ lệ | Số nguyên + `%`, có thể trên 100; không có dữ liệu thì `--` (BR-55) | `112%` |
| Ngày | "Thứ Năm, 29/10" | |
| Giờ | 24 giờ | `06:00` |
| Kỳ thống kê | Tuần: "26/10 – 01/11". Tháng: "Tháng 10, 2026" | |

---

## 6. Khoảng cách, bo góc, viền

### 6.1 Khoảng cách (lưới 4 dp)

| Token | Giá trị | Dùng cho |
|---|---|---|
| `space1` | 4 dp | Giữa số và đơn vị |
| `space2` | 8 dp | Giữa các phần tử nhỏ trong một nhóm |
| `space3` | 12 dp | Giữa các thẻ trong danh sách |
| `space4` | 16 dp | **Lề màn hình**, đệm trong thẻ |
| `space5` | 24 dp | Giữa các khối lớn |
| `space6` | 32 dp | Giữa các phần của S6 |
| `space7` | 48 dp | Khoảng trống trên cùng cho trạng thái trống |

### 6.2 Bo góc

| Token | Giá trị | Dùng cho |
|---|---|---|
| `radiusM` | 12 dp | Nút chữ nhật, ô nhập |
| `radiusL` | 16 dp | Thẻ, khối thời tiết, khối cảnh báo, khung quỹ đạo |
| `radiusXL` | 28 dp | Hộp thoại, mép trên của bottom sheet |
| `radiusFull` | 50% | Chip, nút tròn, vòng chạy, viên chọn ngày |

### 6.3 Viền và độ nổi

- Không dùng đổ bóng (elevation 0) ở mọi nơi, kể cả hộp thoại. Hộp thoại phân biệt bằng lớp phủ tối phía sau (scrim 40%).
- Đường kẻ: 1 dp, màu `outline`.
- Tràn viền (edge-to-edge, SPEC 10.5): nội dung cuộn chạy dưới thanh trạng thái, nhưng thanh trên cùng và thanh dưới cùng chừa đủ `WindowInsets`.

---

## 7. Biểu tượng

- Bộ **Material Symbols Outlined**, độ đậm 400, cỡ 24 dp (20 dp trong chip). Nhập vào dự án dưới dạng vector drawable, chỉ những biểu tượng cần dùng.
- Biểu tượng đang được chọn ở thanh dưới dùng bản **Filled**.
- Không dùng emoji.

| Ý nghĩa | Biểu tượng |
|---|---|
| Kế hoạch | `calendar_month` |
| Thống kê | `insights` |
| Chạy | `play_arrow` (trong nút gradient) |
| Thêm kế hoạch | `add` |
| Tuần trước / sau | `chevron_left` / `chevron_right` |
| Vị trí / vị trí hiện tại | `location_on` / `my_location` |
| Nhiệt độ / mưa / gió / UV | `thermostat` / `rainy` / `air` / `wb_sunny` |
| Cảnh báo | `warning` |
| Tạm dừng / tiếp tục / kết thúc | `pause` / `play_arrow` / `stop` |
| Đạt mục tiêu | `check` |
| Lõi vòng chạy khi thiếu dữ liệu | `directions_run` |
| Xoá | `delete` |
| GPS | `satellite_alt` |
| Tiết kiệm pin | `battery_saver` |
| Áo mưa / mũ / kem chống nắng / nước / áo khoác | `umbrella` / `styler` / `sanitizer` / `water_bottle` / `checkroom` |

---

## 8. Thành phần dùng chung

Mỗi thành phần ghi rõ các trạng thái. Đặt trong `ui/components/`.

### 8.1 `RunButton`: nút Chạy ở thanh dưới

- Hình tròn 56 dp. Ngoài cùng là vòng gradient dày 3 dp, rồi khe hở 3 dp màu `surface`, giữa là hình tròn `inverse` với biểu tượng `play_arrow` màu `onInverse`. Cùng cấu tạo với vòng chạy, để người dùng hiểu "bấm vào đây sẽ tạo ra vòng màu".
- **Đang có buổi chạy dở** (`RECORDING` hoặc `PAUSED`): vòng xoay chậm (mục 11), biểu tượng đổi thành chấm tròn. Bấm mở thẳng S6 (SPEC 9.3).
- `contentDescription`: "Chạy" hoặc "Về buổi chạy đang diễn ra".

### 8.2 `BottomBar`: thanh dưới

- Cao 64 dp + inset của thanh điều hướng hệ thống. Nền `surface`, kẻ trên 1 dp `outline`.
- 3 mục: **Kế hoạch** · `RunButton` · **Thống kê** (SPEC 9.3). Hai tab hiện biểu tượng **và** chữ `labelMedium`.
- Chỉ hiện ở S1 và S8.

### 8.3 `WeekCalendar`: lịch tuần

Phần đầu của S1 (SPEC 9.1).

```
Tháng 10, 2026            [Hôm nay]  ‹  ›
 T2    T3    T4    T5    T6    T7    CN
 ◎     ◌     ⊘     ◎✓    ◌     ·     ·      ← RunRing cỡ S
 26    27    28   (29)   30    31    01      ← (29) = ngày đang chọn
```

- **Hàng tiêu đề:** tên tháng (`titleMedium`). Tuần vắt qua hai tháng thì ghi "Tháng 10 – 11, 2026". Bên phải: chip **Hôm nay** (chỉ hiện khi đang không ở tuần này, hoặc đang không chọn hôm nay), rồi hai nút `‹` `›` 48 dp.
- **7 cột** T2 → CN chia đều chiều rộng (khoảng 46 dp mỗi cột ở màn 360 dp), không cuộn.
- Mỗi cột từ trên xuống: thứ (`labelMedium`), `RunRing` cỡ S (mục 3.2), số ngày (`labelLarge`, `tnum`).
- **Ngày đang chọn:** số ngày nằm trong viên `inverse` cao 24 dp, chữ `onInverse`. Mặc định chọn hôm nay (SPEC 9.1).
- **Hôm nay** (khi không được chọn): thứ và số ngày đậm 700, màu `textPrimary`.
- Cả cột là một vùng bấm (≥ 48 dp cao). Bấm thì đổi ngày đang chọn, danh sách bên dưới đổi theo.
- **Giới hạn:** lùi không giới hạn. `›` bị vô hiệu hoá ở tuần chứa hôm nay + 16 ngày (BR-02). Bấm vào nút bị tắt thì hiện snackbar "Chỉ lên kế hoạch được trong 16 ngày tới".
- Vuốt ngang trên lịch cũng chuyển tuần, chuyển cảnh trượt 250 ms.
- `contentDescription` mỗi cột: "Thứ Năm 29/10, đã chạy 1 buổi, đạt mục tiêu" / "Thứ Sáu 30/10, 1 kế hoạch" / "Thứ Bảy 31/10, không có lịch".

### 8.4 `ActiveRunBanner`: băng "Đang có buổi chạy"

- Viên thuốc (pill) cao 52 dp, nền `inverse`, chữ `onInverse`, lề ngang 16 dp. Nằm ngay dưới lịch tuần.
- Trái: chấm gradient 10 dp nhấp nháy. Giữa: "Đang chạy · 3,2 km · 18:40" (`labelLarge`, `tnum`). Phải: `chevron_right`.
- Khi `PAUSED`: chấm đứng yên màu `textSecondary`, chữ "Tạm dừng · 3,2 km · 18:40".
- Bấm mở S6.

### 8.5 `PlanCard`: thẻ kế hoạch chưa chạy

Cho `PLANNED`, `IN_PROGRESS`, `MISSED`, `CANCELLED`. Bấm cả thẻ mở S4.

```
┌──────────────────────────────────────────┐
│ 06:00   Hồ Tây                     [Chip]│
│         Mục tiêu 5 km                    │
└──────────────────────────────────────────┘
```

- Nền `surfaceContainer`, bo `radiusL`, đệm 16 dp, chiều cao tối thiểu 72 dp.
- Cột trái rộng 56 dp: giờ (`headlineSmall`, `tnum`).
- Giữa: tên địa điểm (`titleMedium`, cắt "…"), mục tiêu (`bodyMedium`, `textSecondary`).
- Phải: `StatusChip` (mục 8.8).
- **Không hiện thời tiết** trên thẻ (SPEC 9.1). Thời tiết xem ở S4.
- `CANCELLED`: cả thẻ giảm độ đậm còn 60%.

### 8.6 `ResultCard`: thẻ buổi đã chạy

Thẻ đặc biệt nhất ở S1, dùng cho **kế hoạch `COMPLETED`** và **buổi chạy tự do**. Điểm khác với `PlanCard`: có **vòng chạy cỡ M** như avatar, và có **kết quả thật**.

```
┌──────────────────────────────────────────┐
│ (◎✓)  06:00 · Hồ Tây          [Hoàn thành]│
│       5,24 km · 105% mục tiêu ✓          │
│                          Xem kết quả  ›  │
└──────────────────────────────────────────┘
```

| Phần | Kế hoạch `COMPLETED` | Chạy tự do |
|---|---|---|
| Trái | `RunRing` cỡ M | `RunRing` cỡ M |
| Dòng 1 | Giờ kế hoạch · tên địa điểm | Giờ bắt đầu · "Chạy tự do" |
| Dòng 2 | Quãng đường · tỷ lệ mục tiêu (BR-37) | Quãng đường · thời gian chạy |
| Chip | "Hoàn thành" | Không có chip (SPEC 6: chạy tự do không có trạng thái) |
| Nút "Xem kết quả" | Mở S7 | Mở S7 |
| Bấm cả thẻ | Mở S4 | Mở S7 |

- Tỷ lệ ≥ 100%: số % màu `success` + biểu tượng `check` 16 dp. Dưới 100%: màu `textSecondary`, không có biểu tượng.
- "Xem kết quả" là nút chữ (`labelLarge`) cao 48 dp ở góc dưới phải.
- Nền thẻ **vẫn là `surfaceContainer`** như mọi thẻ khác. Sự đặc biệt nằm ở vòng chạy, không ở nền thẻ.

### 8.7 `DayList`: danh sách của ngày đang chọn

- Tiêu đề: "Thứ Năm, 29/10" (`titleMedium`), bên phải là tóm tắt nhỏ "2 buổi · 15,4 km" nếu ngày đó có buổi chạy (`labelMedium`, `textSecondary`).
- Kế hoạch và buổi chạy **trộn chung, xếp theo giờ tăng dần** (BR-50). Kế hoạch `COMPLETED` và buổi chạy của nó là **một thẻ** `ResultCard`, không hiện hai lần.
- Ngày được tính theo múi giờ của máy (BR-50).

| Trạng thái ngày | Hiển thị |
|---|---|
| Trống, ngày trong khoảng đặt được (hôm nay → +16 ngày) | "Chưa có lịch" + nút viền "Lên kế hoạch cho ngày này" (mở S2 điền sẵn ngày) |
| Trống, ngày đã qua | "Không có buổi chạy nào" |
| Trống, hôm nay | "Hôm nay chưa có lịch" + hai nút: "Lên kế hoạch" và "Chạy ngay" (viền gradient) |

### 8.8 `StatusChip`: nhãn trạng thái kế hoạch

Cao 24 dp, bo `radiusFull`, chữ `labelSmall`. Luôn có chữ, không chỉ có màu.

| Trạng thái | Chữ (SPEC 6.1) | Kiểu |
|---|---|---|
| `PLANNED` | Đã lên kế hoạch | Viền nét đứt 1 dp `textSecondary`, chữ `textPrimary` (cùng ngôn ngữ với vòng nét đứt) |
| `IN_PROGRESS` | Đang chạy | Viền 1 dp `outlineStrong`, chấm gradient 6 dp trước chữ |
| `COMPLETED` | Hoàn thành | Nền `inverse`, chữ `onInverse`, biểu tượng `check` |
| `MISSED` | Bỏ lỡ | Viền 1 dp `error`, chữ `error` |
| `CANCELLED` | Đã huỷ | Nền `surfaceContainerHigh`, chữ `textSecondary` |

### 8.9 `GoalRing`: vòng tiến độ

- Tham số: `progress` (0 trở lên, có thể > 1), `size`, `strokeWidth`.
- Nền vòng màu `ringTrack`. Phần đã đạt là cung gradient xoay, bắt đầu từ 12 giờ, đầu cung bo tròn.
- `progress ≥ 1`: vòng đầy. Không vẽ vòng thứ hai. Con số % cho biết đã vượt bao nhiêu.
- Cỡ dùng: 280 dp / nét 10 dp (S6), 96 dp / nét 8 dp (S7, S8).
- Luôn đi kèm con số, vì vòng gradient không đủ tương phản để tự mang thông tin.
- Khác `RunRing`: `GoalRing` thể hiện **tỷ lệ** (có phần track xám), `RunRing` thể hiện **sự kiện đã chạy** (luôn đầy hoặc chia đoạn).

### 8.10 `WeatherBlock`: khối thời tiết

Dùng ở S2, S4, S5, S7. Nền `surfaceContainer`, bo `radiusL`, đệm 16 dp.

- Lưới 4 cột: **Nhiệt độ**, **Mưa**, **Gió**, **UV**. Mỗi ô: biểu tượng 20 dp, số `headlineSmall`, nhãn `labelMedium`.
- Ô vượt ngưỡng (BR-10 đến BR-13): biểu tượng và số đổi màu `warning`, thêm chấm nhỏ trên góc biểu tượng.
- Dòng dưới cùng (`labelMedium`, `textSecondary`): "Dự báo cho 06:00 · cập nhật lúc 05:12".

| Trạng thái | Hiển thị |
|---|---|
| Đang tải | 4 ô xương (skeleton) màu `surfaceContainerHigh`, nhấp nháy nhẹ |
| Có dữ liệu | Như trên |
| Dữ liệu cũ, không có mạng | Như trên + biểu tượng `cloud_off` cạnh dòng "cập nhật lúc …" |
| Lỗi, chưa từng có dữ liệu | Nền `errorContainer`: "Không lấy được dự báo thời tiết." + nút chữ **Thử lại**. Không chặn việc lưu hay chạy |
| Chưa đủ thông tin | Chữ `textSecondary`: "Chọn ngày, giờ và vị trí để xem thời tiết" |
| Kế hoạch `COMPLETED` / `CANCELLED`, và S7 | Chỉ hiện dữ liệu đã lưu, không có nút Thử lại (SPEC 5.2) |

### 8.11 `WarningList`: danh sách cảnh báo

- Nền `warningContainer`, bo `radiusL`, đệm 12 dp.
- Mỗi dòng: biểu tượng `warning` màu `warning` + chữ `bodyMedium` đậm 600: "Có thể mưa (70%)", "Quá nóng (36°C)", "Gió mạnh (32 km/h)", "UV cao (9)".
- Hiện **tất cả** cảnh báo (SPEC 5.2). Không có cảnh báo thì không hiện khối này.

### 8.12 `GearChips`: gợi ý vật dụng

- Tiêu đề nhỏ "Nên mang theo" (`labelMedium`, `textSecondary`).
- Hàng chip bọc dòng, mỗi chip: biểu tượng + chữ, viền 1 dp `outlineStrong`, cao 32 dp.
- Gộp trùng: "Mũ" từ BR-21 và BR-22 chỉ hiện một lần; "Nước uống" từ BR-22 và BR-24 cũng vậy.
- Không có gợi ý nào thì không hiện khối này.

### 8.13 `GoalPicker`: chọn mục tiêu

- Nút chọn 2 phần (segmented): **Quãng đường** | **Thời gian**. Phần được chọn nền `inverse`.
- Bên dưới: số lớn `headlineMedium` ở giữa, hai nút tròn `−` và `+` 48 dp hai bên. Bấm số để gõ trực tiếp.
- Bước tăng: 0,1 km hoặc 1 phút (BR-04, BR-05). Giữ nút để tăng nhanh: 1 km hoặc 5 phút.
- Dòng gợi ý dưới (`labelMedium`): "Từ 0,5 đến 100 km" hoặc "Từ 5 phút đến 12 giờ". Vượt giới hạn thì dòng này đổi màu `error`.
- Hàng chip chọn nhanh: 3 km · 5 km · 10 km · 21,1 km (hoặc 20 · 30 · 45 · 60 phút).

### 8.14 `LocationRow`: ô vị trí

- Cao 56 dp, nền `surfaceContainer`, bo `radiusM`. Trái: `location_on`. Giữa: tên địa điểm (`bodyLarge`). Phải: `chevron_right`.
- Chưa chọn: chữ `textSecondary` "Chọn vị trí". Bấm mở S3.

### 8.15 `StartRunButton`: nút Bắt đầu chạy

- Rộng hết lề, cao 56 dp, bo `radiusFull`.
- Viền gradient 2 dp, nền `inverse`, chữ `onInverse` "Bắt đầu chạy" + biểu tượng `play_arrow`.
- Bị vô hiệu hoá: viền `outline`, nền `surfaceContainerHigh`, chữ `textDisabled`, và **luôn có một dòng giải thích** ngay bên dưới (SPEC 6.4).

### 8.16 `StatTile`: ô thống kê

- Nhãn (`labelMedium`, `textSecondary`) ở trên, số (`headlineSmall`, `tnum`) ở dưới, đơn vị nhỏ bên phải số.
- Thường xếp lưới 2 hoặc 3 cột, cách nhau 8 dp.

### 8.17 `TrackCanvas`: vẽ quỹ đạo

- Vẽ bằng Compose Canvas (SPEC F4), không có bản đồ nền.
- Nền `surfaceContainer`, bo `radiusL`, đệm trong 24 dp. Giữ đúng tỷ lệ Bắc–Nam / Đông–Tây (chiếu Mercator đơn giản), căn giữa.
- Nét 5 dp, đầu và góc bo tròn, màu theo pace (mục 2.4).
- **Mỗi `segmentIndex` là một đường riêng, không nối với nhau** (BR-33).
- Điểm đầu: tròn rỗng 12 dp, viền 3 dp `nhipBlue`. Điểm cuối: tròn đặc 12 dp màu `nhipPink`, viền 3 dp `surface`.
- Chú thích ở góc trên trái: thanh gradient 40×6 dp + chữ "chậm → nhanh".
- Không đủ điểm (UC5): biểu tượng `route` + "Buổi chạy quá ít dữ liệu để vẽ quỹ đạo".
- Bản thu nhỏ trong lõi `RunRing` dùng chung hàm chiếu toạ độ nhưng một màu `textPrimary`, không vẽ điểm đầu/cuối.
- Nhiều điểm (tới ~21 600, SPEC 10.2): giảm điểm khi vẽ (ví dụ thuật toán Ramer–Douglas–Peucker) **sau khi đo**, không làm trước. Bản thu nhỏ thì luôn giảm điểm (khoảng 100 điểm là đủ).

### 8.18 `PeriodSwitcher`: chọn kỳ thống kê

- Nút chọn 2 phần **Tuần** | **Tháng**, rồi hàng `‹  26/10 – 01/11  ›`.
- `›` bị vô hiệu hoá ở kỳ hiện tại. Không có giới hạn lùi.
- Chip "Kỳ này" hiện khi đang xem kỳ cũ.

### 8.19 `EmptyState`: trạng thái trống

- Căn giữa, cách trên 48 dp. Hình minh hoạ: một `RunRing` cỡ lớn (72 dp) vẽ **nét đứt** xám, lõi trống. Ý nghĩa: "vòng của bạn đang chờ được tô màu".
- Tiêu đề `titleMedium`, mô tả `bodyMedium` `textSecondary`, một nút hành động.

---

## 9. Từng màn hình

Chiều rộng tham chiếu: 360–412 dp. Chỉ màn hình dọc (SPEC 10.5).

### S1 · Kế hoạch (màn hình chính)

```
RunMate                               [+]
──────────────────────────────────────────
Tháng 10, 2026            [Hôm nay]  ‹  ›
 T2    T3    T4    T5    T6    T7    CN
 ◎     ◌     ⊘     ◎✓    ◌     ·     ·
 26    27    28   (29)   30    31    01
──────────────────────────────────────────
[● Đang chạy · 3,2 km · 18:40          ›]   ← chỉ khi có buổi chạy dở
Thứ Năm, 29/10             2 buổi · 15,4 km
[ResultCard  06:00 · Hồ Tây · Hoàn thành ]
[ResultCard  17:30 · Chạy tự do          ]
[PlanCard    19:00 · Công viên Thống Nhất]
──────────────────────────────────────────
   Kế hoạch        (RUN)        Thống kê
```

- **Thanh trên:** chữ "RunMate" (`titleLarge`, đậm 800) bên trái; nút `add` 48 dp bên phải ("Tạo kế hoạch"). Nếu đang chọn một ngày trong khoảng đặt được, S2 mở với ngày đó điền sẵn.
- `WeekCalendar` (mục 8.3) → `ActiveRunBanner` nếu có → `DayList` (mục 8.7).
- Lịch tuần **không cuộn theo** danh sách: giữ cố định ở trên, chỉ danh sách cuộn.
- Hộp thoại khôi phục buổi chạy dở hiện ở đây (mục 10).
- **Lần đầu dùng app** (chưa có kế hoạch hay buổi chạy nào): lịch tuần vẫn hiện (các ngày không có vòng), `DayList` thay bằng `EmptyState`: "Tuần của bạn đang trống" · "Mỗi buổi chạy sẽ tô màu một vòng trên lịch." · nút **Lên kế hoạch đầu tiên**.

### S2 · Tạo / Sửa kế hoạch

- **Thanh trên:** nút đóng `close` bên trái, tiêu đề "Kế hoạch mới" hoặc "Sửa kế hoạch".
- **Nội dung (cuộn):**
  1. Ngày và Giờ: hai ô cạnh nhau, mở `DatePicker` và `TimePicker` của Material 3. Ngày ngoài khoảng hôm nay → +16 ngày bị làm mờ, không chọn được (BR-02).
  2. `LocationRow`
  3. `GoalPicker`
  4. `WeatherBlock` → `WarningList` → `GearChips` (hiện khi đủ ngày, giờ, vị trí)
- **Đáy cố định:** nút chính "Lưu kế hoạch" (nền `inverse`, cao 56 dp, bo `radiusFull`). Không có gradient vì lưu kế hoạch chưa phải là chạy.
- **Lỗi nhập:** chữ `error` ngay dưới ô sai: "Giờ bắt đầu đã qua" (BR-01), "Chỉ đặt được trong 16 ngày tới" (BR-02).
- Lưu xong về S1, lịch tuần nhảy tới ngày của kế hoạch và ngày đó hiện vòng nét đứt.

### S3 · Chọn vị trí

- **Thanh trên:** nút `arrow_back`, ô tìm kiếm chiếm hết phần còn lại, tự mở bàn phím.
- Dòng đầu tiên cố định: `my_location` + "Dùng vị trí hiện tại" (`titleMedium`).
- Kết quả: mỗi dòng cao 64 dp, tên (`bodyLarge`) + mô tả cấp trên (`bodyMedium`, `textSecondary`), ví dụ "Hồ Hoàn Kiếm" / "Hoàn Kiếm, Hà Nội".

| Trạng thái | Hiển thị |
|---|---|
| Chưa gõ | Chỉ có dòng "Dùng vị trí hiện tại" |
| Đang tìm | Thanh tiến trình mảnh dưới ô tìm |
| Không có kết quả | "Không tìm thấy “…”. Thử tên quận hoặc thành phố." |
| Mất mạng | "Cần mạng để tìm theo tên. Bạn vẫn có thể dùng vị trí hiện tại." (SPEC 10.3) |
| Đang lấy vị trí hiện tại | Dòng đầu hiện vòng xoay nhỏ thay biểu tượng |

### S4 · Chi tiết kế hoạch

- **Thanh trên:** `arrow_back`; menu `more_vert` gồm **Sửa**, **Huỷ kế hoạch**, **Xoá** (chữ `error`). Mục bị tắt theo SPEC 6.4 vẫn hiện, làm mờ, kèm dòng phụ giải thích.
- **Phần đầu:** `StatusChip`, rồi "Thứ Năm, 29/10" (`titleMedium`), giờ "06:00" (`displayMedium`), tên địa điểm, mục tiêu.
- **Kế hoạch `COMPLETED`:** ngay dưới phần đầu có một hàng kết quả thu gọn: `RunRing` cỡ M + "5,24 km · 105% mục tiêu ✓" + "Xem buổi chạy ›" (mở S7).
- `WeatherBlock` → `WarningList` → `GearChips`.
- Khối cảnh báo tiết kiệm pin (BR-61) nếu cần, kiểu giống `WarningList` với biểu tượng `battery_saver`.
- **Đáy cố định:**
  - `PLANNED`: `StartRunButton`.
  - `MISSED`: `StartRunButton` với chữ "Chạy muộn".
  - `IN_PROGRESS`: nút "Về buổi chạy" mở S6.
  - `COMPLETED`: không có nút đáy (đã có hàng kết quả ở trên).
  - `CANCELLED`: không có nút chạy. Dòng gợi ý: "Sửa sang giờ ở tương lai để lên lịch lại".

### S5 · Chuẩn bị chạy tự do

Mở chồng lên từ nút Chạy, trượt từ dưới lên.

- **Thanh trên:** `close`, tiêu đề "Chạy ngay".
- **Gợi ý kế hoạch** (BR-44), nếu có, nằm trên cùng: thẻ viền nét đứt 1,5 dp `textSecondary` (cùng ngôn ngữ với kế hoạch chưa chạy), nền `surface`.
  - "Bạn có kế hoạch lúc 06:00 · 5 km. Chạy theo kế hoạch này?"
  - Hai nút: **Chạy theo kế hoạch** (nền `inverse`) · **Bỏ qua** (nút chữ).
- `LocationRow` (mặc định "Vị trí hiện tại") → `GoalPicker` → `WeatherBlock` → `WarningList` → `GearChips` → cảnh báo tiết kiệm pin nếu có.
- **Đáy cố định:** `StartRunButton`.

### S6 · Đang chạy

Màn hình quan trọng nhất. Mục tiêu: **liếc 1 giây là đọc được**.

```
[● Đang ghi]                     [GPS ●]
             ╭───────────╮
            ╱             ╲
           │     5,24      │   ← GoalRing 280 dp
           │      km       │
            ╲  62% · còn  ╱
             ╰─ 3,2 km ──╯
     26:55           5:08
   Thời gian      Pace /km
──────────────────────────────────────────
       (■)              ( ❚❚ )
     Kết thúc          Tạm dừng
```

- **Số chính** (`displayLarge`) là chỉ số của mục tiêu: mục tiêu `DISTANCE` thì số chính là quãng đường, `DURATION` thì là thời gian chạy. Chỉ số còn lại và pace xuống hàng dưới (`headlineMedium`).
- **`GoalRing`** bao quanh số chính. Dưới số: "62% · còn 3,2 km" (`labelLarge`).
- **Pace** chưa đủ 100 m hiện `--` (BR-35).
- **Pill trạng thái** góc trên trái: "Đang ghi" với chấm gradient nhấp nháy, hoặc "Tạm dừng" với chấm `textSecondary`. Góc phải: tín hiệu GPS (`satellite_alt` + chấm `success`, hoặc `warning` khi điểm GPS bị lọc liên tục theo BR-30).
- **Nút điều khiển** cố định ở đáy, trên inset hệ thống:
  - `RECORDING`: **Tạm dừng** 96 dp ở giữa (vòng gradient 4 dp, nền `surface`, biểu tượng `pause` 40 dp); **Kết thúc** 64 dp bên trái (nền `surfaceContainerHigh`, `stop`).
  - `PAUSED`: nút giữa đổi thành **Tiếp tục** (nền gradient đầy, biểu tượng `play_arrow` màu `#111111` để đủ tương phản), Kết thúc giữ nguyên. Các con số giảm độ đậm còn 50%, cung gradient đứng yên.
  - Mỗi nút có chữ nhãn bên dưới (`labelMedium`). Kích thước đều vượt 48 dp (SPEC 10.6).
- **Đạt mục tiêu** (BR-38): vòng đầy, phóng 1,0 → 1,06 → 1,0, rung một lần, băng nhỏ phía trên số chính "Đạt mục tiêu 5 km" với dấu ✓ `success` trong 4 giây. Không dừng buổi chạy.
- Cảnh báo tiết kiệm pin bật giữa chừng (BR-61): băng `warningContainer` dưới thanh trên, có nút đóng.
- **Không có bản đồ trực tiếp** trong MVP (v1.2).
- **Màn hình tắt theo hệ thống** (D2): S6 không giữ màn hình luôn sáng. Người chạy thường để máy tự tắt màn hình, khi muốn xem thì bật lên. Vì vậy:
  - Khi màn hình bật lại, số liệu hiện **ngay giá trị mới nhất** (đọc từ Room qua Flow, SPEC 8.5). `GoalRing` nhảy thẳng tới tỷ lệ hiện tại, không quét lại từ giá trị cũ, để liếc 1 giây là đọc được.
  - Thông báo của foreground service (SPEC 8.3) là nơi xem nhanh khi màn hình khoá: tiêu đề "Đang chạy · 5,24 km", nội dung "26:55 · 5:08 /km · 62% mục tiêu"; khi tạm dừng tiêu đề đổi thành "Tạm dừng". Bấm vào mở S6.
- **Sáng hay tối theo máy** (D3): S6 dùng đúng chế độ sáng/tối của hệ thống như mọi màn hình khác (SPEC 10.5), không ép nền tối. Cả hai bảng màu phải đọc rõ ngoài trời.

### S7 · Chi tiết buổi chạy

Đây là nơi diễn ra **khoảnh khắc hoàn thành** (mục 3.4) khi mở từ S6.

- **Thanh trên:** `arrow_back`; menu `more_vert` có **Xoá buổi chạy** (chữ `error`). S7 là nơi duy nhất xoá buổi chạy (SPEC UC6).
- **Phần đầu:** "Thứ Năm, 29/10 · 06:02" (`labelMedium`), tên địa điểm (`titleMedium`), nhãn "Theo kế hoạch" hoặc "Chạy tự do".
- `TrackCanvas` vuông, rộng hết lề.
- Hàng kết quả: quãng đường lớn (`displayMedium`) bên trái; `GoalRing` 96 dp bên phải, trong vòng ghi tỷ lệ "105%". Đạt mục tiêu thì dưới vòng có "Đạt mục tiêu" màu `success` + ✓.
- Lưới `StatTile` 3 cột: Thời gian chạy · Pace trung bình · Mục tiêu.
- `WeatherBlock` chỉ đọc, tiêu đề "Thời tiết lúc bắt đầu".
- Mở từ S6: chạy hoạt ảnh mục 3.4 bước 1. Mở từ S1 hoặc S4: hiện thẳng, không hoạt ảnh.

### S8 · Thống kê

SPEC: chỉ có số liệu, **chưa có biểu đồ**.

```
Thống kê
[ Tuần | Tháng ]
‹      26/10 – 01/11      ›
──────────────────────────────────────────
Tổng quãng đường
32,6 km                                      ← displayMedium
┌────────────┬────────────┬───────────┐
│ Số buổi    │ Thời gian  │ Pace TB   │
│ 5          │ 2 giờ 51   │ 5:15 /km  │
└────────────┴────────────┴───────────┘
Giữ lịch
 (  80%  )    4 hoàn thành · 1 bỏ lỡ        ← GoalRing 96 dp
──────────────────────────────────────────
   Kế hoạch        (RUN)        Thống kê
```

- **Thanh trên:** "Thống kê" (`titleLarge`).
- `PeriodSwitcher` (mục 8.18).
- **Số lớn:** tổng quãng đường (`displayMedium`).
- **Lưới 3 `StatTile`:** số buổi chạy, tổng thời gian, pace trung bình (BR-56).
- **Khối Giữ lịch:** `GoalRing` 96 dp với tỷ lệ ở giữa (BR-55), bên phải là "4 hoàn thành · 1 bỏ lỡ". Kỳ không có kế hoạch nào: vòng chỉ có track xám, giữa ghi `--`, chữ "Chưa có kế hoạch trong kỳ này".
- Đổi kỳ: các con số đếm lên từ giá trị cũ tới giá trị mới trong 400 ms, vòng quét tới tỷ lệ mới.
- **Kỳ trống** (không có buổi chạy): các số là `0` và pace `--`, kèm dòng "Chưa có buổi chạy nào trong kỳ này".

---

## 10. Hộp thoại

Dùng `AlertDialog` của Material 3: bo `radiusXL`, nền `surface`, không đổ bóng. Tiêu đề `titleMedium`, nội dung `bodyMedium`. Nút xác nhận ở bên phải. Nút phá huỷ dùng chữ màu `error`.

| Hộp thoại (SPEC 9.2) | Ở đâu | Tiêu đề | Nội dung | Nút |
|---|---|---|---|---|
| Xác nhận xoá | S4, S7 | Xoá kế hoạch? / Xoá buổi chạy? | Không khôi phục được. | Huỷ · **Xoá** |
| Thông báo giữ buổi chạy | S4 | Xoá kế hoạch? | Buổi chạy lúc 06:02 (5,24 km) sẽ được giữ lại dưới dạng chạy tự do. | Huỷ · **Xoá kế hoạch** |
| Kế hoạch đổi trạng thái | S7 | Xoá buổi chạy này? | Kế hoạch lúc 06:00 sẽ quay về "Đã lên kế hoạch" (hoặc "Bỏ lỡ"). Không khôi phục được. | Huỷ · **Xoá** |
| Giải thích quyền | S4, S5 | Cần quyền vị trí | RunMate dùng GPS để đo quãng đường. Điểm GPS chỉ lưu trên máy bạn. | Để sau · **Mở cài đặt** |
| Xác nhận kết thúc | S6 | Kết thúc buổi chạy? | 5,24 km · 26:55 | Chạy tiếp · **Kết thúc** |
| Buổi chạy quá ngắn | S6 | Buổi chạy quá ngắn | Dưới 100 m hoặc dưới 1 phút nên không được lưu. | **Đã hiểu** |
| Lỗi thời tiết | S2, S4, S5 | Không lấy được thời tiết | Kiểm tra kết nối mạng. Bạn vẫn có thể tiếp tục. | Tiếp tục không có thời tiết · **Thử lại** |
| Tiết kiệm pin | S4, S5, S6 | Tiết kiệm pin đang bật | GPS có thể bị tắt khi khoá màn hình, quãng đường sẽ không chính xác. Nên tắt khi chạy. | Vẫn chạy · **Mở cài đặt** |
| Khôi phục buổi chạy dở | S1 | Buổi chạy chưa kết thúc | Buổi chạy lúc 06:02 (3,2 km · 18:40) chưa kết thúc. | Kết thúc · **Tiếp tục** (ẩn nếu quá 1 giờ, BR-60) |

Mọi chữ trong bảng đặt trong `strings.xml` (SPEC 10.5).

Xoá kế hoạch `COMPLETED` ở S4: vòng chạy của ngày đó **vẫn giữ màu** (buổi chạy còn, chỉ thành chạy tự do), thẻ trong danh sách đổi từ "Hoàn thành" sang thẻ chạy tự do.

---

## 11. Chuyển động và rung

| Token | Thời lượng | Đường cong | Dùng cho |
|---|---|---|---|
| `motionFast` | 150 ms | `FastOutSlowIn` | Đổi trạng thái nút, chip, ngày đang chọn |
| `motionMedium` | 250 ms | `FastOutSlowIn` | Chuyển màn hình, chuyển tuần, mở hộp thoại |
| `motionSlow` | 400 ms | `EaseOutCubic` | Vòng tiến độ đổi giá trị, số đếm ở S8 |

| Hiệu ứng | Mô tả |
|---|---|
| Chấm "đang ghi" | Độ mờ 100% → 40% → 100%, chu kỳ 1,2 giây, lặp |
| Vòng xoay (`RunButton`, ngày đang chạy) | Gradient xoay 360° mỗi 4 giây, tuyến tính |
| Vòng tiến độ ở S6 | Mỗi lần số liệu cập nhật thì cung nhích tới giá trị mới trong 400 ms |
| Đạt mục tiêu ở S6 | Cung chạy nốt tới đầy, vòng phóng 1,0 → 1,06 → 1,0 trong 500 ms |
| Quỹ đạo tự vẽ ở S7 | Cắt dần đường (path trim) từ 0 → 100% trong 1 000 ms, theo thứ tự thời gian |
| Vòng chạy vẽ vào ở S1 | Gradient quét 0° → 360° trong 600 ms, rồi huy hiệu ✓ phóng 0 → 1,15 → 1,0 trong 300 ms |
| Chuyển tuần | Lịch trượt ngang 250 ms theo hướng vuốt |
| Mở S5 | Trượt từ dưới lên, 250 ms |
| Ô xương (skeleton) | Độ mờ 60% ↔ 100%, chu kỳ 1 giây |

**Rung:**

- Qua mỗi km (hoặc mỗi 5 phút với mục tiêu thời gian): rung ngắn một lần.
- Đạt mục tiêu (S6, và S7 lúc vừa kết thúc): rung hai lần.
- Tạm dừng / Tiếp tục: rung ngắn khi bấm.

**Giảm chuyển động:** khi người dùng tắt hoạt ảnh trong hệ thống (`ANIMATOR_DURATION_SCALE` = 0), tắt mọi hiệu ứng lặp và hiệu ứng ăn mừng, hiện thẳng trạng thái cuối.

---

## 12. Khả năng truy cập

- Vùng chạm tối thiểu **48 × 48 dp** ở mọi nơi; nút điều khiển ở S6 lớn hơn (64 và 96 dp).
- Chữ thường đạt tương phản ≥ 4,5:1, chữ lớn ≥ 3:1. Không đặt chữ lên gradient, trừ biểu tượng `#111111` trên nút Tiếp tục (đạt ≥ 4,5:1 trên mọi điểm của dải).
- Gradient và màu ngữ nghĩa **không bao giờ là nguồn thông tin duy nhất**:
  - Vòng chạy khác vòng kế hoạch bằng **hình dạng** (vòng liền có quỹ đạo bên trong vs. vòng nét đứt có số ngày), không chỉ bằng màu.
  - Đạt mục tiêu có huy hiệu ✓, không chỉ có màu.
  - Vòng tiến độ đi kèm %, trạng thái đi kèm chữ, cảnh báo đi kèm biểu tượng và chữ.
- Biểu tượng không có chữ có `contentDescription` (SPEC 10.6). Biểu tượng chỉ để trang trí đặt `contentDescription = null`.
- Mỗi cột của lịch tuần đọc thành một câu (mục 8.3).
- S6: gộp mỗi cặp số + nhãn thành một nút ngữ nghĩa ("Quãng đường 5,24 km"). Đạt mục tiêu thì thông báo qua `LiveRegion` lịch sự.
- Chữ dùng đơn vị `sp`. Kiểm tra ở cỡ chữ hệ thống 200%: số chính ở S6 được phép co lại để vừa màn hình, không được cắt. Lịch tuần ở cỡ chữ lớn: ẩn chữ thứ, giữ vòng và số ngày.

---

## 13. Cách làm trong Compose

### 13.1 File

| File | Chứa |
|---|---|
| `ui/theme/Color.kt` | Hằng số màu ở mục 2, 4 |
| `ui/theme/Type.kt` | `FontFamily` Inter, `Typography` ở mục 5.2 |
| `ui/theme/Shape.kt` | `Shapes` ở mục 6.2 |
| `ui/theme/Dimens.kt` | Khoảng cách mục 6.1, cỡ thành phần (cỡ S/M của vòng chạy…) |
| `ui/theme/RunMateColors.kt` | Màu ngoài Material 3: gradient, `ringTrack`, `warning`, `success` |
| `ui/theme/Theme.kt` | `RunMateTheme`: chọn bảng sáng/tối, cung cấp `RunMateColors` qua `CompositionLocal` |
| `ui/components/RunRing.kt` | Vòng chạy (mục 3) |
| `ui/components/…` | Các thành phần khác ở mục 8, mỗi thành phần một file |
| `domain/…` | Hàm thuần: trạng thái của một ngày (bảng 3.2), tô màu theo pace (mục 2.4), giảm điểm quỹ đạo |

Mỗi thành phần có `@Preview` cho cả chế độ sáng và tối và cho **từng trạng thái** (với `RunRing`: đủ 6 dòng của bảng 3.2).

### 13.2 Trạng thái của một ngày

Tính trong `domain/` để viết unit test (SPEC 8.5, 10.7):

```kotlin
sealed interface DayRingState {
    data object Empty : DayRingState                       // không có gì hoặc chỉ CANCELLED
    data object Planned : DayRingState                     // vòng nét đứt
    data object Missed : DayRingState                      // vòng xám + "!"
    data class Ran(
        val runCount: Int,                                 // 1 = vòng đầy, >1 = chia đoạn (tối đa 4)
        val allGoalsReached: Boolean,                      // huy hiệu ✓
        val thumbnailSessionId: Long,                      // buổi dài nhất, vẽ trong lõi
    ) : DayRingState
    data object Running : DayRingState                     // vòng xoay
}

// Ưu tiên: Running > Ran > Planned > Missed > Empty
fun dayRingState(plans: List<RunPlan>, sessions: List<RunSession>): DayRingState
```

### 13.3 Gradient

```kotlin
val NhipStops = arrayOf(
    0.0f to Color(0xFF3B82F6),
    0.2f to Color(0xFF22D3EE),
    0.4f to Color(0xFFA3E635),
    0.6f to Color(0xFFFACC15),
    0.8f to Color(0xFFF97316),
    1.0f to Color(0xFFEC4899),
)

// Vòng tròn: sweepGradient, xoay canvas -90° để bắt đầu từ 12 giờ
fun nhipSweep(center: Offset) = Brush.sweepGradient(*NhipStops, center = center)

// Hình chữ nhật: chéo từ dưới-trái lên trên-phải
fun nhipLinear(size: Size) = Brush.linearGradient(
    *NhipStops,
    start = Offset(0f, size.height),
    end = Offset(size.width, 0f),
)

// Màu tại vị trí t (0..1), dùng tô quỹ đạo theo pace (mục 2.4)
fun nhipColorAt(t: Float): Color {
    val x = t.coerceIn(0f, 1f)
    val i = NhipStops.indexOfLast { it.first <= x }.coerceAtMost(NhipStops.lastIndex - 1)
    val (p0, c0) = NhipStops[i]
    val (p1, c1) = NhipStops[i + 1]
    return lerp(c0, c1, (x - p0) / (p1 - p0))
}
```

### 13.4 Vẽ vòng chia đoạn

```kotlin
// Trong DrawScope của RunRing, khi runCount > 1
val segments = runCount.coerceAtMost(4)
val gapDeg = 6f
val sweep = 360f / segments - gapDeg
rotate(-90f) {
    repeat(segments) { i ->
        drawArc(
            brush = nhipSweep(center),
            startAngle = i * (sweep + gapDeg) + gapDeg / 2,
            sweepAngle = sweep,
            useCenter = false,
            style = Stroke(width = ringWidthPx, cap = StrokeCap.Round),
        )
    }
}
```

Vòng nét đứt (`Planned`): `Stroke(width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp, 3.dp)))`.

### 13.5 Ghi chú khác

- Theme: `dynamicColor = false`.
- Số liệu: `style = MaterialTheme.typography.displayLarge.merge(TextStyle(fontFeatureSettings = "tnum"))`.
- Ảnh thu nhỏ quỹ đạo trong lịch tuần: tính sẵn đường đã giảm điểm trong ViewModel (không đọc hàng nghìn điểm cho mỗi ô lịch lúc vẽ). Có thể thêm truy vấn DAO chỉ lấy mỗi điểm thứ N.

---

## 14. Nên và không nên

| Nên | Không nên |
|---|---|
| Để màu đen trắng làm chủ, gradient làm điểm nhấn | Tô gradient lên nền, thẻ, thanh trên |
| Gradient chỉ cho "chạy": đã chạy, đang chạy, bấm để chạy | Tô gradient cho kế hoạch chưa chạy |
| Để vòng chạy và quỹ đạo bên trong làm điểm đặc biệt của thẻ kết quả | Làm thẻ kết quả nổi bằng nền màu, viền dày, đổ bóng |
| Giữ vòng chạy luôn có màu, kể cả sau khi đã xem | Làm xám vòng như Instagram |
| Số to, đậm, `tnum` | Số nhỏ, mảnh, nhảy chữ khi cập nhật |
| Viết chữ thật, ngắn, tiếng Việt tự nhiên ("Bỏ lỡ", "Chạy muộn", "Xem kết quả") | Viết như thông báo hệ thống ("Trạng thái: MISSED") |
| Nút bị tắt luôn kèm giải thích | Ẩn nút mà không nói vì sao |
| Cảnh báo dùng màu `warning` + biểu tượng + chữ | Cảnh báo bằng màu cam của gradient |
| Thêm `@Preview` cho mọi trạng thái | Chỉ xem trước trạng thái "đẹp" |

---

## 15. Quyết định đã chốt

| # | Câu hỏi | Quyết định |
|---|---|---|
| D1 | SPEC S1 ghi "ngày có kế hoạch hoặc buổi chạy có **dấu chấm**", DESIGN dùng **vòng** | Dùng vòng. Đã sửa dòng S1 trong SPEC: "ngày có kế hoạch hoặc buổi chạy có vòng chỉ trạng thái của ngày (xem DESIGN 3.2)" |
| D2 | S6 có giữ màn hình luôn sáng không? | **Không.** Màn hình tắt theo hệ thống, người chạy bật lên khi muốn xem. Số liệu hiện ngay khi bật lại, thông báo của service cho xem nhanh (mục 9, S6) |
| D3 | S6 có luôn dùng nền tối không? | **Không.** Theo chế độ sáng/tối của máy, giống mọi màn hình khác (SPEC 10.5) |
| D4 | Vòng chia đoạn: các đoạn có dài bằng nhau không? | **Có**, bằng nhau bất kể buổi dài hay ngắn, giống Instagram |
| D5 | Ngày có cả buổi đã chạy và kế hoạch chưa chạy: có thêm dấu hiệu "còn kế hoạch" trên vòng không? | **Không.** Vòng chỉ hiện trạng thái ưu tiên cao nhất (mục 3.2), kế hoạch còn lại xem trong danh sách ngày |
| D6 | Tên trên thanh trên | **"RunMate"**, `titleLarge`, đậm 800 |