package com.nhacongdong.runmate.domain

import org.junit.Assert.assertEquals
import org.junit.Test

// Test bỏ dấu tiếng Việt khi tìm địa điểm dự phòng (SPEC 8.2)
class PlaceRulesTest {
    @Test
    fun toAsciiQuery_removesToneMarks() {
        assertEquals("Ha Noi", PlaceRules.toAsciiQuery("Hà Nội"))
        assertEquals("Ho Guom", PlaceRules.toAsciiQuery("Hồ Gươm"))
    }

    @Test
    fun toAsciiQuery_mapsVietnameseD() {
        assertEquals("Da Nang", PlaceRules.toAsciiQuery("Đà Nẵng"))
        assertEquals("Dong Da", PlaceRules.toAsciiQuery("Đống Đa"))
    }

    @Test
    fun toAsciiQuery_collapsesSpaces() {
        assertEquals("Ho Guom", PlaceRules.toAsciiQuery("  Hồ   Gươm "))
    }

    @Test
    fun toAsciiQuery_keepsAsciiUnchanged() {
        assertEquals("Hanoi", PlaceRules.toAsciiQuery("Hanoi"))
    }

    @Test
    fun toAsciiQuery_blankBecomesEmpty() {
        assertEquals("", PlaceRules.toAsciiQuery("   "))
    }
}
