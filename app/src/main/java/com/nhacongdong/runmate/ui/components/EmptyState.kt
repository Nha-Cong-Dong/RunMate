package com.nhacongdong.runmate.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.nhacongdong.runmate.domain.DayRingState
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.RunRingSizes

// Trạng thái trống (DESIGN 8.19): vòng nét đứt 72 dp "đang chờ được tô màu", tiêu đề, mô tả, một nút
@Composable
fun EmptyState(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(top = Dimens.space7),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.space2),
    ) {
        RunRing(DayRingState.Planned, RunRingSizes.L)
        Text(
            title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Dimens.space2),
        )
        Text(
            message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onAction, shape = MaterialTheme.shapes.medium, modifier = Modifier.padding(top = Dimens.space2)) {
            Text(actionLabel, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
        }
    }
}

@ThemePreviews
@Composable
private fun EmptyStatePreview() = PreviewBox {
    EmptyState(
        title = "Tuần của bạn đang trống",
        message = "Mỗi buổi chạy sẽ tô màu một vòng trên lịch.",
        actionLabel = "Lên kế hoạch đầu tiên",
        onAction = {},
    )
}
