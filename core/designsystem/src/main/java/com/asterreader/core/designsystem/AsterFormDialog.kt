package com.asterreader.core.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** 表单使用窗口约束，避免滚动内容参与弹窗窗口的反复固有尺寸测量。 */
@Composable
fun AsterFormDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit
) {
    Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(AsterLayout.PagePadding), contentAlignment = Alignment.Center) {
            Surface(
                Modifier.widthIn(max = AsterLayout.AuthPanelMaxWidth).fillMaxWidth().heightIn(max = maxHeight),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(Modifier.padding(AsterLayout.PagePadding), verticalArrangement = Arrangement.spacedBy(AsterLayout.GroupSpacing)) {
                    ProvideTextStyle(MaterialTheme.typography.headlineSmall, content = title)
                    Box(Modifier.weight(1f, fill = false)) { text() }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically) {
                        dismissButton()
                        Spacer(Modifier.width(AsterLayout.ControlSpacing))
                        confirmButton()
                    }
                }
            }
        }
    }
}
