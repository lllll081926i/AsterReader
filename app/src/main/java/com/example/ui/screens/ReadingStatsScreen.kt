package com.example.ui.screens

import com.example.ui.components.AsterSubScreenHeader

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.ReadingStatsStore
import com.example.ui.AsterViewModel
import com.example.ui.theme.LoraFontFamily
import java.time.LocalDate

/**
 * 独立统计页。只展示阅读时长与完成度，不做内容采集。
 * 关闭「记录阅读时长」后不再进入本页（入口与开关都在设置里）。
 */
@Composable
fun ReadingStatsScreen(
    viewModel: AsterViewModel,
    finishedBooks: Int,
    totalBooks: Int,
    onBack: () -> Unit
) {
    val stats by viewModel.readingStats.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshReadingStats() }
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        AsterSubScreenHeader("阅读统计", onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 今日焦点卡
            Surface(
                shape = MaterialTheme.shapes.large,
                color = colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "今日阅读",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stats.todayText,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = LoraFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("近 7 天", stats.weekText, Modifier.weight(1f))
                StatTile("累计", stats.totalHoursText, Modifier.weight(1f))
            }

            Surface(
                shape = MaterialTheme.shapes.large,
                color = colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "最近 7 天",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "共 ${stats.weekText}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    WeekBars(stats.daily)
                    if (stats.daily.sumOf { it.minutes } == 0) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "还没有阅读记录，翻开一本书开始计时吧。",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Surface(
                shape = MaterialTheme.shapes.large,
                color = colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "已读完",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$finishedBooks / $totalBooks",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (totalBooks <= 0) 0f
                            else (finishedBooks.toFloat() / totalBooks).coerceIn(0f, 1f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = colorScheme.primary,
                        trackColor = colorScheme.surfaceContainerHighest
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2
            )
        }
    }
}

/** 7 根柱子，按当周最大值归一化；带入场动画，今天高亮。 */
@Composable
private fun WeekBars(days: List<ReadingStatsStore.DailyMinutes>) {
    val peak = (days.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)
    val colorScheme = MaterialTheme.colorScheme
    val today = LocalDate.now()
    val weekday = listOf("一", "二", "三", "四", "五", "六", "日")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEachIndexed { index, day ->
            val ratio by animateFloatAsState(
                targetValue = (day.minutes.toFloat() / peak).coerceIn(0f, 1f),
                animationSpec = com.asterreader.core.designsystem.AsterMotion.effectsSpec(com.asterreader.core.designsystem.LocalAsterReduceMotion.current),
                label = "stats_bar_$index"
            )
            val barBrush = Brush.verticalGradient(
                colors = if (day.day == today) {
                    listOf(colorScheme.primary, colorScheme.primaryContainer)
                } else {
                    listOf(colorScheme.primaryContainer, colorScheme.primary.copy(alpha = 0.7f))
                }
            )
            Box(
                modifier = Modifier.weight(1f).fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(modifier = Modifier.fillMaxWidth(0.62f).fillMaxSize()) {
                    val corner = CornerRadius(size.width / 3f)
                    drawRoundRect(
                        color = colorScheme.surfaceContainerHighest,
                        topLeft = Offset.Zero,
                        size = size,
                        cornerRadius = corner
                    )
                    val barHeight = size.height * ratio
                    if (barHeight > 0f) {
                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(0f, size.height - barHeight),
                            size = Size(size.width, barHeight),
                            cornerRadius = corner
                        )
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth()) {
        days.forEach { day ->
            Text(
                text = weekday.getOrElse(day.day.dayOfWeek.value - 1) { "" },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (day.day == today) FontWeight.Bold else FontWeight.Normal,
                color = if (day.day == today) colorScheme.primary else colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
