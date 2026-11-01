package com.nhacongdong.runmate.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.ui.theme.Dimens

// Khung nội dung hộp thoại (DESIGN 14): bo radiusXL, nền surface, không đổ bóng, tiêu đề titleMedium,
// nội dung bodyMedium, nút xác nhận bên phải. Hộp thoại thật bọc khung này bằng Dialog. Preview gọi thẳng khung này,
// vì cửa sổ Dialog không hiện trong khung xem trước của Android Studio
@Composable
fun DialogCard(
    title: String,
    text: String,
    confirm: @Composable () -> Unit,
    dismiss: @Composable () -> Unit = {},
) {
    Surface(
        modifier = Modifier.widthIn(min = 280.dp, max = 560.dp),
        shape = RoundedCornerShape(Dimens.radiusXL),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.padding(Dimens.space5)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Dimens.space3))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Dimens.space5))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dismiss()
                confirm()
            }
        }
    }
}
