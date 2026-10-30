package com.nhacongdong.runmate.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.domain.PlanRules
import com.nhacongdong.runmate.ui.format.RunFormat
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import kotlin.math.abs

// Giá trị mục tiêu giữ theo đơn vị của domain: mét cho quãng đường, giây cho thời gian
const val DEFAULT_DISTANCE_M = 5_000
const val DEFAULT_DURATION_S = 1_800
private val DISTANCE_CHIPS_M = listOf(3_000, 5_000, 10_000, 21_100)
private val DURATION_CHIPS_S = listOf(1_200, 1_800, 2_700, 3_600)

// Bánh xe (DESIGN 8.13): 3 mốc hiện cùng lúc, mốc giữa là giá trị đang chọn
private val ITEM_HEIGHT = 48.dp
private val WHEEL_HEIGHT = ITEM_HEIGHT * 3
private val CENTER_PADDING = (WHEEL_HEIGHT - ITEM_HEIGHT) / 2

// Số lần lặp danh sách để cuộn vòng. Vị trí luôn nằm giữa dải này, nên không bao giờ chạm đầu hoặc cuối
private const val REPEATS = 100

// Chọn mục tiêu trên S2 (DESIGN 8.13). Giá trị ngoài giới hạn được tự chỉnh bằng PlanRules.clampGoal
@Composable
fun GoalPicker(
    goalType: GoalType,
    goalValue: Int,
    onGoalTypeChange: (GoalType) -> Unit,
    onGoalValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val isDistance = goalType == GoalType.DISTANCE
    val chips = if (isDistance) DISTANCE_CHIPS_M else DURATION_CHIPS_S

    // Đổi giá trị từ một cột: ghép lại với cột kia rồi tự chỉnh về hợp lệ
    fun setGoal(value: Int) = onGoalValueChange(PlanRules.clampGoal(goalType, value))

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.space4)) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space2)) {
            Text(
                stringResource(R.string.goal_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                GoalType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = type == goalType,
                        onClick = { onGoalTypeChange(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, GoalType.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = colors.primary,
                            activeContentColor = colors.onPrimary,
                        ),
                        label = {
                            Text(stringResource(if (type == GoalType.DISTANCE) R.string.goal_distance else R.string.goal_duration))
                        },
                    )
                }
            }
        }

        if (isDistance) {
            val km = goalValue / 1_000
            val tenth = goalValue % 1_000 / 100
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WheelColumn(
                    count = 101,
                    selected = km,
                    label = { "$it" },
                    onSelected = { setGoal(it * 1_000 + tenth * 100) },
                    modifier = Modifier.weight(1f),
                )
                Text(",", style = numberStyle(), color = colors.onSurface)
                WheelColumn(
                    count = 10,
                    selected = tenth,
                    label = { "$it" },
                    onSelected = { setGoal(km * 1_000 + it * 100) },
                    modifier = Modifier.weight(1f),
                )
                UnitText(R.string.unit_km)
            }
        } else {
            val hours = goalValue / 3_600
            val minutes = goalValue % 3_600 / 60
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WheelColumn(
                    count = 13,
                    selected = hours,
                    label = { "%02d".format(it) },
                    onSelected = { setGoal(it * 3_600 + minutes * 60) },
                    modifier = Modifier.weight(1f),
                )
                UnitText(R.string.unit_hours)
                WheelColumn(
                    count = 60,
                    selected = minutes,
                    label = { "%02d".format(it) },
                    onSelected = { setGoal(hours * 3_600 + it * 60) },
                    modifier = Modifier.weight(1f),
                )
                UnitText(R.string.unit_minutes)
            }
        }

        Text(
            stringResource(if (isDistance) R.string.goal_range_distance else R.string.goal_range_duration),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
            chips.forEach { value ->
                FilterChip(
                    selected = value == goalValue,
                    onClick = { onGoalValueChange(value) },
                    label = { Text(RunFormat.formatGoal(goalType, value)) },
                )
            }
        }
    }
}

