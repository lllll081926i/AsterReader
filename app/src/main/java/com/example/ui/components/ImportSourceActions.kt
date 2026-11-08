package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.asterreader.core.designsystem.AsterLayout

/** 文件与目录各保留一个入口，左右等宽，文字允许随系统大字体换行。 */
@Composable
internal fun ImportSourceActions(onImportFile: () -> Unit, onAddDirectory: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(AsterLayout.GroupSpacing)
    ) {
        ImportSourceAction("导入文件", "压缩包、PDF、图片", Icons.Outlined.Description, onImportFile,
            Modifier.weight(1f).fillMaxHeight())
        ImportSourceAction("添加目录", "扫描文件夹中的书籍", Icons.Outlined.CreateNewFolder, onAddDirectory,
            Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun ImportSourceAction(
    title: String, description: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = AsterLayout.TouchTarget),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Column(
            Modifier.padding(AsterLayout.PagePadding),
            verticalArrangement = Arrangement.spacedBy(AsterLayout.ControlSpacing)
        ) {
            Icon(icon, contentDescription = null)
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
