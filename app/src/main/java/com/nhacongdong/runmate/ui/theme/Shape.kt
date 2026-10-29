package com.nhacongdong.runmate.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

// Bo góc (DESIGN 6.2): radiusM cho nút và ô nhập, radiusL cho thẻ, radiusXL cho hộp thoại
val RunMateShapes = Shapes(
    small = RoundedCornerShape(Dimens.radiusM),
    medium = RoundedCornerShape(Dimens.radiusM),
    large = RoundedCornerShape(Dimens.radiusL),
    extraLarge = RoundedCornerShape(Dimens.radiusXL),
)
