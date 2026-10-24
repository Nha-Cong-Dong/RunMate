package com.nhacongdong.runmate.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

// Màn hình tạm của khung code (bước 3.4).
// Sẽ được thay bằng màn hình thật ở các giai đoạn B–F.
data class PlaceholderAction(@StringRes val label: Int, val onClick: () -> Unit)

@Composable
fun PlaceholderScreen(
    @StringRes title: Int,
    actions: List<PlaceholderAction>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
        actions.forEach { action ->
            Button(onClick = action.onClick) { Text(stringResource(action.label)) }
        }
    }
}
