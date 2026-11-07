<div align="center">

# 🏃 RunMate

**Lên kế hoạch chạy theo thời tiết, ghi lại từng bước chạy.**

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)
![License](https://img.shields.io/github/license/Nha-Cong-Dong/RunMate)
![Last commit](https://img.shields.io/github/last-commit/Nha-Cong-Dong/RunMate)

</div>

---

## Giới thiệu

RunMate là ứng dụng Android giúp bạn chọn thời gian và địa điểm để chạy bộ. App cho xem dự báo thời tiết đúng lúc đó, cảnh báo khi thời tiết xấu và gợi ý vật dụng nên mang theo. Trong lúc chạy, RunMate ghi lại GPS để tính quãng đường, thời gian và pace. Sau buổi chạy, bạn xem lại được quỹ đạo đã chạy, và mọi buổi chạy đều được lưu vào lịch sử.

Mục tiêu chính: học qua vibe code. Hiểu được thứ AI viết ra quan trọng hơn tốc độ.

<!-- TODO: thêm ảnh chụp màn hình / GIF demo
<p align="center">
  <img src="docs/screenshots/plan.png" width="250" />
  <img src="docs/screenshots/run.png" width="250" />
  <img src="docs/screenshots/history.png" width="250" />
</p>
-->

## Tính năng

### MVP
- [x] **Lên kế hoạch chạy:** chọn ngày, giờ, vị trí và mục tiêu (số km hoặc thời gian chạy)
- [x] **Đánh giá thời tiết** tại thời điểm và vị trí đã chọn
  - Dự báo: nhiệt độ, khả năng mưa, gió, chỉ số UV
  - Cảnh báo khi thời tiết xấu (mưa, quá nóng, gió mạnh, UV cao)
  - Gợi ý vật dụng nên mang (áo mưa, mũ, kem chống nắng, nước…)
- [x] **Ghi một buổi chạy:** theo kế hoạch hoặc chạy tự do. GPS, quãng đường, thời gian, pace, tiến độ so với mục tiêu
- [x] **Xem lại quỹ đạo** sau khi chạy (vẽ đường chạy, chưa có bản đồ nền)
- [x] **Lịch sử và thống kê:** xem các buổi đã chạy trên lịch tuần cùng kế hoạch, thống kê theo tuần hoặc tháng

### Sau MVP
- [ ] Thông báo trước giờ chạy 30 phút, kèm dự báo lại thời tiết
- [ ] Hiển thị quỹ đạo trên bản đồ nền
- [ ] Vẽ đường theo thời gian thực trong lúc chạy

## Công nghệ

| Thành phần     | Công nghệ                                                                                                                                                            | Lý do chọn                                           |
| -------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------- |
| Ngôn ngữ       | Kotlin                                                                                                                                                               | Ngôn ngữ chính thức cho Android                      |
| Build          | Gradle (Kotlin DSL)                                                                                                                                                  | Mặc định của Android Studio                          |
| UI             | [Jetpack Compose](https://developer.android.com/compose)                                                                                                             | Google khuyến nghị cho app mới, viết UI ít code hơn  |
| Điều hướng     | [Navigation Compose](https://developer.android.com/guide/navigation)                                                                                                 | Thư viện chính thức, dùng cùng Compose               |
| Kiến trúc      | [MVVM](https://developer.android.com/topic/architecture) (ViewModel + StateFlow)                                                                                     | Đơn giản, đúng hướng dẫn chính thức                  |
| Bất đồng bộ    | [Kotlin Coroutines + Flow](https://kotlinlang.org/docs/coroutines-overview.html)                                                                                     | Room, Retrofit và ViewModel đều hỗ trợ sẵn           |
| Lưu trữ        | [Room 3](https://developer.android.com/training/data-storage/room)                                                                                                   | Lưu offline kế hoạch, buổi chạy và điểm GPS          |
| Thời tiết      | [Open-Meteo API](https://open-meteo.com/)                                                                                                                            | Không cần API key, có dự báo theo giờ đến 16 ngày    |
| Tìm địa điểm   | [Android Geocoder](https://developer.android.com/reference/android/location/Geocoder), dự phòng [Open-Meteo Geocoding](https://open-meteo.com/en/docs/geocoding-api) | Tìm được cả địa điểm cụ thể, không cần API key       |
| Gọi mạng       | [Retrofit](https://square.github.io/retrofit/) + kotlinx.serialization                                                                                               | Phổ biến, có nhiều tài liệu                          |
| Vị trí         | [Fused Location Provider](https://developer.android.com/develop/sensors-and-location/location)                                                                       | Chính xác và tiết kiệm pin                           |
| Ghi GPS nền    | [Foreground Service loại `location`](https://developer.android.com/about/versions/14/changes/fgs-types-required)                                                     | Bắt buộc từ Android 14                               |
| Vẽ quỹ đạo     | Compose Canvas                                                                                                                                                       | Không cần API key, không phụ thuộc dịch vụ bên ngoài |
| Tiêm phụ thuộc | [Tự tạo thủ công](https://developer.android.com/training/dependency-injection/manual) (`AppContainer`)                                                               | App nhỏ, dễ thấy mọi thứ được tạo ở đâu              |

> **Lưu ý:** Open-Meteo miễn phí cho mục đích phi thương mại. Nếu RunMate kiếm tiền, cần chuyển sang gói trả phí.

## Bắt đầu

### Yêu cầu

- Android Studio Rabbit 1 (2026.2.1) trở lên
- Không cần cài JDK riêng: Gradle tự tải JDK 25 ở lần build đầu tiên (cần mạng). Code biên dịch ở mức Java 17
- Android SDK: `minSdk` 26 (Android 8.0) / `targetSdk` 36 (Android 16)

### Cài đặt

```bash
git clone https://github.com/Nha-Cong-Dong/RunMate.git
cd RunMate
```

1. Mở thư mục bằng Android Studio và chờ Gradle sync (lần đầu sẽ tải JDK 25 nếu máy chưa có).
2. Chạy app trên máy ảo hoặc thiết bị thật (nên dùng máy thật để thử GPS).

Build APK từ dòng lệnh:

```bash
./gradlew assembleDebug
```

### Cài đặt nhanh file APK demo

Nếu không muốn cài đặt Android Studio hay tự biên dịch mã nguồn, bạn có thể tải trực tiếp file APK đã dựng sẵn:
1. Vào mục [Releases](https://github.com/Nha-Cong-Dong/RunMate/releases) trên GitHub của dự án.
2. Tải file `app-debug.apk` về điện thoại Android (hỗ trợ Android 8.0 trở lên).
3. Mở file APK trên điện thoại và chọn **Cài đặt** (cho phép cài đặt từ nguồn không xác định nếu thiết bị yêu cầu).

> MVP không cần API key nào: Open-Meteo không yêu cầu key, và quỹ đạo được vẽ không dùng bản đồ.

## Cấu trúc thư mục

```
RunMate/
├── app/
│   └── src/main/
│       ├── java/com/nhacongdong/runmate/
│       │   ├── ui/          # Màn hình Compose, ViewModel, điều hướng, theme
│       │   ├── domain/      # Quy tắc nghiệp vụ (BR-xx), use case
│       │   ├── data/        # Repository, Room 3, Retrofit
│       │   └── service/     # Ghi GPS chạy nền (Foreground Service)
│       ├── res/             # Tài nguyên giao diện (strings, drawable, font)
│       └── AndroidManifest.xml
├── docs/
│   └── SPEC.md              # Đặc tả sản phẩm và quy tắc nghiệp vụ chi tiết
├── gradle/
│   └── libs.versions.toml   # Quản lý phiên bản thư viện
├── build.gradle.kts
└── settings.gradle.kts
```

Mã nguồn được tổ chức theo kiến trúc phân tầng sạch (Clean Architecture) với luồng phụ thuộc một chiều (`ui` → `domain` → `data`):

- **`ui/` (Giao diện & Trình bày):** Toàn bộ các màn hình Jetpack Compose theo mô hình MVVM (Lịch S1, Lên kế hoạch S2, Chọn địa điểm S3, Chi tiết kế hoạch S4, Chuẩn bị chạy S5, Màn hình đang chạy S6, Chi tiết kết quả S7, Thống kê S8), ViewModel, quản lý luồng trạng thái (`StateFlow`), điều hướng (Navigation Compose) và hệ thống thiết kế (màu sắc, kiểu chữ, khoảng cách).
- **`domain/` (Quy tắc nghiệp vụ):** Các hàm xử lý nghiệp vụ thuần Kotlin, không phụ thuộc Android framework:
  - Đánh giá thời tiết, phát cảnh báo thời tiết xấu và gợi ý vật dụng mang theo (`WeatherRules`).
  - Bộ lọc toạ độ GPS (loại điểm sai số cao, nhảy vọt vô lý), ngắt đoạn khi tạm dừng, tính quãng đường và pace (`TrackRules`).
  - Kiểm tra tính hợp lệ và chuyển trạng thái kế hoạch/buổi chạy (`PlanRules`, `RunRules`, `WeekRules`).
  - Các Use Case điều phối vòng đời buổi chạy (`StartPlanRun`, `StartFreeRun`, `PauseRun`, `ResumeRun`, `FinishRun`).
- **`data/` (Tầng dữ liệu & Kho lưu trữ):**
  - **Lưu trữ offline (Room 3):** Quản lý bảng kế hoạch (`RunPlan`), buổi chạy (`RunSession`) và các điểm GPS (`TrackPoint`).
  - **Gọi mạng (Retrofit):** Tích hợp Open-Meteo API để lấy dự báo thời tiết theo giờ và toạ độ.
  - **Tìm kiếm địa điểm:** Sử dụng Android Geocoder và cơ chế dự phòng qua Open-Meteo Geocoding API.
  - **Repository:** Cung cấp dữ liệu sạch cho ViewModel (`PlanRepository`, `SessionRepository`, `WeatherRepository`, `PlaceRepository`).
- **`service/` (Dịch vụ nền):** `RunTrackingService` là một Android Foreground Service loại `location`, phụ trách duy trì việc nhận GPS liên tục qua Fused Location Provider, lọc điểm và lưu vào cơ sở dữ liệu ngay cả khi người dùng tắt màn hình hoặc chuyển app.

## Lộ trình

- [x] **MVP:** lên kế hoạch kèm đánh giá thời tiết, ghi buổi chạy, xem quỹ đạo, lịch sử trên lịch tuần và thống kê
- [ ] **v1.1:** thông báo trước giờ chạy 30 phút và dự báo lại thời tiết
- [ ] **v1.2:** bản đồ nền và vẽ đường theo thời gian thực

## Giấy phép

Phát hành theo giấy phép [MIT](LICENSE).
