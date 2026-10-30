package com.nhacongdong.runmate.ui.plan

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.domain.WeekRules
import com.nhacongdong.runmate.domain.model.Place
import com.nhacongdong.runmate.domain.model.PlanError
import com.nhacongdong.runmate.ui.components.GoalPicker
import com.nhacongdong.runmate.ui.components.LocationRow
import com.nhacongdong.runmate.ui.format.RunFormat
import com.nhacongdong.runmate.ui.navigation.PlanEdit
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

// S2 nối với ViewModel. Chọn vị trí đi qua S3, kết quả quay về qua pickedPlace (SavedStateHandle ở RunMateApp)
@Composable
fun PlanEditRoute(
    args: PlanEdit,
    pickedPlace: Place?,
    onPlaceConsumed: () -> Unit,
    onPickLocation: () -> Unit,
    onDone: () -> Unit,
    viewModel: PlanEditViewModel = viewModel(
        factory = PlanEditViewModel.factory(args.planId, args.initialDay?.let(LocalDate::ofEpochDay)),
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(pickedPlace) {
        pickedPlace?.let {
            viewModel.onLocationPicked(it)
            onPlaceConsumed()
        }
    }
    // Lưu xong thì quay lại màn hình trước: S1 khi tạo, S4 khi sửa (SPEC 9.3)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { onDone() }
    }

    PlanEditScreen(
        state = state,
        onDateChange = viewModel::onDateChange,
        onTimeChange = viewModel::onTimeChange,
        onPickLocation = onPickLocation,
        onGoalTypeChange = viewModel::onGoalTypeChange,
        onGoalValueChange = viewModel::onGoalValueChange,
        onSave = viewModel::save,
        onClose = onDone,
    )
}

// S2 · Tạo / Sửa kế hoạch (DESIGN 9 S2): ngày và giờ, vị trí, mục tiêu, đáy là nút Lưu
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditScreen(
    state: PlanEditUiState,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    onPickLocation: () -> Unit,
    onGoalTypeChange: (GoalType) -> Unit,
    onGoalValueChange: (Int) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(painterResource(R.drawable.ic_close), stringResource(R.string.cd_close))
                    }
                },
                title = {
                    Text(
                        stringResource(if (state.isEditing) R.string.plan_edit_title else R.string.plan_new_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                // Thanh trạng thái đã được Scaffold của RunMateApp chừa ra
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.space4, vertical = Dimens.space4)
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(stringResource(R.string.action_save_plan), style = MaterialTheme.typography.labelLarge)
            }
        },
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (!state.isLoading) {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.space4),
                verticalArrangement = Arrangement.spacedBy(Dimens.space4),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.space2)) {
                        FieldBox(
                            label = stringResource(R.string.label_date),
                            value = RunFormat.formatDayTitle(state.start.toLocalDate()),
                            onClick = { showDatePicker = true },
                        )
                        if (PlanError.START_TOO_FAR in state.errors) ErrorText(stringResource(R.string.error_start_too_far))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.space2)) {
                        FieldBox(
                            label = stringResource(R.string.label_time),
                            value = "%02d:%02d".format(state.start.hour, state.start.minute),
                            onClick = { showTimePicker = true },
                        )
                        if (PlanError.START_IN_PAST in state.errors) ErrorText(stringResource(R.string.error_start_past))
                    }
                }

                LocationRow(
                    name = state.location?.name,
                    isError = state.locationMissing,
                    onClick = onPickLocation,
                )

                GoalPicker(
                    goalType = state.goalType,
                    goalValue = state.goalValue,
                    onGoalTypeChange = onGoalTypeChange,
                    onGoalValueChange = onGoalValueChange,
                )
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = state.start.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                // Ngoài hôm nay đến hôm nay + 16 ngày thì làm mờ (BR-02)
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    WeekRules.isPlannable(utcDate(utcTimeMillis), state.today)
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDateChange(utcDate(it)) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_select)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
            tonalElevation = 0.dp,
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = state.start.hour,
            initialMinute = state.start.minute,
            is24Hour = true,
        )
        // Hộp chọn giờ hiện giờ đang chọn bằng displayLarge (96 sp), nên đổi sang displayMedium (DESIGN 9 S2)
        MaterialTheme(typography = MaterialTheme.typography.copy(displayLarge = MaterialTheme.typography.displayMedium)) {
            TimePickerDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text(stringResource(R.string.label_time), style = MaterialTheme.typography.titleMedium) },
                confirmButton = {
                    TextButton(onClick = {
                        onTimeChange(LocalTime.of(timeState.hour, timeState.minute))
                        showTimePicker = false
                    }) { Text(stringResource(R.string.action_select)) }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.action_cancel)) }
                },
            ) {
                TimePicker(state = timeState)
            }
        }
    }
}

// DatePicker của Material 3 làm việc với mốc nửa đêm UTC
private fun utcDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

// Ô ngày và giờ: nền surfaceContainer, bo radiusM, cao 56 dp
@Composable
private fun FieldBox(label: String, value: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(Dimens.radiusM))
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.space4),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Chữ lỗi ngay dưới ô sai (DESIGN 9 S2)
@Composable
private fun ErrorText(message: String) {
    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

private val PreviewToday: LocalDate = LocalDate.of(2026, 10, 29)

@Composable
private fun PreviewScreen(state: PlanEditUiState) = RunMateTheme {
    PlanEditScreen(
        state = state,
        onDateChange = {}, onTimeChange = {}, onPickLocation = {},
        onGoalTypeChange = {}, onGoalValueChange = {}, onSave = {}, onClose = {},
    )
}

@Preview(name = "Tạo mới · Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Tạo mới · Tối", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlanEditNewPreview() = PreviewScreen(
    PlanEditUiState(
        isLoading = false,
        today = PreviewToday,
        start = LocalDateTime.of(2026, 10, 29, 9, 0),
        location = Place("Hồ Hoàn Kiếm", "Hoàn Kiếm, Hà Nội", 21.028, 105.852),
    ),
)

@Preview(name = "Giờ đã qua · Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Composable
private fun PlanEditErrorPreview() = PreviewScreen(
    PlanEditUiState(
        isEditing = true,
        isLoading = false,
        today = PreviewToday,
        start = LocalDateTime.of(2026, 10, 29, 7, 0),
        location = Place("Hồ Hoàn Kiếm", "Hoàn Kiếm, Hà Nội", 21.028, 105.852),
        goalType = GoalType.DURATION,
        goalValue = 1_800,
        errors = listOf(PlanError.START_IN_PAST),
    ),
)
