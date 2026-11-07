package com.nhacongdong.runmate.ui.run

import android.content.Context
import android.content.res.Configuration
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.data.model.SessionStatus
import com.nhacongdong.runmate.domain.RunSummary
import com.nhacongdong.runmate.ui.components.GoalRing
import com.nhacongdong.runmate.ui.components.RunControls
import com.nhacongdong.runmate.ui.components.rememberReducedMotion
import com.nhacongdong.runmate.ui.format.RunFormat
import com.nhacongdong.runmate.ui.location.LocationDialog
import com.nhacongdong.runmate.ui.location.LocationDialogs
import com.nhacongdong.runmate.ui.location.hasLocationPermission
import com.nhacongdong.runmate.ui.location.isLocationOn
import com.nhacongdong.runmate.ui.location.rememberLocationGate
import com.nhacongdong.runmate.ui.power.PowerSaveBanner
import com.nhacongdong.runmate.ui.power.rememberPowerSaveWarningOn
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.NhipDiagonal
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import com.nhacongdong.runmate.ui.theme.tabular
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

// Cách chuyển số ở S6 (DESIGN 11): false = nhảy thẳng, true = cuộn từng chữ số đổi
private const val SCROLL_DIGITS = true

// Thời gian băng "Đạt mục tiêu" hiện lên (DESIGN 9 S6, BR-38)
private const val GOAL_BANNER_MS = 4_000L

// S6 nối với ViewModel: nhận sự kiện (đạt mục tiêu, kết thúc, quá ngắn), nối service và điều hướng
@Composable
fun RunRoute(
    onSaved: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: RunViewModel = viewModel(factory = RunViewModel.factory()),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var locationOk by remember { mutableStateOf(context.hasLocationPermission() && context.isLocationOn()) }
    var finishAsked by remember { mutableStateOf(false) }
    var tooShort by remember { mutableStateOf(false) }
    // Tăng mỗi lần đạt mục tiêu, để banner và vòng phóng chạy lại
    var goalTick by remember { mutableIntStateOf(0) }
    // Bấm Tiếp tục đang chờ quyền vị trí: cấp quyền xong thì tiếp tục luôn
    var resumeWanted by remember { mutableStateOf(false) }

    // Quay lại từ màn cài đặt thì kiểm tra lại quyền vị trí
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { locationOk = context.hasLocationPermission() && context.isLocationOn() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                RunEvent.GoalReached -> {
                    vibrateTwice(context)
                    goalTick++
                }
                is RunEvent.Saved -> onSaved(event.sessionId)
                RunEvent.TooShort -> { tooShort = true }
                RunEvent.NoActiveRun -> onBack()
            }
        }
    }

    // Cấp quyền ở banner thì chỉ cập nhật giao diện. Chỉ khi người dùng bấm Tiếp tục thì mới tiếp tục buổi chạy
    val gate = rememberLocationGate(onReady = {
        locationOk = context.hasLocationPermission() && context.isLocationOn()
        if (resumeWanted) {
            resumeWanted = false
            viewModel.resume()
        } else if (locationOk) {
            viewModel.restartLocation()
        }
    })

    RunScreen(
        state = state,
        locationOk = locationOk,
        goalTick = goalTick,
        onBack = onBack,
        onPause = viewModel::pause,
        onResume = {
            resumeWanted = true
            gate.start()
        },
        onAskFinish = { finishAsked = true },
        onGrantLocation = gate.start,
        locationDialog = gate.dialog,
        onDismissLocationDialog = {
            resumeWanted = false
            gate.dismiss()
        },
        finishAsked = finishAsked,
        onDismissFinish = { finishAsked = false },
        onConfirmFinish = {
            finishAsked = false
            viewModel.finish()
        },
        tooShort = tooShort,
        onUnderstood = {
            tooShort = false
            onBack()
        },
    )
}

