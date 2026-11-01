package com.nhacongdong.runmate.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import com.nhacongdong.runmate.ui.theme.tabular

// Ô thống kê (DESIGN 8.16): nhãn labelMedium phía trên, số headlineSmall (tnum), đơn vị nhỏ bên phải số
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    centered: Boolean = false,   // căn giữa nhãn và số, dùng cho thời gian chỉ có phút và giây
) {
    Column(
        modifier,
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start,
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall.tabular(),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.alignByBaseline(),
            )
            if (unit != null) {
                Spacer(Modifier.width(Dimens.space1))
                Text(
                    unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

@Preview(name = "Ô thống kê · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Ô thống kê · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatTilePreview() = RunMateTheme {
    Row(
        Modifier.padding(Dimens.space4),
        horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
    ) {
        StatTile("Thời gian chạy", "26:55", Modifier.width(110.dp))
        StatTile("Pace trung bình", "5:08", Modifier.width(110.dp), unit = "/km")
        StatTile("Mục tiêu", "5 km", Modifier.width(110.dp))
    }
}
