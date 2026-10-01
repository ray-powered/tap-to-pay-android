package com.yumedev.taptopayandroid.presentation.ui.components

import android.view.SoundEffectConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yumedev.taptopayandroid.R

private val KEYPAD_SUBTITLES = mapOf(
    "1" to "",
    "2" to "ABC",
    "3" to "DEF",
    "4" to "GHI",
    "5" to "JKL",
    "6" to "MNO",
    "7" to "PQRS",
    "8" to "TUV",
    "9" to "WXYZ",
    "C" to "CLEAR",
    "0" to "+",
    "⌫" to "BACK"
)

@Composable
fun Keypad(
    onNumberClick: (Int) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit
) {
    val keys = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        "C", "0", "⌫"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    PosHardwareKey(
                        label = key,
                        subLabel = KEYPAD_SUBTITLES[key] ?: "",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (key) {
                                "C" -> onClear()
                                "⌫" -> onBackspace()
                                else -> onNumberClick(key.toInt())
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PosHardwareKey(
    label: String,
    subLabel: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val isClear = label == "C"
    val isDelete = label == "⌫"
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // POS keycap colors:
    // In light mode, maintain clean uniform surface keys without tinted patches or contrasting borders.
    // In dark mode, provide subtle POS hardware accent keycaps for Clear and Delete.
    val keyBg = when {
        !isDark -> MaterialTheme.colorScheme.surface
        isClear -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        isDelete -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.surface
    }

    val keyBorderColor = when {
        !isDark -> MaterialTheme.colorScheme.outlineVariant
        isClear -> MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                onClick()
            },
        shape = RoundedCornerShape(10.dp),
        color = keyBg,
        border = BorderStroke(1.dp, keyBorderColor),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isDelete) {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = "Delete",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isClear) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp
                )
            }

            if (subLabel.isNotEmpty()) {
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = if (isClear) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
