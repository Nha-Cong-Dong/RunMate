package com.nhacongdong.runmate.data.place

import com.nhacongdong.runmate.data.remote.GeocodingApi
import com.nhacongdong.runmate.data.remote.GeocodingResponse
import com.nhacongdong.runmate.domain.model.Place

// Tìm địa điểm bằng Open-Meteo Geocoding (dự phòng, SPEC 8.2). Chữ truyền vào phải đã bỏ dấu.
class OpenMeteoPlaceSearch(private val api: GeocodingApi) : PlaceSearch {
    override suspend fun search(query: String): List<Place> =
        api.search(name = query, language = LANGUAGE).toPlaces()

    private companion object {
        const val LANGUAGE = "vi"
    }
}

// Dòng phụ là tỉnh và quốc gia, ví dụ "Hoàn Kiếm, Hà Nội" hoặc "Hanoi, Vietnam"
internal fun GeocodingResponse.toPlaces(): List<Place> =
    results.orEmpty().map { result ->
        Place(
            name = result.name,
            subtitle = listOfNotNull(result.admin1, result.country).distinct().joinToString(", "),
            latitude = result.latitude,
            longitude = result.longitude
        )
    }
