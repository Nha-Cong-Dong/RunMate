package com.nhacongdong.runmate.data.place

import com.nhacongdong.runmate.domain.model.Place
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

// Test luồng tìm địa điểm: Geocoder trước, Open-Meteo khi Geocoder không dùng được (SPEC 8.2)
class PlaceRepositoryTest {
    private val hoGuom = Place("Hồ Hoàn Kiếm", "Hoàn Kiếm, Hà Nội", 21.028, 105.852)
    private val hanoi = Place("Hanoi", "Hanoi, Vietnam", 21.03, 105.85)

    // Giả lập một nguồn tìm kiếm, ghi lại chữ đã được gửi tới
    private class FakeSearch(private val answer: () -> List<Place>) : PlaceSearch {
        val queries = mutableListOf<String>()

        override suspend fun search(query: String): List<Place> {
            queries += query
            return answer()
        }
    }

    @Test
    fun geocoderFound_usesGeocoderOnly() = runBlocking {
        val geocoder = FakeSearch { listOf(hoGuom) }
        val fallback = FakeSearch { listOf(hanoi) }

        val result = PlaceRepository(geocoder, fallback).search("Hồ Gươm")

        assertEquals(listOf(hoGuom), result)
        assertEquals(listOf("Hồ Gươm"), geocoder.queries)
        assertEquals(emptyList<String>(), fallback.queries)
    }

    @Test
    fun geocoderEmpty_returnsEmptyWithoutFallback() = runBlocking {
        val geocoder = FakeSearch { emptyList() }
        val fallback = FakeSearch { listOf(hanoi) }

        val result = PlaceRepository(geocoder, fallback).search("xyz")

        assertEquals(emptyList<Place>(), result)
        assertEquals(emptyList<String>(), fallback.queries)
    }

    @Test
    fun geocoderMissing_usesFallbackWithAsciiQuery() = runBlocking {
        val fallback = FakeSearch { listOf(hanoi) }

        val result = PlaceRepository(null, fallback).search("Hà Nội")

        assertEquals(listOf(hanoi), result)
        assertEquals(listOf("Ha Noi"), fallback.queries)
    }

    @Test
    fun geocoderError_usesFallbackWithAsciiQuery() = runBlocking {
        val geocoder = FakeSearch { throw IOException("Geocoder lỗi") }
        val fallback = FakeSearch { listOf(hanoi) }

        val result = PlaceRepository(geocoder, fallback).search("Hà Nội")

        assertEquals(listOf(hanoi), result)
        assertEquals(listOf("Ha Noi"), fallback.queries)
    }

    @Test
    fun fallbackError_isThrownToCaller() {
        val fallback = FakeSearch { throw IOException("mất mạng") }

        assertThrows(IOException::class.java) {
            runBlocking { PlaceRepository(null, fallback).search("Hà Nội") }
        }
    }
}
