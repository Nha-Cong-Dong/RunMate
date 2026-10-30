package com.nhacongdong.runmate.data.place

import com.nhacongdong.runmate.domain.model.Place

// Một cách tìm địa điểm theo tên. Geocoder của máy và Open-Meteo đều implement
interface PlaceSearch {
    suspend fun search(query: String): List<Place>
}