// Một cột cuộn vòng kiểu bánh xe. Danh sách được lặp REPEATS lần, mốc thật là vị trí % count.
// Thả tay thì cột dừng đúng mốc, và báo giá trị mốc giữa qua onSelected
@Composable
private fun WheelColumn(
    count: Int,
    selected: Int,
    label: (Int) -> String,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState()
    val middleBase = count * REPEATS / 2
    val centerIndex by remember { derivedStateOf { centerItemIndex(state) ?: (middleBase + selected) } }

    // Mở màn hình thì đưa mốc đang chọn về giữa
    LaunchedEffect(Unit) {
        state.scrollToItem(middleBase + selected)
    }
    // Giá trị đổi từ nơi khác (chip, cột kia tự chỉnh) thì cuộn tới mốc gần nhất có giá trị đó
    // Đặt lại vị trí cột khi giá trị đổi từ nơi khác. Tăng resync để chạy lại khi giá trị bị kẹp mà không đổi
    var resync by remember { mutableIntStateOf(0) }
    LaunchedEffect(selected, resync) {
        if (!state.isScrollInProgress && centerIndex % count != selected) {
            val target = centerIndex + shortestSteps(centerIndex % count, selected, count)
            state.animateScrollToItem(target)
        }
    }
    // Người dùng vừa thả tay ở mốc khác thì báo mốc đó, rồi cho cột quay về giá trị đã lưu
    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress && centerIndex % count != selected) {
            onSelected(centerIndex % count)
            resync++
        }
    }
    // Đang gần đầu hoặc cuối dải thì nhảy về giữa dải, cùng giá trị, nên người dùng không thấy gì
    LaunchedEffect(centerIndex) {
        val lowest = count * REPEATS / 10
        val highest = count * REPEATS * 9 / 10
        if (!state.isScrollInProgress && (centerIndex < lowest || centerIndex > highest)) {
            state.scrollToItem(middleBase + centerIndex % count)
        }
    }

    LazyColumn(
        state = state,
        modifier = modifier.height(WHEEL_HEIGHT),
        contentPadding = PaddingValues(vertical = CENTER_PADDING),
        flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
    ) {
        items(count * REPEATS) { index ->
            val isCenter = index == centerIndex
            Box(Modifier.fillMaxWidth().height(ITEM_HEIGHT), contentAlignment = Alignment.Center) {
                Text(
                    label(index % count),
                    style = if (isCenter) numberStyle() else MaterialTheme.typography.titleMedium,
                    color = if (isCenter) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

// Số mốc cần cuộn từ `from` tới `to` theo đường ngắn nhất trên vòng tròn có `count` mốc
private fun shortestSteps(from: Int, to: Int, count: Int): Int {
    val forward = Math.floorMod(to - from, count)
    return if (forward > count / 2) forward - count else forward
}

// Mốc gần giữa cột nhất
private fun centerItemIndex(state: LazyListState): Int? {
    val info = state.layoutInfo
    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index
}

// Số luôn bật tnum để chữ số không nhảy khi cuộn (DESIGN 5.1)
@Composable
private fun numberStyle(): TextStyle =
    MaterialTheme.typography.headlineMedium.merge(TextStyle(fontFeatureSettings = "tnum"))

@Composable
private fun UnitText(unit: Int) {
    Text(
        stringResource(unit),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Dimens.space1),
    )
}

@Preview(name = "Quãng đường · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Quãng đường · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GoalPickerDistancePreview() = RunMateTheme {
    GoalPicker(
        goalType = GoalType.DISTANCE, goalValue = 5_000,
        onGoalTypeChange = {}, onGoalValueChange = {},
        modifier = Modifier.padding(Dimens.space4),
    )
}

@Preview(name = "Thời gian · Sáng", widthDp = 360, showBackground = true)
@Composable
private fun GoalPickerDurationPreview() = RunMateTheme {
    GoalPicker(
        goalType = GoalType.DURATION, goalValue = 1_800,
        onGoalTypeChange = {}, onGoalValueChange = {},
        modifier = Modifier.padding(Dimens.space4),
    )
}
