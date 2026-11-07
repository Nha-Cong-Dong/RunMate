package com.nhacongdong.runmate.ui.session

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.data.local.RunSession
import com.nhacongdong.runmate.data.local.TrackPoint
import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.data.model.SessionStatus
import com.nhacongdong.runmate.domain.DayAgenda
import com.nhacongdong.runmate.domain.PlanRules
import com.nhacongdong.runmate.domain.RunSummary
import com.nhacongdong.runmate.ui.components.DialogCard
import com.nhacongdong.runmate.ui.components.GoalRing
import com.nhacongdong.runmate.ui.components.StatTile
import com.nhacongdong.runmate.ui.components.TrackCanvas
import com.nhacongdong.runmate.ui.components.previewMillis
import com.nhacongdong.runmate.ui.format.RunFormat
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import com.nhacongdong.runmate.ui.theme.tabular
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// S7 nối với ViewModel (SPEC 9.1). Xoá buổi chạy là việc của bước E2-3
@Composable
fun SessionDetailRoute(
    sessionId: Long,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: SessionDetailViewModel = viewModel(
        key = "session-$sessionId",
        factory = SessionDetailViewModel.factory(sessionId),
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { onDeleted() }
    }
    SessionDetailScreen(
        state = state,
        onBack = onBack,
        onDeleteSession = viewModel::deleteSession,
        onDeletePlan = viewModel::deletePlan,
    )
}

// Hộp thoại xác nhận đang mở: xoá buổi chạy hay xoá kế hoạch
private enum class DeleteAsk { SESSION, PLAN }

// S7 · Chi tiết buổi chạy (DESIGN 9 S7): phần đầu, quỹ đạo, quãng đường và vòng tỷ lệ, ba ô thống kê,
// menu xoá buổi chạy và xoá kế hoạch đã hoàn thành (mỗi lần đều hỏi xác nhận)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    state: SessionDetailUiState,
    onBack: () -> Unit,
    onDeleteSession: () -> Unit = {},
    onDeletePlan: () -> Unit = {},
    zone: ZoneId = ZoneId.systemDefault(),
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var ask by rememberSaveable { mutableStateOf<DeleteAsk?>(null) }
    val plan = state.plan
    val session = state.session

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (session != null) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.cd_more))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = {
                                        Text(stringResource(R.string.session_delete_menu), color = MaterialTheme.colorScheme.error)
                                    },
                                    onClick = {
                                        menuOpen = false
                                        ask = DeleteAsk.SESSION
                                    },
                                )
                                if (plan?.status == PlanStatus.COMPLETED) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(stringResource(R.string.plan_delete_menu), color = MaterialTheme.colorScheme.error)
                                        },
                                        onClick = {
                                            menuOpen = false
                                            ask = DeleteAsk.PLAN
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
                title = {},
                // Thanh trạng thái đã được Scaffold của RunMateApp chừa ra
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val summary = state.summary
        if (session == null || summary == null) return@Scaffold

        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.space4, vertical = Dimens.space4),
            verticalArrangement = Arrangement.spacedBy(Dimens.space4),
        ) {
            Header(session, zone)
            TrackCanvas(state.points, Modifier.fillMaxWidth())
            ResultRow(summary)
            StatRow(session, summary)
        }
    }

    // Hộp thoại xác nhận (DESIGN 10): nút xoá màu error ở bên phải, Huỷ ở bên trái
    val asked = ask
    if (asked != null && session != null) {
        Dialog(
            onDismissRequest = { ask = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            when (asked) {
                DeleteAsk.SESSION -> DeleteSessionCard(
                    planStatusLabel = plan?.let {
                        val status = PlanRules.statusAfterSessionRemoved(it.startTime, System.currentTimeMillis())
                        stringResource(if (status == PlanStatus.MISSED) R.string.status_missed else R.string.status_planned)
                    },
                    onConfirm = {
                        ask = null
                        onDeleteSession()
                    },
                    onCancel = { ask = null },
                )
                DeleteAsk.PLAN -> DeletePlanCard(
                    time = RunFormat.formatTime(session.startedAt, zone),
                    distance = RunFormat.formatDistanceKm(session.distanceMeters),
                    onConfirm = {
                        ask = null
                        onDeletePlan()
                    },
                    onCancel = { ask = null },
                )
            }
        }
    }
}

// Hộp thoại xoá buổi chạy: nếu thuộc kế hoạch thì báo kế hoạch sẽ quay về trạng thái nào (BR-53)
@Composable
private fun DeleteSessionCard(planStatusLabel: String?, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val text = if (planStatusLabel == null) {
        stringResource(R.string.delete_plan_message)
    } else {
        stringResource(R.string.session_delete_plan_note, planStatusLabel)
    }
    DialogCard(
        title = stringResource(R.string.session_delete_title),
        text = text,
        confirm = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismiss = { TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) } },
    )
}

