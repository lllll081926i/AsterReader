package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import com.asterreader.core.designsystem.AsterChoice
import com.asterreader.core.designsystem.AsterLayout
import com.asterreader.core.designsystem.AsterSettingsChoice
import com.example.ui.AsterUiState
import com.example.ui.AsterViewModel
import com.example.ui.ReaderLayoutMode
import kotlin.math.roundToInt

/** 手机与平板共用阅读设置；默认值与单本覆盖分别保存。 */
@Composable
fun ReaderSettingsContent(
    state: AsterUiState,
    model: AsterViewModel,
    mode: ReaderLayoutMode,
    fit: String,
    cover: Boolean,
    rightToLeft: Boolean,
    onMode: (ReaderLayoutMode) -> Unit,
    onFit: (String) -> Unit,
    onCover: (Boolean) -> Unit,
    onRightToLeft: (Boolean) -> Unit,
    onReset: () -> Unit,
    showStatus: Boolean,
    onShowStatus: (Boolean) -> Unit
) {
    var defaults by rememberSaveable { mutableStateOf(false) }
    val selectedMode = if (defaults) state.readerLayoutMode else mode
    val selectedFit = if (defaults) state.fitModeLabel else fit
    val selectedCover = if (defaults) state.smartSpreadStitch else cover
    Column(verticalArrangement = Arrangement.spacedBy(AsterLayout.CompactSpacing)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AsterLayout.CompactSpacing)) {
            AsterSettingsChoice(listOf(AsterChoice("本书", Icons.Outlined.Book), AsterChoice("默认", Icons.Outlined.Settings)),
                if (defaults) 1 else 0, { defaults = it == 1 }, Modifier.weight(1f), compact = true)
            IconButton(onClick = onReset, enabled = !defaults,
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)) {
                Icon(Icons.Outlined.RestartAlt, "恢复默认", Modifier.size(AsterLayout.HeaderIconSize))
            }
        }
        ReaderSettingsLabel("翻页")
        AsterSettingsChoice(readerModeChoices, ReaderLayoutMode.entries.indexOf(selectedMode), {
            val next = ReaderLayoutMode.entries[it]
            if (defaults) model.setReaderLayoutMode(next) else onMode(next)
        }, compact = true, singleRow = true)
        if (!defaults && selectedMode == ReaderLayoutMode.DUAL_PAGE) {
            AsterSwitchRow("从右向左", checked = rightToLeft, onCheckedChange = onRightToLeft)
        }
        ReaderSettingsLabel("缩放")
        AsterSettingsChoice(readerFitChoices, if (selectedFit == "适合屏幕") 0 else 1, {
            val next = if (it == 0) "适合屏幕" else "原始比例"
            if (defaults) model.setFitModeLabel(next) else onFit(next)
        }, compact = true)
        AsterSwitchRow("封面单页", checked = selectedCover,
            onCheckedChange = { if (defaults) model.setSmartSpreadStitch(it) else onCover(it) })
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ReaderSettingsLabel("预加载（通用）")
        PreloadPagesRow("向上预加载", state.preloadBefore, model::setPreloadBefore)
        PreloadPagesRow("向下预加载", state.preloadAfter, model::setPreloadAfter)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("亮度", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(AsterLayout.ControlSpacing))
            Slider(state.readerBrightness, model::setReaderBrightness,
                Modifier.weight(1f).semantics { contentDescription = "亮度" }, valueRange = 0.25f..1f)
            Spacer(Modifier.width(AsterLayout.ControlSpacing))
            Text("${(state.readerBrightness * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
        }
        AsterSwitchRow("音量键翻页", checked = state.volumeKeyPageTurn, onCheckedChange = model::setVolumeKeyPageTurn)
        AsterSwitchRow("状态条", checked = showStatus, onCheckedChange = onShowStatus)
    }
}

@Composable
private fun PreloadPagesRow(label: String, pages: Int, onChange: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("$pages 页", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(pages.toFloat(), { onChange(it.roundToInt()) },
            Modifier.fillMaxWidth().semantics { contentDescription = label }, valueRange = 0f..16f, steps = 15)
    }
}

@Composable
private fun ReaderSettingsLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary)
}
