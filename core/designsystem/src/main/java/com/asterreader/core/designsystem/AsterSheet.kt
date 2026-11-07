package com.asterreader.core.designsystem

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

@OptIn(ExperimentalMaterial3Api::class)
class AsterSheetState internal constructor(internal val value: SheetState)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberAsterSheetState(skipPartiallyExpanded: Boolean = false): AsterSheetState {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded)
    return remember(state) { AsterSheetState(state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsterModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: AsterSheetState = rememberAsterSheetState(),
    shape: Shape = MaterialTheme.shapes.extraLarge,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    showDragHandle: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest, modifier = modifier,
        sheetState = sheetState.value, shape = shape, containerColor = containerColor,
        dragHandle = if (showDragHandle) ({ BottomSheetDefaults.DragHandle() }) else null,
        content = content)
}
