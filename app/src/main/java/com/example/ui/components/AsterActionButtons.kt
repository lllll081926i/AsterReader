package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.asterreader.core.designsystem.AsterLayout

/** 并列操作保持等宽同高；大字体和窄窗口整组换行。 */
@Composable
fun AsterPairedActions(
    leftLabel: String,
    leftIcon: ImageVector,
    onLeft: () -> Unit,
    rightLabel: String,
    rightIcon: ImageVector,
    onRight: () -> Unit,
    modifier: Modifier = Modifier,
    primaryOnRight: Boolean = false,
    rightEnabled: Boolean = true
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.widthIn(max = 480.dp).fillMaxWidth()) {
        val stacked = maxWidth < (56.dp + 72.dp * fontScale) * 2 + AsterLayout.GroupSpacing
        @Composable fun action(left: Boolean, actionModifier: Modifier) {
            val content: @Composable RowScope.() -> Unit = {
                Icon(if (left) leftIcon else rightIcon, null, Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(if (left) leftLabel else rightLabel, textAlign = TextAlign.Center)
            }
            val enabled = left || rightEnabled
            val click = if (left) onLeft else onRight
            if (left != primaryOnRight) Button(click, actionModifier.heightIn(min = AsterLayout.TouchTarget), enabled = enabled, content = content)
            else OutlinedButton(click, actionModifier.heightIn(min = AsterLayout.TouchTarget), enabled = enabled, content = content)
        }
        if (stacked) Column(verticalArrangement = Arrangement.spacedBy(AsterLayout.GroupSpacing)) {
            action(true, Modifier.fillMaxWidth())
            action(false, Modifier.fillMaxWidth())
        } else Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AsterLayout.GroupSpacing)) {
            action(true, Modifier.weight(1f).fillMaxHeight())
            action(false, Modifier.weight(1f).fillMaxHeight())
        }
    }
}
