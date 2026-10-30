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
- [ ] **Lên kế hoạch chạy:** chọn ngày, giờ, vị trí và mục tiêu (số km hoặc thời gian chạy)
- [ ] **Đánh giá thời tiết** tại thời điểm và vị trí đã chọn
  - Dự báo: nhiệt độ, khả năng mưa, gió, chỉ số UV
  - Cảnh báo khi thời tiết xấu (mưa, quá nóng, gió mạnh, UV cao)
  - Gợi ý vật dụng nên mang (áo mưa, mũ, kem chống nắng, nước…)
- [ ] **Ghi một buổi chạy:** theo kế hoạch hoặc chạy tự do. GPS, quãng đường, thời gian, pace, tiến độ so với mục tiêu
- [ ] **Xem lại quỹ đạo** sau khi chạy (vẽ đường chạy, chưa có bản đồ nền)
- [ ] **Lịch sử và thống kê:** xem các buổi đã chạy trên lịch tuần cùng kế hoạch, thống kê theo tuần hoặc tháng

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
- JDK 17
- Android SDK: `minSdk` 26 (Android 8.0) / `targetSdk` 36 (Android 16)

### Cài đặt

```bash
git clone https://github.com/Nha-Cong-Dong/RunMate.git
cd RunMate
```

1. Mở thư mục bằng Android Studio và chờ Gradle sync.
2. Chạy app trên máy ảo hoặc thiết bị thật (nên dùng máy thật để thử GPS).

```bash
./gradlew assembleDebug
```

> MVP không cần API key nào: Open-Meteo không yêu cầu key, và quỹ đạo được vẽ không dùng bản đồ.

## Cấu trúc thư mục

```
RunMate/
├── app/
│   └── src/main/
│       ├── java/com/nhacongdong/runmate/
│       │   ├── ui/          # Màn hình Compose, ViewModel, điều hướng, theme
│       │   ├── domain/      # Quy tắc nghiệp vụ (BR-xx), use case
│       │   ├── data/        # Repository, Room, Retrofit
│       │   └── service/     # Ghi GPS chạy nền (Foreground Service)
│       ├── res/             # Chữ hiển thị, theme, icon
│       └── AndroidManifest.xml
├── docs/
│   └── SPEC.md     # Đặc tả sản phẩm
├── gradle/
│   └── libs.versions.toml   # Phiên bản thư viện
├── build.gradle.kts
└── settings.gradle.kts
```

Hiện mới có `ui/`. Các thư mục `domain/`, `data/`, `service/` sẽ có khi làm các tính năng. Chi tiết kiến trúc xem `docs/SPEC.md` mục 8.5.

## Lộ trình

- [ ] **MVP:** lên kế hoạch kèm đánh giá thời tiết, ghi buổi chạy, xem quỹ đạo, lịch sử trên lịch tuần và thống kê
- [ ] **v1.1:** thông báo trước giờ chạy 30 phút và dự báo lại thời tiết
- [ ] **v1.2:** bản đồ nền và vẽ đường theo thời gian thực

## Giấy phép

Phát hành theo giấy phép [MIT](LICENSE).
