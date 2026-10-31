package com.nhacongdong.runmate.ui.format

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.domain.Gear

// Chữ và biểu tượng của vật dụng (DESIGN 8.12, mục 7). Đặt ở lớp giao diện để domain không phụ thuộc Android
@StringRes
fun Gear.labelRes(): Int = when (this) {
    Gear.RAIN_COAT -> R.string.gear_rain_coat
    Gear.HAT -> R.string.gear_hat
    Gear.SUNSCREEN -> R.string.gear_sunscreen
    Gear.WATER -> R.string.gear_water
    Gear.WINDBREAKER -> R.string.gear_windbreaker
}

@DrawableRes
fun Gear.iconRes(): Int = when (this) {
    Gear.RAIN_COAT -> R.drawable.ic_umbrella
    Gear.HAT -> R.drawable.ic_styler
    Gear.SUNSCREEN -> R.drawable.ic_sanitizer
    Gear.WATER -> R.drawable.ic_water_bottle
    Gear.WINDBREAKER -> R.drawable.ic_checkroom
}
