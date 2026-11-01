package com.nhacongdong.runmate.ui.run

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.data.model.SessionStatus
import com.nhacongdong.runmate.ui.InterruptedRun
import com.nhacongdong.runmate.ui.components.DialogCard
import com.nhacongdong.runmate.ui.components.PreviewBox
import com.nhacongdong.runmate.ui.components.ThemePreviews
import com.nhacongdong.runmate.ui.format.RunFormat
import java.time.ZoneId

// Hộp thoại khi mở app và có buổi chạy dở mà service đã mất (SPEC 10.1, DESIGN 9 S1).
// Quá 1 giờ (BR-60) thì chỉ còn Kết thúc. Phải chọn một trong hai, không tắt bằng nút Back
@Composable
fun RecoverRunDialog(
    run: InterruptedRun,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        RecoverRunCard(run, onResume, onFinish, zone)
    }
}

// Nội dung hộp thoại, tách riêng để xem trước được
@Composable
fun RecoverRunCard(
    run: InterruptedRun,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    DialogCard(
        title = stringResource(R.string.recover_title),
        text = stringResource(
            R.string.recover_message,
            RunFormat.formatTime(run.startedAt, zone),
            RunFormat.formatDistanceKm(run.distanceM),
            RunFormat.formatDuration(run.durationMs / 1_000),
        ),
        // Theo DESIGN 9 S1: "Kết thúc · **Tiếp tục**", nút đậm ở bên phải. Quá 1 giờ thì chỉ còn Kết thúc
        confirm = {
            if (run.canResume) {
                TextButton(onClick = onResume) { Text(stringResource(R.string.run_resume)) }
            } else {
                TextButton(onClick = onFinish) { Text(stringResource(R.string.run_finish)) }
            }
        },
        dismiss = {
            if (run.canResume) {
                TextButton(onClick = onFinish) { Text(stringResource(R.string.run_finish)) }
            }
        },
    )
}

private fun previewRun(canResume: Boolean) = InterruptedRun(
    sessionId = 1L,
    status = SessionStatus.RECORDING,
    startedAt = 1_790_000_000_000L,
    distanceM = 3_200.0,
    durationMs = 1_120_000L,
    canResume = canResume,
    endedAt = 1_790_001_000_000L,
)

@ThemePreviews
@Composable
private fun RecoverRunDialogPreview() = PreviewBox {
    RecoverRunCard(run = previewRun(canResume = true), onResume = {}, onFinish = {})
}

@ThemePreviews
@Composable
private fun RecoverRunDialogExpiredPreview() = PreviewBox {
    RecoverRunCard(run = previewRun(canResume = false), onResume = {}, onFinish = {})
}
