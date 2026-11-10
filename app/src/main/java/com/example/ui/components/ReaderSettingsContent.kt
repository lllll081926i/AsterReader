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
import com.asterreader.core.designsystem.AsterPreferenceGroup
import com.asterreader.core.designsystem.AsterPreferenceItem
import com.asterreader.core.designsystem.AsterPreferenceSwitch
import com.asterreader.core.designsystem.AsterPreferenceDivider
import com.asterreader.core.designsystem.AsterPreferenceSlider
import com.asterreader.core.designsystem.AsterPreferenceLayout
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
    val modeLabels = ReaderLayoutMode.entries.map(::readerModeLabel)
    Column(verticalArrangement = Arrangement.spacedBy(AsterPreferenceLayout.SectionSpacing)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TabRow(selectedTabIndex = if (defaults) 1 else 0, modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = !defaults, onClick = { defaults = false }, text = { Text("本书") })
                Tab(selected = defaults, onClick = { defaults = true }, text = { Text("默认") })
            }
            IconButton(onClick = onReset, enabled = !defaults) {
                Icon(Icons.Outlined.RestartAlt, "恢复默认", Modifier.size(AsterPreferenceLayout.IconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (defaults) 0.38f else 1f))
            }
        }
        AsterPreferenceGroup("页面") {
            AsterPreferenceItem("翻页", trailingText = modeLabels[ReaderLayoutMode.entries.indexOf(selectedMode)],
                onClick = { picker = "mode" })
            AsterPreferenceDivider()
            AsterPreferenceItem("缩放", trailingText = selectedFit, onClick = { picker = "fit" })
            AsterPreferenceDivider()
            AsterPreferenceSwitch("封面单页", checked = selectedCover,
                onCheckedChange = { if (defaults) model.setSmartSpreadStitch(it) else onCover(it) })
            if (!defaults && selectedMode == ReaderLayoutMode.DUAL_PAGE) {
                AsterPreferenceDivider()
                AsterPreferenceSwitch("从右向左", checked = rightToLeft, onCheckedChange = onRightToLeft)
            }
        }
        AsterPreferenceGroup("预加载") {
            AsterPreferenceSlider("向上预加载", "${state.preloadBefore} 页", state.preloadBefore.toFloat(),
                0f..16f, 15) { model.setPreloadBefore(it.roundToInt()) }
            AsterPreferenceDivider()
            AsterPreferenceSlider("向下预加载", "${state.preloadAfter} 页", state.preloadAfter.toFloat(),
                0f..16f, 15) { model.setPreloadAfter(it.roundToInt()) }
        }
        AsterPreferenceGroup("屏幕") {
            AsterPreferenceSlider("亮度", "${(state.readerBrightness * 100).roundToInt()}%", state.readerBrightness,
                0.2f..1f, onChange = model::setReaderBrightness)
            AsterPreferenceDivider()
            AsterPreferenceSwitch("状态条", checked = showStatus, onCheckedChange = onShowStatus)
        }
        AsterPreferenceGroup("操作") {
            AsterPreferenceSwitch("音量键翻页", checked = state.volumeKeyPageTurn, onCheckedChange = model::setVolumeKeyPageTurn)
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
                        ListItem(modifier = Modifier.fillMaxWidth().selectable(index == selected, role = Role.RadioButton,
                            onClick = {
                                if (choosingMode) {
                                    if (defaults) model.setReaderLayoutMode(ReaderLayoutMode.entries[index])
                                    else onMode(ReaderLayoutMode.entries[index])
                                } else {
                                    if (defaults) model.setFitModeLabel(choice.label) else onFit(choice.label)
                                }
                                picker = null
                            }).heightIn(min = AsterPreferenceLayout.RowHeight)
                            .semantics { contentDescription = choice.description ?: choice.label },
                            headlineContent = { Text(choice.label, style = MaterialTheme.typography.bodyLarge) },
                            leadingContent = { Icon(choice.icon, null, Modifier.size(AsterPreferenceLayout.IconSize)) },
                            trailingContent = { RadioButton(index == selected, onClick = null) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh))
                    }
                }
            }, confirmButton = { TextButton(onClick = { picker = null }) { Text("取消") } })
    }
}
