package com.nhacongdong.runmate.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.ui.format.RunFormat
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.NhipBlue
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import com.nhacongdong.runmate.ui.theme.tabular

// Dữ liệu cho một cột trong biểu đồ thống kê (DESIGN 9 S8)
data class BarChartItem(
    val label: String,       // Dòng 1 chân cột: "T2", "T3"... hoặc "Tuần 1", "Tuần 2"...
    val subLabel: String,    // Dòng 2 chân cột: "26/10" hoặc "01/10 – 04/10"
    val distanceMeters: Double,
)

// Biểu đồ cột ở màn hình S8 (DESIGN 9 S8).
// Vùng cột cao 140 dp.
// Cột có chạy: màu nhipBlue, bo góc trên 4 dp, chiều cao tỷ lệ theo cột lớn nhất.
// Cột 0 km: thanh ngang ringTrack dày 2 dp ở đáy.
// Trên đầu cột: số km cụ thể (hoặc "–" khi 0 km).
// Dưới chân cột: 2 dòng nhãn (label và subLabel).
@Composable
fun StatsBarChart(
    items: List<BarChartItem>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 140.dp,
) {
    val reducedMotion = rememberReducedMotion()
    val maxDistance = items.maxOfOrNull { it.distanceMeters }?.takeIf { it > 0.0 } ?: 1.0

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        items.forEach { item ->
            BarColumn(
                item = item,
                maxDistance = maxDistance,
                chartHeight = chartHeight,
                reducedMotion = reducedMotion,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BarColumn(
    item: BarChartItem,
    maxDistance: Double,
    chartHeight: Dp,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val hasDistance = item.distanceMeters > 0.0
    val targetFraction = if (hasDistance) {
        (item.distanceMeters / maxDistance).toFloat().coerceIn(0.06f, 1f)
    } else {
        0f
    }

    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = if (reducedMotion) tween(0) else tween(durationMillis = 400),
        label = "barHeight",
    )

    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 1. Số km trên đầu cột: định dạng km hoặc "–"
        Text(
            text = if (hasDistance) RunFormat.formatDistanceKm(item.distanceMeters) else "–",
            style = MaterialTheme.typography.labelSmall.tabular(),
            color = if (hasDistance) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )

        Spacer(Modifier.height(Dimens.space1))

        // 2. Vùng thân cột: cao chartHeight (140 dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (hasDistance) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .fillMaxHeight(animatedFraction)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(NhipBlue)
                )
            } else {
                // Ngày không chạy: thanh ngang ringTrack dày 2 dp ở đáy
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(RunMateTheme.colors.ringTrack)
                )
            }
        }

        Spacer(Modifier.height(Dimens.space2))

        // 3. Dưới chân cột: 2 dòng nhãn
        // Dòng 1: Thứ hoặc Tên tuần
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )

        // Dòng 2: Ngày/tháng hoặc khoảng ngày
        Text(
            text = item.subLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Preview(name = "Biểu đồ tuần · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Biểu đồ tuần · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatsBarChartWeekPreview() = RunMateTheme {
    val sampleWeek = listOf(
        BarChartItem("T2", "26/10", 0.0),
        BarChartItem("T3", "27/10", 5_200.0),
        BarChartItem("T4", "28/10", 0.0),
        BarChartItem("T5", "29/10", 10_000.0),
        BarChartItem("T6", "30/10", 3_500.0),
        BarChartItem("T7", "31/10", 0.0),
        BarChartItem("CN", "01/11", 8_000.0),
    )
    StatsBarChart(
        items = sampleWeek,
        modifier = Modifier.padding(16.dp),
    )
}

@Preview(name = "Biểu đồ tháng · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Biểu đồ tháng · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatsBarChartMonthPreview() = RunMateTheme {
    val sampleMonth = listOf(
        BarChartItem("Tuần 1", "01 – 04/10", 12_500.0),
        BarChartItem("Tuần 2", "05 – 11/10", 25_000.0),
        BarChartItem("Tuần 3", "12 – 18/10", 0.0),
        BarChartItem("Tuần 4", "19 – 25/10", 18_200.0),
        BarChartItem("Tuần 5", "26 – 31/10", 15_000.0),
    )
    StatsBarChart(
        items = sampleMonth,
        modifier = Modifier.padding(16.dp),
    )
}
