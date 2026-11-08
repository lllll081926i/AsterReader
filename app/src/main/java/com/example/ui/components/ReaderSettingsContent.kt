package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.asterreader.core.designsystem.AsterChoice
import com.asterreader.core.designsystem.AsterLayout
import com.example.ui.AsterUiState
import com.example.ui.AsterViewModel
import com.example.ui.ReaderLayoutMode
import kotlin.math.roundToInt

/** 单本和默认共用同一份设置表单，页面选项按需展开。 */
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
    com.asterreader.core.designsystem.AsterSettingsScale {
        ReaderSettingsContentBody(state, model, mode, fit, cover, rightToLeft, onMode, onFit, onCover, onRightToLeft, onReset, showStatus, onShowStatus)
    }
}

@Composable
private fun ReaderSettingsContentBody(
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
    var picker by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedMode = if (defaults) state.readerLayoutMode else mode
    val selectedFit = if (defaults) state.fitModeLabel else fit
    val selectedCover = if (defaults) state.smartSpreadStitch else cover
    val modeLabels = listOf("上下滚动", "从右向左", "从左向右", "双页")
    Column(verticalArrangement = Arrangement.spacedBy(AsterLayout.GroupSpacing)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TabRow(selectedTabIndex = if (defaults) 1 else 0, modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.background) {
                    Tab(selected = !defaults, onClick = { defaults = false }, text = { Text("本书") })
                    Tab(selected = defaults, onClick = { defaults = true }, text = { Text("默认") })
                }
                IconButton(onClick = onReset, enabled = !defaults) {
                    Icon(Icons.Outlined.RestartAlt, "恢复默认", Modifier.size(AsterLayout.HeaderIconSize))
                }
            }
            Text(if (defaults) "用于未单独设置的漫画" else "仅应用于当前漫画",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = AsterLayout.ControlSpacing))
        }
        AsterSettingsGroup("页面") {
            AsterSettingsRow("翻页", trailingText = modeLabels[ReaderLayoutMode.entries.indexOf(selectedMode)],
                onClick = { picker = "mode" })
            AsterGroupDivider()
            AsterSettingsRow("缩放", trailingText = selectedFit, onClick = { picker = "fit" })
            AsterGroupDivider()
            AsterSwitchRow("封面单页", checked = selectedCover,
                onCheckedChange = { if (defaults) model.setSmartSpreadStitch(it) else onCover(it) })
            if (!defaults && selectedMode == ReaderLayoutMode.DUAL_PAGE) {
                AsterGroupDivider()
                AsterSwitchRow("从右向左", checked = rightToLeft, onCheckedChange = onRightToLeft)
            }
        }
        AsterSettingsGroup("预加载 · 通用") {
            ReaderValueSlider("向上预加载", "${state.preloadBefore} 页", state.preloadBefore.toFloat(),
                0f..16f, 15) { model.setPreloadBefore(it.roundToInt()) }
            AsterGroupDivider()
            ReaderValueSlider("向下预加载", "${state.preloadAfter} 页", state.preloadAfter.toFloat(),
                0f..16f, 15) { model.setPreloadAfter(it.roundToInt()) }
        }
        AsterSettingsGroup("屏幕") {
            ReaderValueSlider("亮度", "${(state.readerBrightness * 100).roundToInt()}%", state.readerBrightness,
                0.2f..1f, onChange = model::setReaderBrightness)
            AsterGroupDivider()
            AsterSwitchRow("状态条", checked = showStatus, onCheckedChange = onShowStatus)
        }
        AsterSettingsGroup("操作") {
            AsterSwitchRow("音量键翻页", checked = state.volumeKeyPageTurn, onCheckedChange = model::setVolumeKeyPageTurn)
        }
        Spacer(Modifier.height(AsterLayout.BottomSpacing))
    }
    if (picker != null) {
        val choosingMode = picker == "mode"
        val choices = if (choosingMode) readerModeChoices.mapIndexed { index, choice ->
            AsterChoice(modeLabels[index], choice.icon, choice.description)
        } else readerFitChoices
        val selected = if (choosingMode) ReaderLayoutMode.entries.indexOf(selectedMode)
            else if (selectedFit == "适合屏幕") 0 else 1
        AlertDialog(onDismissRequest = { picker = null }, title = { Text(if (choosingMode) "翻页" else "缩放") },
            text = {
                Column(Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                    choices.forEachIndexed { index, choice ->
                        Row(Modifier.fillMaxWidth().selectable(index == selected, role = Role.RadioButton,
                            onClick = {
                                if (choosingMode) {
                                    if (defaults) model.setReaderLayoutMode(ReaderLayoutMode.entries[index])
                                    else onMode(ReaderLayoutMode.entries[index])
                                } else {
                                    if (defaults) model.setFitModeLabel(choice.label) else onFit(choice.label)
                                }
                                picker = null
                            }).heightIn(min = AsterLayout.TouchTarget)
                            .semantics { contentDescription = choice.description ?: choice.label },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AsterLayout.ControlSpacing)) {
                            RadioButton(index == selected, onClick = null)
                            Text(choice.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }, confirmButton = { TextButton(onClick = { picker = null }) { Text("取消") } })
    }
}

@Composable
private fun ReaderValueSlider(
    label: String, valueText: String, value: Float, range: ClosedFloatingPointRange<Float>,
    steps: Int = 0, onChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = AsterLayout.PagePadding, vertical = AsterLayout.ControlSpacing)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AsterLayout.ControlSpacing)) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(valueText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value, onChange, Modifier.fillMaxWidth().semantics { contentDescription = label },
            valueRange = range, steps = steps)
    }
}