// S6 · Đang chạy (DESIGN 9 S6): số chính trong GoalRing, pill trạng thái, GPS, số liệu phụ, nút điều khiển
@Composable
fun RunScreen(
    state: RunUiState,
    locationOk: Boolean,
    goalTick: Int,
    onBack: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAskFinish: () -> Unit,
    onGrantLocation: () -> Unit,
    locationDialog: LocationDialog?,
    onDismissLocationDialog: () -> Unit,
    finishAsked: Boolean,
    onDismissFinish: () -> Unit,
    onConfirmFinish: () -> Unit,
    tooShort: Boolean,
    onUnderstood: () -> Unit,
) {
    val reduced = rememberReducedMotion()
    val powerWarningOn = rememberPowerSaveWarningOn()
    var powerBannerClosed by rememberSaveable { mutableStateOf(false) }
    // Tắt tiết kiệm pin thì cho phép hiện lại băng ở lần bật sau
    LaunchedEffect(powerWarningOn) {
        if (!powerWarningOn) powerBannerClosed = false
    }
    val summary = state.summary ?: RunSummary(0.0, 0L, null, 0.0, false)
    val status = state.status ?: SessionStatus.RECORDING
    val recording = status == SessionStatus.RECORDING
    val isGoalDistance = state.goalType == GoalType.DISTANCE
    val kmUnit = stringResource(R.string.run_distance_unit)

    // Vòng nhích tới giá trị mới trong 400 ms (DESIGN 11). Tắt hoạt ảnh trong hệ thống thì nhảy thẳng
    val ringProgress by animateFloatAsState(
        targetValue = summary.progress.toFloat(),
        animationSpec = if (reduced) snap() else tween(400),
        label = "ringProgress",
    )

    // Vòng phóng 1,0 → 1,06 → 1,0 khi đạt mục tiêu (DESIGN 11)
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(goalTick) {
        if (goalTick > 0 && !reduced) {
            pulse.animateTo(1.06f, tween(250))
            pulse.animateTo(1f, tween(250))
        }
    }

    // Băng "Đạt mục tiêu" hiện 4 giây rồi tự tắt, buổi chạy vẫn tiếp tục (BR-38)
    var showGoalBanner by remember { mutableStateOf(false) }
    LaunchedEffect(goalTick) {
        if (goalTick > 0) {
            showGoalBanner = true
            delay(GOAL_BANNER_MS)
            showGoalBanner = false
        }
    }

    // Chấm "Đang ghi" nhấp nháy, tắt khi người dùng tắt hoạt ảnh
    val blink by rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(1_000), RepeatMode.Reverse),
        label = "blink",
    )

    // Số chính và số phụ theo mục tiêu: mục tiêu quãng đường thì số chính là km, mục tiêu thời gian thì là thời gian
    val primary = if (isGoalDistance) {
        RunFormat.formatDistanceKm(summary.distanceM)
    } else {
        RunFormat.formatDuration(summary.durationMs / 1_000)
    }
    val primaryUnit = if (isGoalDistance) kmUnit else ""
    val secondaryLabel = stringResource(if (isGoalDistance) R.string.run_stat_time else R.string.run_stat_distance)
    val secondaryValue = if (isGoalDistance) {
        RunFormat.formatDuration(summary.durationMs / 1_000)
    } else {
        "${RunFormat.formatDistanceKm(summary.distanceM)} $kmUnit"
    }
    val paceLabel = "${stringResource(R.string.run_stat_pace)} ${stringResource(R.string.run_pace_unit)}"
    val paceValue = summary.paceSecPerKm?.let { RunFormat.formatDuration(it.roundToLong()) } ?: "--"

    val percentLine = if (summary.goalReached) {
        stringResource(R.string.cd_goal_reached)
    } else {
        val remaining = if (isGoalDistance) {
            "${RunFormat.formatDistanceKm((state.goalValue - summary.distanceM).coerceAtLeast(0.0))} $kmUnit"
        } else {
            RunFormat.formatDuration(((state.goalValue * 1_000L - summary.durationMs).coerceAtLeast(0L)) / 1_000)
        }
        stringResource(R.string.run_percent_remaining, RunFormat.formatPercent(summary.progress), remaining)
    }

    Scaffold(
        bottomBar = {
            RunControls(
                status = status,
                onPause = onPause,
                onResume = onResume,
                onFinish = onAskFinish,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = Dimens.space4, vertical = Dimens.space4),
            )
        },
        // Thanh trạng thái đã được Scaffold của RunMateApp chừa ra
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = Dimens.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Quay lại không dừng buổi chạy (SPEC 9.3). Chỉ đổi màn hình
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.cd_back))
                }
                StatusPill(recording = recording, dotAlpha = if (reduced) 1f else blink)
                Spacer(Modifier.weight(1f))
                GpsSignal(ok = locationOk && !state.gpsWeak)
            }
            if (!locationOk) {
                Spacer(Modifier.height(Dimens.space3))
                LocationWarning(onGrant = onGrantLocation)
            }
            // Tiết kiệm pin bật giữa chừng thì cảnh báo (BR-61). Đóng rồi thì ẩn, tắt tiết kiệm pin rồi bật lại thì hiện lại
            if (powerWarningOn && !powerBannerClosed) {
                Spacer(Modifier.height(Dimens.space3))
                PowerSaveBanner(onClose = { powerBannerClosed = true })
            }

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(visible = showGoalBanner) {
                Row(
                    Modifier.padding(bottom = Dimens.space3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space1),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = RunMateTheme.colors.success,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        stringResource(R.string.run_goal_reached_banner, RunFormat.formatGoal(state.goalType, state.goalValue)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            GoalRing(
                progress = ringProgress.toDouble(),
                size = 280.dp,
                strokeWidth = 10.dp,
                modifier = Modifier.graphicsLayer {
                    scaleX = pulse.value
                    scaleY = pulse.value
                },
            ) {
                RingContent(number = primary, unit = primaryUnit, percentLine = percentLine)
            }

            Spacer(Modifier.height(Dimens.space6))
            Row(Modifier.fillMaxWidth()) {
                StatColumn(label = secondaryLabel, value = secondaryValue, modifier = Modifier.weight(1f))
                StatColumn(label = paceLabel, value = paceValue, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.weight(1f))
        }
    }

    if (finishAsked) {
        AlertDialog(
            onDismissRequest = onDismissFinish,
            title = {
                Text(stringResource(R.string.finish_confirm_title), style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Text(
                    stringResource(
                        R.string.distance_and_duration,
                        RunFormat.formatDistanceKm(summary.distanceM),
                        RunFormat.formatDuration(summary.durationMs / 1_000),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmFinish) { Text(stringResource(R.string.run_finish)) }
            },
            dismissButton = {
                TextButton(onClick = onDismissFinish) { Text(stringResource(R.string.action_continue_run)) }
            },
            tonalElevation = 0.dp,
        )
    }

    if (tooShort) {
        AlertDialog(
            onDismissRequest = onUnderstood,
            title = {
                Text(stringResource(R.string.too_short_title), style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Text(stringResource(R.string.too_short_message), style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                TextButton(onClick = onUnderstood) { Text(stringResource(R.string.action_understood)) }
            },
            tonalElevation = 0.dp,
        )
    }

    LocationDialogs(
        dialog = locationDialog,
        permissionMessage = R.string.location_permission_run_message,
        locationOffMessage = R.string.location_off_run_message,
        onDismiss = onDismissLocationDialog,
    )
}

// Số chính, đơn vị và dòng tỷ lệ nằm trong vòng
@Composable
private fun RingContent(number: String, unit: String, percentLine: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BigNumber(number)
        if (unit.isNotEmpty()) {
            Text(unit, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(Dimens.space2))
        Text(percentLine, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// Số chính cỡ displayLarge (hoặc tự co theo độ dài chuỗi để luôn vừa GoalRing 280dp mà không tràn viền).
// Cách cuộn (SCROLL_DIGITS): chỉ chữ số đổi mới trượt từ dưới lên, dấu phẩy và dấu hai chấm đứng yên
@Composable
private fun BigNumber(text: String) {
    val baseStyle = when {
        text.length >= 6 -> MaterialTheme.typography.displayMedium.copy(fontSize = 54.sp, lineHeight = 54.sp)
        text.length == 5 -> MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, lineHeight = 64.sp)
        else -> MaterialTheme.typography.displayLarge
    }
    val style = baseStyle.tabular()
    val color = MaterialTheme.colorScheme.onSurface
    if (!SCROLL_DIGITS) {
        Text(text, style = style, maxLines = 1, softWrap = false, color = color)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        text.forEachIndexed { index, char ->
            if (char.isDigit()) {
                // Mỗi chữ số có AnimatedContent riêng. Chỉ chữ số nào đổi giá trị mới chạy hiệu ứng
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        (slideInVertically(tween(150)) { it } + fadeIn(tween(150))) togetherWith
                            (slideOutVertically(tween(150)) { -it } + fadeOut(tween(150)))
                    },
                    modifier = Modifier.clipToBounds(),
                    label = "digit$index",
                ) { digit ->
                    Text(digit.toString(), style = style, maxLines = 1, softWrap = false, color = color)
                }
            } else {
                Text(char.toString(), style = style, maxLines = 1, softWrap = false, color = color)
            }
        }
    }
}

// Pill góc trên trái: "Đang ghi" với chấm gradient, hoặc "Tạm dừng" với chấm xám
@Composable
private fun StatusPill(recording: Boolean, dotAlpha: Float) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = Dimens.space3, vertical = Dimens.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .alpha(if (recording) dotAlpha else 1f)
                .clip(CircleShape)
                .background(if (recording) NhipDiagonal else androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.onSurfaceVariant)),
        )
        Text(
            stringResource(if (recording) R.string.run_recording else R.string.run_paused),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// Góc trên phải: biểu tượng vệ tinh, chấm xanh khi có quyền vị trí, vàng khi thiếu
@Composable
private fun GpsSignal(ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.space1)) {
        Icon(
            painterResource(R.drawable.ic_satellite_alt),
            contentDescription = stringResource(R.string.cd_gps),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (ok) RunMateTheme.colors.success else RunMateTheme.colors.warning),
        )
    }
}

// Băng khi quyền vị trí tắt: chữ giải thích và nút Cấp quyền (DESIGN 9 S6)
@Composable
private fun LocationWarning(onGrant: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.radiusM))
            .background(RunMateTheme.colors.warningContainer)
            .padding(start = Dimens.space4, end = Dimens.space2, top = Dimens.space2, bottom = Dimens.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(R.drawable.ic_warning),
            contentDescription = null,
            tint = RunMateTheme.colors.warning,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(Dimens.space2))
        Text(
            stringResource(R.string.run_gps_off),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onGrant, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.action_grant_location), style = MaterialTheme.typography.labelLarge)
        }
    }
}

