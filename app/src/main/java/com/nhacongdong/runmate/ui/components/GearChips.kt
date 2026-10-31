package com.nhacongdong.runmate.ui.components

import android.content.res.Configuration

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.domain.Gear
import com.nhacongdong.runmate.ui.format.iconRes
import com.nhacongdong.runmate.ui.format.labelRes
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.RunMateTheme

// Gợi ý vật dụng (DESIGN 8.12). Chip cao 32 dp, viền 1 dp. Không có gợi ý thì không hiện gì
@Composable
fun GearChips(
    gear: Set<Gear>,
    modifier: Modifier = Modifier,
) {
    if (gear.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.space2)) {
        Text(
            stringResource(R.string.gear_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
            verticalArrangement = Arrangement.spacedBy(Dimens.space2),
        ) {
            gear.forEach { item ->
                Row(
                    Modifier
                        .height(32.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space1),
                ) {
                    Icon(
                        painterResource(item.iconRes()),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        stringResource(item.labelRes()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun GearChipsPreviewScreen(gear: Set<Gear>) = RunMateTheme {
    GearChips(
        gear = gear,
        modifier = Modifier.padding(Dimens.space4),
    )
}

@Preview(name = "Một vật dụng · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Một vật dụng · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GearChipsOnePreview() = GearChipsPreviewScreen(setOf(Gear.WATER))

@Preview(name = "Năm vật dụng · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Năm vật dụng · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GearChipsAllPreview() = GearChipsPreviewScreen(
    setOf(Gear.RAIN_COAT, Gear.HAT, Gear.SUNSCREEN, Gear.WATER, Gear.WINDBREAKER),
)
