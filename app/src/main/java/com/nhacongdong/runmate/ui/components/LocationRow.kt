package com.nhacongdong.runmate.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.RunMateTheme

// Ô vị trí trên S2 (DESIGN 8.14). Bấm để mở S3
@Composable
fun LocationRow(
    name: String?,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(colors.surfaceContainer, RoundedCornerShape(Dimens.radiusM))
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.space4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
    ) {
        Icon(
            painterResource(R.drawable.ic_location_on),
            contentDescription = null,
            tint = if (isError) colors.error else colors.onSurface,
        )
        Text(
            name ?: stringResource(R.string.location_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = when {
                name != null -> colors.onSurface
                isError -> colors.error
                else -> colors.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = colors.onSurfaceVariant)
    }
}

@Preview(name = "Chưa chọn · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Chưa chọn · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LocationRowEmptyPreview() = RunMateTheme {
    LocationRow(name = null, isError = false, onClick = {}, modifier = Modifier.padding(Dimens.space4))
}

@Preview(name = "Đã chọn · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Đã chọn · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LocationRowPickedPreview() = RunMateTheme {
    LocationRow(name = "Hồ Hoàn Kiếm", isError = false, onClick = {}, modifier = Modifier.padding(Dimens.space4))
}
