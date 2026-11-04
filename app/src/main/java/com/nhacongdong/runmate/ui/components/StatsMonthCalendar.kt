package com.nhacongdong.runmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.domain.WeekRules
import com.nhacongdong.runmate.ui.format.RunFormat
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.NhipBlue
import java.time.LocalDate
import java.time.YearMonth

// Lịch tháng ở S8 (DESIGN mục S8, kỳ Tháng). Mỗi ngày có buổi chạy đã lưu có một chấm tròn 6 dp dưới số ngày.
// Lưới 7 cột T2 đến CN, ô ngoài tháng để trống
@Composable
fun StatsMonthCalendar(
    yearMonth: YearMonth,
    runningDays: Set<LocalDate>,
    modifier: Modifier = Modifier,
) {
    // Ngày 01/01/2024 là Thứ Hai, dùng để lấy tên thứ theo đúng thứ tự T2 đến CN
    val monday = LocalDate.of(2024, 1, 1)
    val weekdayLabels = (0L until 7L).map { RunFormat.formatWeekday(monday.plusDays(it)) }

    // Ô ngoài tháng vẫn có ngày (của tháng trước hoặc sau) nhưng mờ, không có chấm
    val first = yearMonth.atDay(1)
    val gridStart = WeekRules.weekStartOf(first)
    val last = yearMonth.atEndOfMonth()
    val gridEnd = last.plusDays((7 - ((last.dayOfWeek.value - 1) % 7 + 1)).toLong())
    val cells: List<LocalDate> = generateSequence(gridStart) { it.plusDays(1) }
        .takeWhile { !it.isAfter(gridEnd) }
        .toList()
    val rows = cells.chunked(7)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.space1)) {
        Row(Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { date ->
                    val inMonth = YearMonth.from(date) == yearMonth
                    Column(
                        Modifier.weight(1f).height(44.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (inMonth) 1f else 0.4f),
                        )
                        Spacer(Modifier.height(Dimens.space1))
                        if (inMonth && date in runningDays) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NhipBlue),
                            )
                        }
                    }
                }
            }
        }
    }
}

// Tháng 10/2026 có vài ngày chạy: chấm tròn hiện ở các ngày này. Ngày ngoài tháng mờ, không có chấm
@ThemePreviews
@Composable
private fun StatsMonthCalendarPreview() = PreviewBox {
    StatsMonthCalendar(
        yearMonth = YearMonth.of(2026, 10),
        runningDays = setOf(
            LocalDate.of(2026, 10, 2),
            LocalDate.of(2026, 10, 5),
            LocalDate.of(2026, 10, 9),
            LocalDate.of(2026, 10, 14),
            LocalDate.of(2026, 10, 18),
            LocalDate.of(2026, 10, 23),
            LocalDate.of(2026, 10, 29),
            LocalDate.of(2026, 10, 31),
        ),
    )
}
