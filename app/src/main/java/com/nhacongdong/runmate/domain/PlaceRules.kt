package com.nhacongdong.runmate.domain

import java.text.Normalizer

// Quy tắc tìm địa điểm (SPEC 8.2). Open-Meteo Geocoding không tìm được tên có dấu nên phải bỏ dấu trước
object PlaceRules {
    private val COMBINING_MARKS = Regex("\\p{Mn}+")
    private val SPACES = Regex("\\s+")

    fun toAsciiQuery(input: String): String =
        Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .replace(SPACES, " ")
            .trim()
}