// Một số phụ: nhãn nhỏ phía trên, số lớn bên dưới (headlineMedium)
@Composable
private fun StatColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium.tabular(),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

// Rung hai lần khi đạt mục tiêu (DESIGN 11). Cần quyền VIBRATE trong manifest
private fun vibrateTwice(context: Context) {
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 150, 120), -1))
}

private val PreviewSummary = RunSummary(
    distanceM = 1_880.0,
    durationMs = 600_000L,
    paceSecPerKm = 319.0,
    progress = 0.376,
    goalReached = false,
)

@Composable
private fun RunPreviewScreen(state: RunUiState) = RunMateTheme {
    RunScreen(
        state = state,
        locationOk = true,
        goalTick = 0,
        onBack = {},
        onPause = {},
        onResume = {},
        onAskFinish = {},
        onGrantLocation = {},
        locationDialog = null,
        onDismissLocationDialog = {},
        finishAsked = false,
        onDismissFinish = {},
        onConfirmFinish = {},
        tooShort = false,
        onUnderstood = {},
    )
}

@Preview(name = "Đang ghi · Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Đang ghi · Tối", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RunRecordingPreview() = RunPreviewScreen(
    RunUiState(sessionId = 1L, status = SessionStatus.RECORDING, summary = PreviewSummary, goalType = GoalType.DISTANCE, goalValue = 5_000),
)

@Preview(name = "Tạm dừng · Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Tạm dừng · Tối", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RunPausedPreview() = RunPreviewScreen(
    RunUiState(sessionId = 1L, status = SessionStatus.PAUSED, summary = PreviewSummary, goalType = GoalType.DISTANCE, goalValue = 5_000),
)

private val PreviewSummaryLarge = RunSummary(
    distanceM = 11_240.0,            // 11,2 km (> 10 km)
    durationMs = 3_924_000L,         // 1 giờ 05 phút 24 giây (> 1 giờ)
    paceSecPerKm = 349.0,            // 5:49 /km
    progress = 0.749,                // 75%
    goalReached = false,
)

@Preview(name = "Đang chạy >1h · Mục tiêu km (Sáng)", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Đang chạy >1h · Mục tiêu km (Tối)", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RunOverOneHourDistancePreview() = RunPreviewScreen(
    RunUiState(sessionId = 1L, status = SessionStatus.RECORDING, summary = PreviewSummaryLarge, goalType = GoalType.DISTANCE, goalValue = 15_000),
)

@Preview(name = "Đang chạy >1h · Mục tiêu giờ (Sáng)", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Đang chạy >1h · Mục tiêu giờ (Tối)", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RunOverOneHourDurationPreview() = RunPreviewScreen(
    RunUiState(sessionId = 1L, status = SessionStatus.RECORDING, summary = PreviewSummaryLarge, goalType = GoalType.DURATION, goalValue = 5_400),
)

private val PreviewSummary22m30s = RunSummary(
    distanceM = 4_240.0,             // 4,24 km
    durationMs = 1_350_000L,         // 22 phút 30 giây (1350s)
    paceSecPerKm = 318.0,            // 5:18 /km
    progress = 0.848,                // 85%
    goalReached = false,
)

@Preview(name = "Đang chạy 22m30s · Mục tiêu km (Sáng)", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Đang chạy 22m30s · Mục tiêu km (Tối)", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Run22m30sDistancePreview() = RunPreviewScreen(
    RunUiState(sessionId = 1L, status = SessionStatus.RECORDING, summary = PreviewSummary22m30s, goalType = GoalType.DISTANCE, goalValue = 5_000),
)

@Preview(name = "Đang chạy 22m30s · Mục tiêu phút (Sáng)", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Đang chạy 22m30s · Mục tiêu phút (Tối)", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Run22m30sDurationPreview() = RunPreviewScreen(
    RunUiState(sessionId = 1L, status = SessionStatus.RECORDING, summary = PreviewSummary22m30s, goalType = GoalType.DURATION, goalValue = 1_800),
)
