package com.nhacongdong.runmate.ui.plan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

// Vuốt ngang để đổi ngày trên S1 (DESIGN 9 S1, mục 11). Chỉ nhận vuốt ngang, cuộn dọc của danh sách không bị ảnh hưởng.
// Danh sách bám theo tay khi vuốt. Tới ngưỡng thì đổi ngày và trượt sang ngày mới, chưa tới thì trở về chỗ cũ.
private val SWIPE_THRESHOLD = 80.dp
private const val SLIDE_MS = 200

@Composable
fun SwipeDayContainer(
    selected: LocalDate,
    onSwipeDay: (Long) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (LocalDate) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { SWIPE_THRESHOLD.toPx() }
    val dragX = remember { Animatable(0f) }
    val currentOnSwipe by rememberUpdatedState(onSwipeDay)

    Box(
        modifier
            .pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onHorizontalDrag = { _, amount ->
                        total += amount
                        scope.launch { dragX.snapTo(total) }
                    },
                    onDragEnd = {
                        if (abs(total) >= thresholdPx) {
                            scope.launch { dragX.snapTo(0f) }
                            // Vuốt sang trái là ngày sau
                            currentOnSwipe(if (total < 0) 1L else -1L)
                        } else {
                            scope.launch { dragX.animateTo(0f, tween(SLIDE_MS, easing = LinearOutSlowInEasing)) }
                        }
                    },
                    onDragCancel = { scope.launch { dragX.animateTo(0f, tween(SLIDE_MS)) } },
                )
            }
            .offset { IntOffset(dragX.value.roundToInt(), 0) },
    ) {
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                // Ngày sau trượt vào từ phải và ngày cũ trượt ra bên trái. Ngày trước thì ngược lại
                val forward = targetState > initialState
                (slideInHorizontally(tween(SLIDE_MS, easing = LinearOutSlowInEasing)) { if (forward) it else -it } togetherWith
                    slideOutHorizontally(tween(SLIDE_MS, easing = LinearOutSlowInEasing)) { if (forward) -it else it })
            },
            label = "swipe-day",
        ) { date ->
            content(date)
        }
    }
}
