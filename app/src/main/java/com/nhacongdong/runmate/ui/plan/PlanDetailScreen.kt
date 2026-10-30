package com.nhacongdong.runmate.ui.plan

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.data.local.RunPlan
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.domain.PlanRules
import com.nhacongdong.runmate.ui.components.PreviewZone
import com.nhacongdong.runmate.ui.components.StartRunButton
import com.nhacongdong.runmate.ui.components.StatusChip
import com.nhacongdong.runmate.ui.components.previewPlan
import com.nhacongdong.runmate.ui.format.RunFormat
import com.nhacongdong.runmate.ui.navigation.PlanDetail
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import java.time.Instant
import java.time.ZoneId

// S4 nối với ViewModel. Huỷ hoặc xoá xong thì ViewModel báo để về S1
@Composable
fun PlanDetailRoute(
    args: PlanDetail,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenRun: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: PlanDetailViewModel = viewModel(factory = PlanDetailViewModel.factory(args.planId)),
) {
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    // Mỗi lần S4 hiện lên thì cập nhật MISSED, giống S1 (SPEC 6.3)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshStatus() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { onDeleted() }
    }

    PlanDetailScreen(
        plan = plan,
        onBack = onBack,
        onEdit = { onEdit(args.planId) },
        onCancel = viewModel::cancelPlan,
        onDelete = viewModel::deletePlan,
        onOpenRun = onOpenRun,
        onStartRun = onOpenRun,
    )
}

// S4 · Chi tiết kế hoạch (DESIGN 9 S4): menu Sửa, Huỷ kế hoạch, Xoá; phần đầu; nút đáy theo trạng thái
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    plan: RunPlan?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onOpenRun: () -> Unit,
    onStartRun: () -> Unit,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { if (plan != null) StatusChip(plan.status) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (plan != null) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.cd_more))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = {
                                        MenuItemText(
                                            label = stringResource(R.string.action_edit),
                                            reason = editBlockedReason(plan.status),
                                        )
                                    },
                                    onClick = { menuOpen = false; onEdit() },
                                    enabled = PlanRules.canEdit(plan.status),
                                )
                                DropdownMenuItem(
                                    text = {
                                        MenuItemText(
                                            label = stringResource(R.string.action_cancel_plan),
                                            reason = cancelBlockedReason(plan.status),
                                        )
                                    },
                                    onClick = { menuOpen = false; onCancel() },
                                    enabled = PlanRules.canCancel(plan.status),
                                )
                                DropdownMenuItem(
                                    text = {
                                        MenuItemText(
                                            label = stringResource(R.string.action_delete),
                                            reason = deleteBlockedReason(plan.status),
                                            labelColor = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = { menuOpen = false; confirmDelete = true },
                                    enabled = PlanRules.canDelete(plan.status),
                                )
                            }
                        }
                    }
                },
                // Thanh trạng thái đã được Scaffold của RunMateApp chừa ra
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (plan != null) {
                BottomAction(plan = plan, onStartRun = onStartRun, onOpenRun = onOpenRun)
            }
        },
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (plan != null) {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.space4),
                verticalArrangement = Arrangement.spacedBy(Dimens.space2),
            ) {
                InfoLine(
                    icon = R.drawable.ic_calendar_month,
                    text = RunFormat.formatDayTitle(Instant.ofEpochMilli(plan.startTime).atZone(zone).toLocalDate()),
                    style = MaterialTheme.typography.titleMedium,
                )
                InfoLine(
                    icon = R.drawable.ic_schedule,
                    text = RunFormat.formatTime(plan.startTime, zone),
                    style = MaterialTheme.typography.displayMedium,
                )
                InfoLine(
                    icon = R.drawable.ic_location_on,
                    text = plan.locationName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                )
                InfoLine(
                    icon = R.drawable.ic_flag,
                    text = stringResource(R.string.plan_goal, RunFormat.formatGoal(plan.goalType, plan.goalValue)),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_plan_title), style = MaterialTheme.typography.titleMedium) },
            text = { Text(stringResource(R.string.delete_plan_message), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
            tonalElevation = 0.dp,
        )
    }
}

// Một dòng thông tin có biểu tượng 20 dp, màu textSecondary (DESIGN 9 S4, mục 7)
@Composable
private fun InfoLine(
    icon: Int,
    text: String,
    style: TextStyle,
    maxLines: Int = Int.MAX_VALUE,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text,
            style = style,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Nút đáy theo trạng thái (DESIGN 9 S4). COMPLETED không có nút đáy
@Composable
private fun BottomAction(plan: RunPlan, onStartRun: () -> Unit, onOpenRun: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.space4, vertical = Dimens.space4),
    ) {
        when (plan.status) {
            PlanStatus.PLANNED -> StartRunButton(
                label = stringResource(R.string.action_start_run),
                enabled = true,
                onClick = onStartRun,
                icon = R.drawable.ic_play_arrow,
            )
            PlanStatus.MISSED -> StartRunButton(
                label = stringResource(R.string.action_start_late),
                enabled = true,
                onClick = onStartRun,
                icon = R.drawable.ic_play_arrow,
            )
            PlanStatus.IN_PROGRESS -> StartRunButton(
                label = stringResource(R.string.action_back_to_run),
                enabled = true,
                onClick = onOpenRun,
            )
            PlanStatus.CANCELLED -> Text(
                stringResource(R.string.hint_reschedule),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PlanStatus.COMPLETED -> Unit
        }
    }
}

// Tên mục menu, và dòng lý do bên dưới khi mục bị tắt (DESIGN 9 S4)
@Composable
private fun MenuItemText(label: String, reason: Int?, labelColor: Color = Color.Unspecified) {
    Column {
        Text(label, color = labelColor)
        if (reason != null) {
            Text(stringResource(reason), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Lý do không sửa được (SPEC 6.4), hiện làm dòng phụ của mục menu bị tắt
private fun editBlockedReason(status: PlanStatus): Int? = when (status) {
    PlanStatus.IN_PROGRESS -> R.string.blocked_edit_running
    PlanStatus.COMPLETED -> R.string.blocked_edit_completed
    else -> null
}

private fun cancelBlockedReason(status: PlanStatus): Int? = when (status) {
    PlanStatus.IN_PROGRESS -> R.string.blocked_cancel_running
    PlanStatus.COMPLETED -> R.string.blocked_cancel_completed
    PlanStatus.CANCELLED -> R.string.blocked_cancel_done
    else -> null
}

private fun deleteBlockedReason(status: PlanStatus): Int? =
    if (status == PlanStatus.IN_PROGRESS) R.string.blocked_delete_running else null

@Preview(name = "Chưa chạy · Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Chưa chạy · Tối", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlanDetailPlannedPreview() = RunMateTheme {
    PlanDetailScreen(
        plan = previewPlan(PlanStatus.PLANNED),
        onBack = {}, onEdit = {}, onCancel = {}, onDelete = {}, onOpenRun = {}, onStartRun = {},
        zone = PreviewZone,
    )
}

@Preview(name = "Đã huỷ · Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Composable
private fun PlanDetailCancelledPreview() = RunMateTheme {
    PlanDetailScreen(
        plan = previewPlan(PlanStatus.CANCELLED),
        onBack = {}, onEdit = {}, onCancel = {}, onDelete = {}, onOpenRun = {}, onStartRun = {},
        zone = PreviewZone,
    )
}