// Hộp thoại xoá kế hoạch đã hoàn thành: buổi chạy được giữ lại thành chạy tự do (BR-52)
@Composable
private fun DeletePlanCard(time: String, distance: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    DialogCard(
        title = stringResource(R.string.delete_plan_title),
        text = stringResource(R.string.plan_delete_keep_session, time, distance),
        confirm = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.plan_delete_menu), color = MaterialTheme.colorScheme.error)
            }
        },
        dismiss = { TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) } },
    )
}

// "Thứ Năm, 29/10 · 06:02", tên địa điểm, nhãn "Theo kế hoạch" hoặc "Chạy tự do" (DESIGN 9 S7)
@Composable
private fun Header(session: RunSession, zone: ZoneId) {
    val date = DayAgenda.dateOf(session.startedAt, zone)
    Column(Modifier.padding(horizontal = Dimens.space2)) {
        Text(
            "${RunFormat.formatDayTitle(date)} · ${RunFormat.formatTime(session.startedAt, zone)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            session.locationName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(if (session.planId != null) R.string.session_from_plan else R.string.free_run),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Quãng đường lớn bên trái. Vòng tỷ lệ 96 dp bên phải, ghi phần trăm. Đạt mục tiêu thì dưới vòng có ✓ (DESIGN 9 S7)
@Composable
private fun ResultRow(summary: RunSummary) {
    val km = stringResource(R.string.run_distance_unit)
    Row(Modifier.fillMaxWidth().padding(horizontal = Dimens.space2), verticalAlignment = Alignment.CenterVertically) {
        // Số km và chữ "km" căn theo đường nền chữ, nằm cùng một hàng
        Row(Modifier.weight(1f)) {
            Text(
                RunFormat.formatDistanceKm(summary.distanceM),
                style = MaterialTheme.typography.displayMedium.tabular(),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.alignByBaseline(),
            )
            Spacer(Modifier.width(Dimens.space1))
            Text(
                km,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alignByBaseline(),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GoalRing(progress = summary.progress, size = 96.dp, strokeWidth = 8.dp) {
                Text(
                    RunFormat.formatPercent(summary.progress),
                    style = MaterialTheme.typography.titleMedium.tabular(),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
            if (summary.goalReached) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = com.nhacongdong.runmate.ui.theme.RunMateTheme.colors.success,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(Dimens.space1))
                    Text(
                        stringResource(R.string.cd_goal_reached),
                        style = MaterialTheme.typography.labelMedium,
                        color = com.nhacongdong.runmate.ui.theme.RunMateTheme.colors.success,
                    )
                }
            }
        }
    }
}

// Lưới ba ô: thời gian chạy, pace trung bình, mục tiêu (DESIGN 9 S7)
@Composable
private fun StatRow(session: RunSession, summary: RunSummary) {
    val pace = summary.paceSecPerKm?.let { RunFormat.formatDuration(it.toLong()) } ?: "--"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        val seconds = summary.durationMs / 1_000
        StatTile(
            label = stringResource(R.string.session_stat_time),
            value = RunFormat.formatDuration(seconds),
            modifier = Modifier.widthIn(min = 96.dp),
            centered = seconds < 3_600,   // chỉ có phút và giây thì căn giữa
        )
        StatTile(
            label = stringResource(R.string.session_pace_avg),
            value = pace,
            modifier = Modifier.widthIn(min = 96.dp),
            unit = stringResource(R.string.run_pace_unit),
            centered = true,
        )
        StatTile(
            label = stringResource(R.string.goal_title),
            value = RunFormat.formatGoal(session.goalType, session.goalValue),
            modifier = Modifier.widthIn(min = 96.dp),
            centered = true,
        )
    }
}

// Buổi chạy mẫu để xem trước: vòng quanh Hồ Hoàn Kiếm, khoảng 5,2 km trong 27 phút
private fun previewSession(goalValue: Int): SessionDetailUiState {
    val points = (0 until 60).map { i ->
        val a = i * 2 * PI / 52
        TrackPoint(
            sessionId = 1L,
            segmentIndex = if (i < 30) 0 else 1,
            timestamp = previewMillis(6, 2) + i * 27_000L,
            latitude = 21.028 + 0.0074 * sin(a),
            longitude = 105.852 + 0.0092 * cos(a),
            accuracyM = 5f,
        )
    }
    val session = RunSession(
        id = 1L,
        planId = 1L,
        status = SessionStatus.FINISHED,
        startedAt = previewMillis(6, 2),
        endedAt = previewMillis(6, 29),
        locationName = "Hồ Hoàn Kiếm",
        latitude = 21.028,
        longitude = 105.852,
        goalType = GoalType.DISTANCE,
        goalValue = goalValue,
        distanceMeters = 5_240.0,
        activeDurationSec = 1_620L,
        weather = null,
    )
    val summary = com.nhacongdong.runmate.domain.RunSummaryRules.summarize(points, session.goalType, session.goalValue)
    return SessionDetailUiState(session, points, summary)
}

@Preview(name = "Đạt mục tiêu · Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Đạt mục tiêu · Tối", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SessionDetailReachedPreview() = RunMateTheme {
    SessionDetailScreen(state = previewSession(goalValue = 5_000), onBack = {})
}

@Preview(name = "Chưa đạt mục tiêu · Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Composable
private fun SessionDetailNotReachedPreview() = RunMateTheme {
    SessionDetailScreen(state = previewSession(goalValue = 6_000), onBack = {})
}

// Buổi chạy dài để xem các số lớn: khoảng 89 km trong 4 giờ 10 phút, mục tiêu 80 km
private fun previewLongRun(): SessionDetailUiState {
    val points = (0 until 200).map { i ->
        TrackPoint(
            sessionId = 2L,
            segmentIndex = if (i < 100) 0 else 1,
            timestamp = previewMillis(5, 30) + i * 75_000L,
            latitude = 21.0 + 0.8 * i / 199.0,
            longitude = 105.85 + 0.005 * sin(i * 0.2),
            accuracyM = 5f,
        )
    }
    val session = RunSession(
        id = 2L,
        planId = null,
        status = SessionStatus.FINISHED,
        startedAt = previewMillis(5, 30),
        endedAt = previewMillis(9, 40),
        locationName = "Đường ven sông",
        latitude = 21.0,
        longitude = 105.85,
        goalType = GoalType.DISTANCE,
        goalValue = 80_000,
        distanceMeters = 89_000.0,
        activeDurationSec = 15_000L,
        weather = null,
    )
    val summary = com.nhacongdong.runmate.domain.RunSummaryRules.summarize(points, session.goalType, session.goalValue)
    return SessionDetailUiState(session, points, summary)
}

@Preview(name = "Số lớn · Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Số lớn · Tối", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SessionDetailLargeNumbersPreview() = RunMateTheme {
    SessionDetailScreen(state = previewLongRun(), onBack = {})
}

// Buổi chạy mục tiêu thời gian: 27 phút, mục tiêu 25 phút (đạt). Mục tiêu tính bằng giây (DESIGN 5.3)
private fun previewTimeGoal(goalSeconds: Int): SessionDetailUiState {
    val points = (0 until 60).map { i ->
        val a = i * 2 * PI / 52
        TrackPoint(
            sessionId = 3L,
            segmentIndex = if (i < 30) 0 else 1,
            timestamp = previewMillis(6, 2) + i * 27_000L,
            latitude = 21.028 + 0.0074 * sin(a),
            longitude = 105.852 + 0.0092 * cos(a),
            accuracyM = 5f,
        )
    }
    val session = RunSession(
        id = 3L,
        planId = null,
        status = SessionStatus.FINISHED,
        startedAt = previewMillis(6, 2),
        endedAt = previewMillis(6, 29),
        locationName = "Hồ Hoàn Kiếm",
        latitude = 21.028,
        longitude = 105.852,
        goalType = GoalType.DURATION,
        goalValue = goalSeconds,
        distanceMeters = 5_240.0,
        activeDurationSec = 1_620L,
        weather = null,
    )
    val summary = com.nhacongdong.runmate.domain.RunSummaryRules.summarize(points, session.goalType, session.goalValue)
    return SessionDetailUiState(session, points, summary)
}

@Preview(name = "Mục tiêu thời gian · Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Mục tiêu thời gian · Tối", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SessionDetailTimeGoalPreview() = RunMateTheme {
    SessionDetailScreen(state = previewTimeGoal(goalSeconds = 1_500), onBack = {})
}

@Preview(name = "Mục tiêu thời gian chưa đạt · Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Composable
private fun SessionDetailTimeGoalNotReachedPreview() = RunMateTheme {
    SessionDetailScreen(state = previewTimeGoal(goalSeconds = 2_700), onBack = {})
}

// Buổi chạy 22 phút 30 giây, 4,24 km (mục tiêu 5 km)
private fun preview22m30s(): SessionDetailUiState {
    val points = (0 until 50).map { i ->
        val a = i * 2 * PI / 45
        TrackPoint(
            sessionId = 4L,
            segmentIndex = 0,
            timestamp = previewMillis(6, 10) + i * 27_000L,
            latitude = 21.028 + 0.005 * sin(a),
            longitude = 105.852 + 0.007 * cos(a),
            accuracyM = 5f,
        )
    }
    val session = RunSession(
        id = 4L,
        planId = null,
        status = SessionStatus.FINISHED,
        startedAt = previewMillis(6, 10),
        endedAt = previewMillis(6, 32) + 30_000L,
        locationName = "Hồ Hoàn Kiếm",
        latitude = 21.028,
        longitude = 105.852,
        goalType = GoalType.DISTANCE,
        goalValue = 5_000,
        distanceMeters = 4_240.0,
        activeDurationSec = 1_350L,
        weather = null,
    )
    val summary = com.nhacongdong.runmate.domain.RunSummaryRules.summarize(points, session.goalType, session.goalValue)
    return SessionDetailUiState(session, points, summary)
}

@Preview(name = "Buổi chạy 22m30s (4,24km) · Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Buổi chạy 22m30s (4,24km) · Tối", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SessionDetail22m30sPreview() = RunMateTheme {
    SessionDetailScreen(state = preview22m30s(), onBack = {})
}
