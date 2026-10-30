package com.nhacongdong.runmate.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.ui.theme.Dimens

// Tab đang chọn ở thanh dưới
enum class BottomTab { Calendar, Stats }

// Thanh dưới (DESIGN 8.2): Lịch · RunButton · Thống kê. Chỉ hiện ở S1 và S8
@Composable
fun BottomBar(
    selected: BottomTab,
    onSelect: (BottomTab) -> Unit,
    onRun: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
            // Kẻ trên 1 dp màu outline của DESIGN (outlineVariant trong Material 3, DESIGN 4.4)
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().height(64.dp).selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TabItem(
                    label = R.string.tab_calendar,
                    icon = R.drawable.ic_calendar_month,
                    iconSelected = R.drawable.ic_calendar_month_filled,
                    isSelected = selected == BottomTab.Calendar,
                    onClick = { onSelect(BottomTab.Calendar) },
                    modifier = Modifier.weight(1f),
                )
                RunButton(onClick = onRun)
                TabItem(
                    label = R.string.tab_stats,
                    icon = R.drawable.ic_insights,
                    iconSelected = R.drawable.ic_insights_filled,
                    isSelected = selected == BottomTab.Stats,
                    onClick = { onSelect(BottomTab.Stats) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// Biểu tượng (Filled khi đang chọn, DESIGN 7) và chữ labelMedium. Cả ô là một vùng bấm
@Composable
private fun TabItem(
    @StringRes label: Int,
    @DrawableRes icon: Int,
    @DrawableRes iconSelected: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val color = if (isSelected) colors.onSurface else colors.onSurfaceVariant
    Column(
        modifier
            .fillMaxHeight()
            .selectable(selected = isSelected, onClick = onClick, role = Role.Tab),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.space1, Alignment.CenterVertically),
    ) {
        Icon(painterResource(if (isSelected) iconSelected else icon), contentDescription = null, tint = color)
        Text(
            stringResource(label), style = MaterialTheme.typography.labelMedium, color = color,
            maxLines = 1, softWrap = false,
        )
    }
}

@ThemePreviews
@Composable
private fun BottomBarPreview() = PreviewBox { BottomBar(BottomTab.Calendar, {}, {}) }
