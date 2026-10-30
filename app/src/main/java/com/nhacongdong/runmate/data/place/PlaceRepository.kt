package com.nhacongdong.runmate.data.place

import com.nhacongdong.runmate.domain.PlaceRules
import com.nhacongdong.runmate.domain.model.Place
import java.io.IOException

// Tìm địa điểm theo tên (SPEC 8.2). Ưu tiên Geocoder của máy vì giữ được dấu và tìm được địa điểm cụ thể.
// Máy không có Geocoder hoặc Geocoder báo lỗi thì dùng Open-Meteo với chữ đã bỏ dấu.
// Geocoder trả về rỗng thì không dùng dự phòng.
class PlaceRepository(
    private val geocoder: PlaceSearch?,
    private val fallback: PlaceSearch
) {
    suspend fun search(query: String): List<Place> {
        val primary = geocoder
        if (primary != null) {
            try {
                return primary.search(query)
            } catch (e: IOException) {
                // Geocoder lỗi: rơi xuống dự phòng
            }
        }
        return fallback.search(PlaceRules.toAsciiQuery(query))
    }
}
