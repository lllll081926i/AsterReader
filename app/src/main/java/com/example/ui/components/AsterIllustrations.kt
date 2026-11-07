package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.LocalAsterColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * 1. Botanical Grimoire 2D Vector Line-Art Illustration
 * Used in Onboarding (O-01), About (T-07), and decorative headers.
 */
@Composable
fun BotanicalGrimoireIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val tertiary = MaterialTheme.colorScheme.tertiary
    val outline = MaterialTheme.colorScheme.outline

    val floatOffset = 0f

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer { translationY = floatOffset }
    ) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val strokeThin = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val strokeBold = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Subtle tonal backdrop circle
        drawCircle(
            color = secondaryContainer.copy(alpha = 0.55f),
            radius = w * 0.42f,
            center = Offset(cx, cy)
        )

        // Outer architectural orbit ring
        drawCircle(
            color = outline.copy(alpha = 0.25f),
            radius = w * 0.46f,
            center = Offset(cx, cy),
            style = strokeThin
        )

        // Open Grimoire pages path
        val bookPath = Path().apply {
            // Left page
            moveTo(cx, cy + h * 0.22f)
            cubicTo(cx - w * 0.12f, cy + h * 0.16f, cx - w * 0.26f, cy + h * 0.18f, cx - w * 0.34f, cy + h * 0.22f)
            lineTo(cx - w * 0.34f, cy - h * 0.14f)
            cubicTo(cx - w * 0.24f, cy - h * 0.18f, cx - w * 0.12f, cy - h * 0.16f, cx, cy - h * 0.08f)
            // Right page
            cubicTo(cx + w * 0.12f, cy - h * 0.16f, cx + w * 0.24f, cy - h * 0.18f, cx + w * 0.34f, cy - h * 0.14f)
            lineTo(cx + w * 0.34f, cy + h * 0.22f)
            cubicTo(cx + w * 0.26f, cy + h * 0.18f, cx + w * 0.12f, cy + h * 0.16f, cx, cy + h * 0.22f)
            close()
        }
        drawPath(path = bookPath, color = Color.White.copy(alpha = 0.65f))
        drawPath(path = bookPath, color = primary, style = strokeBold)

        // Center spine line
        drawLine(
            color = primary,
            start = Offset(cx, cy - h * 0.08f),
            end = Offset(cx, cy + h * 0.22f),
            strokeWidth = 2.4.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Delicate page text lines on left & right pages
        for (i in 0..2) {
            val yOff = cy - h * 0.04f + i * (h * 0.06f)
            drawLine(
                color = primary.copy(alpha = 0.55f),
                start = Offset(cx - w * 0.26f, yOff),
                end = Offset(cx - w * 0.08f, yOff - h * 0.015f),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = primary.copy(alpha = 0.55f),
                start = Offset(cx + w * 0.08f, yOff - h * 0.015f),
                end = Offset(cx + w * 0.26f, yOff),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Botanical sprig rising from book spine
        val stemPath = Path().apply {
            moveTo(cx, cy - h * 0.08f)
            quadraticTo(cx - w * 0.04f, cy - h * 0.24f, cx + w * 0.03f, cy - h * 0.36f)
        }
        drawPath(path = stemPath, color = primary, style = strokeThin)

        // Botanical leaves (left & right)
        val leftLeaf = Path().apply {
            moveTo(cx - w * 0.01f, cy - h * 0.18f)
            quadraticTo(cx - w * 0.16f, cy - h * 0.22f, cx - w * 0.14f, cy - h * 0.30f)
            quadraticTo(cx - w * 0.04f, cy - h * 0.26f, cx - w * 0.01f, cy - h * 0.18f)
        }
        drawPath(path = leftLeaf, color = primary.copy(alpha = 0.22f))
        drawPath(path = leftLeaf, color = primary, style = strokeThin)

        val rightLeaf = Path().apply {
            moveTo(cx + w * 0.01f, cy - h * 0.23f)
            quadraticTo(cx + w * 0.17f, cy - h * 0.26f, cx + w * 0.15f, cy - h * 0.34f)
            quadraticTo(cx + w * 0.05f, cy - h * 0.31f, cx + w * 0.01f, cy - h * 0.23f)
        }
        drawPath(path = rightLeaf, color = tertiary.copy(alpha = 0.22f))
        drawPath(path = rightLeaf, color = primary, style = strokeThin)

        // Four-point Aster star above stem
        val starCx = cx + w * 0.03f
        val starCy = cy - h * 0.40f
        val starR = w * 0.06f
        val starPath = Path().apply {
            moveTo(starCx, starCy - starR)
            quadraticTo(starCx, starCy, starCx + starR, starCy)
            quadraticTo(starCx, starCy, starCx, starCy + starR)
            quadraticTo(starCx, starCy, starCx - starR, starCy)
            quadraticTo(starCx, starCy, starCx, starCy - starR)
        }
        drawPath(path = starPath, color = primary)
    }
}

/**
 * 2. Tranquil Reading Desk 2D Line-Art Illustration
 * Used in Empty Library (L-01), Zero Search Results (L-04), Empty Shelf (S-02).
 */
@Composable
fun TranquilDeskEmptyIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 150.dp
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val outline = MaterialTheme.colorScheme.outline

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Soft arch backdrop
        val archPath = Path().apply {
            moveTo(w * 0.18f, h * 0.82f)
            lineTo(w * 0.18f, h * 0.42f)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(w * 0.18f, h * 0.12f, w * 0.82f, h * 0.72f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(w * 0.82f, h * 0.82f)
            close()
        }
        drawPath(path = archPath, color = secondaryContainer.copy(alpha = 0.45f))
        drawPath(path = archPath, color = outline.copy(alpha = 0.35f), style = stroke)

        // Desk baseline
        drawLine(
            color = primary,
            start = Offset(w * 0.10f, h * 0.82f),
            end = Offset(w * 0.90f, h * 0.82f),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Bottom horizontal book in stack
        drawRoundRect(
            color = Color.White.copy(alpha = 0.8f),
            topLeft = Offset(w * 0.24f, h * 0.70f),
            size = Size(w * 0.46f, h * 0.12f),
            cornerRadius = CornerRadius(6.dp.toPx())
        )
        drawRoundRect(
            color = primary,
            topLeft = Offset(w * 0.24f, h * 0.70f),
            size = Size(w * 0.46f, h * 0.12f),
            cornerRadius = CornerRadius(6.dp.toPx()),
            style = stroke
        )

        // Middle horizontal book in stack
        drawRoundRect(
            color = secondaryContainer,
            topLeft = Offset(w * 0.28f, h * 0.59f),
            size = Size(w * 0.40f, h * 0.11f),
            cornerRadius = CornerRadius(5.dp.toPx())
        )
        drawRoundRect(
            color = primary,
            topLeft = Offset(w * 0.28f, h * 0.59f),
            size = Size(w * 0.40f, h * 0.11f),
            cornerRadius = CornerRadius(5.dp.toPx()),
            style = stroke
        )

        // Bookmark ribbon hanging from middle book
        val ribbon = Path().apply {
            moveTo(w * 0.56f, h * 0.65f)
            lineTo(w * 0.56f, h * 0.77f)
            lineTo(w * 0.59f, h * 0.74f)
            lineTo(w * 0.62f, h * 0.77f)
            lineTo(w * 0.62f, h * 0.65f)
        }
        drawPath(path = ribbon, color = primary, style = stroke)

        // Minimalist reading lamp arc from right
        val lampArc = Path().apply {
            moveTo(w * 0.76f, h * 0.82f)
            lineTo(w * 0.76f, h * 0.40f)
            quadraticTo(w * 0.76f, h * 0.24f, w * 0.56f, h * 0.26f)
        }
        drawPath(path = lampArc, color = primary, style = stroke)

        // Lamp shade cone
        val shade = Path().apply {
            moveTo(w * 0.56f, h * 0.26f)
            lineTo(w * 0.46f, h * 0.36f)
            lineTo(w * 0.62f, h * 0.36f)
            close()
        }
        drawPath(path = shade, color = primary.copy(alpha = 0.2f))
        drawPath(path = shade, color = primary, style = stroke)
    }
}

/**
 * 3. Vault Geometric Emblem & Line-Art Shield
 * Used in AuthGate (V-01), Vault Setup (V-03), and Vault Overview (V-06).
 */
@Composable
fun VaultGeometricShieldIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val driftY = 0f

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer { translationY = driftY }
    ) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Outer precision ring with 8 tick marks
        drawCircle(
            color = outlineVariant.copy(alpha = 0.6f),
            radius = w * 0.46f,
            center = Offset(cx, cy),
            style = stroke
        )
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            val r1 = w * 0.41f
            val r2 = w * 0.46f
            drawLine(
                color = primary.copy(alpha = 0.45f),
                start = Offset(cx + (r1 * cos(angle)).toFloat(), cy + (r1 * sin(angle)).toFloat()),
                end = Offset(cx + (r2 * cos(angle)).toFloat(), cy + (r2 * sin(angle)).toFloat()),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Inner filled container circle
        drawCircle(
            color = primaryContainer,
            radius = w * 0.34f,
            center = Offset(cx, cy)
        )

        // Shield contour inside
        val shield = Path().apply {
            moveTo(cx, cy - h * 0.18f)
            lineTo(cx + w * 0.15f, cy - h * 0.11f)
            lineTo(cx + w * 0.15f, cy + h * 0.02f)
            quadraticTo(cx + w * 0.15f, cy + h * 0.15f, cx, cy + h * 0.21f)
            quadraticTo(cx - w * 0.15f, cy + h * 0.15f, cx - w * 0.15f, cy + h * 0.02f)
            lineTo(cx - w * 0.15f, cy - h * 0.11f)
            close()
        }
        drawPath(
            path = shield,
            color = onPrimaryContainer,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Keyhole circle + stem
        drawCircle(
            color = onPrimaryContainer,
            radius = w * 0.035f,
            center = Offset(cx, cy - h * 0.01f)
        )
        drawLine(
            color = onPrimaryContainer,
            start = Offset(cx, cy + h * 0.01f),
            end = Offset(cx, cy + h * 0.09f),
            strokeWidth = 2.4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/**
 * 4. Smart Book Cover Component:
 * Hotlinks images from the HTML prototypes, and provides a bespoke 2D vector botanical/architectural
 * book cover fallback for offline mode or custom imported books.
 */
@Composable
fun AsterBookCover(
    coverUrl: String,
    title: String,
    format: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    if (coverUrl.isNotBlank()) {
        // 兼容历史数据：裸文件路径补全 file:// 前缀，否则 Coil 无法识别
        val model = if (coverUrl.contains("://")) coverUrl else "file://$coverUrl"
        val protectedCover = coverUrl.startsWith("vault-cover://")
        val request = remember(context, model, protectedCover) { ImageRequest.Builder(context)
                .data(model)
                .crossfade(false)
                .diskCachePolicy(if (protectedCover) coil.request.CachePolicy.DISABLED else coil.request.CachePolicy.ENABLED)
                .memoryCachePolicy(if (protectedCover) coil.request.CachePolicy.DISABLED else coil.request.CachePolicy.ENABLED)
                .build() }
        var hasImage by remember(model) { mutableStateOf(false) }
        Box(modifier) {
            if (!hasImage) VectorBookCoverFallback(title = title, format = format, modifier = Modifier.fillMaxSize())
            AsyncImage(model = request, contentDescription = title, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(), onLoading = { hasImage = false },
                onError = { hasImage = false }, onSuccess = { hasImage = true })
        }
    } else {
        VectorBookCoverFallback(title = title, format = format, modifier = modifier)
    }
}

@Composable
fun VectorBookCoverFallback(
    title: String,
    format: String,
    modifier: Modifier = Modifier
) {
    val asterColors = LocalAsterColors.current
    val primary = MaterialTheme.colorScheme.primary
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        asterColors.surfaceContainerHigh,
                        secondaryContainer.copy(alpha = 0.65f),
                        asterColors.surfaceContainer
                    )
                )
            )
    ) {
        // Minimalist geometric line-art decoration on the cover
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val stroke = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)

            // Editorial frame inset
            drawRoundRect(
                color = primary.copy(alpha = 0.28f),
                topLeft = Offset(w * 0.08f, h * 0.06f),
                size = Size(w * 0.84f, h * 0.88f),
                cornerRadius = CornerRadius(6.dp.toPx()),
                style = stroke
            )

            // Botanical arch & celestial circle
            drawCircle(
                color = primary.copy(alpha = 0.16f),
                radius = w * 0.26f,
                center = Offset(w * 0.5f, h * 0.42f)
            )
            drawCircle(
                color = primary.copy(alpha = 0.38f),
                radius = w * 0.26f,
                center = Offset(w * 0.5f, h * 0.42f),
                style = stroke
            )

            // Diagonal delicate botanical lines
            drawLine(
                color = primary.copy(alpha = 0.35f),
                start = Offset(w * 0.25f, h * 0.68f),
                end = Offset(w * 0.75f, h * 0.22f),
                strokeWidth = 1.2.dp.toPx()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = format,
                style = MaterialTheme.typography.labelSmall,
                color = outline,
                modifier = Modifier.align(Alignment.End)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Text(
                text = "ASTER EDITION",
                style = MaterialTheme.typography.labelSmall,
                color = primary.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 5. Manga Page Artwork with Offline Line-Art Manga Panel Fallback
 * Ensures both hotlinked manga pages and procedural pages (1..121) look like authentic manga spreads.
 */
@Composable
fun MangaPageCanvas(
    imageUrl: String,
    pageNumber: Int,
    chapterTitle: String,
    dialogueLine: String = "",
    isDarkCanvas: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(modifier = modifier) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = chapterTitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                OfflineMangaPanelsVector(
                    pageNumber = pageNumber,
                    chapterTitle = chapterTitle,
                    isDarkCanvas = isDarkCanvas,
                    modifier = Modifier.fillMaxSize()
                )
            },
            error = {
                OfflineMangaPanelsVector(
                    pageNumber = pageNumber,
                    chapterTitle = chapterTitle,
                    isDarkCanvas = isDarkCanvas,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    }
}

@Composable
private fun OfflineMangaPanelsVector(
    pageNumber: Int,
    chapterTitle: String,
    isDarkCanvas: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isDarkCanvas) Color(0xFF1A1C1B) else Color(0xFFFBF9F5)
    val inkColor = if (isDarkCanvas) Color(0xFFE1E3DF) else Color(0xFF191C1B)

    Canvas(modifier = modifier.background(bgColor)) {
        val w = size.width
        val h = size.height
        val pad = 18.dp.toPx()
        val strokePanel = Stroke(width = 2.dp.toPx())
        val strokeFine = Stroke(width = 1.dp.toPx())

        // Top Panel (Cinematic wide landscape panel)
        val topH = h * 0.32f
        drawRect(
            color = inkColor.copy(alpha = 0.8f),
            topLeft = Offset(pad, pad),
            size = Size(w - pad * 2, topH),
            style = strokePanel
        )
        // Mountain/forest silhouette lines inside top panel
        val mountainPath = Path().apply {
            moveTo(pad, pad + topH * 0.78f)
            lineTo(pad + w * 0.25f, pad + topH * 0.35f)
            lineTo(pad + w * 0.48f, pad + topH * 0.62f)
            lineTo(pad + w * 0.72f, pad + topH * 0.25f)
            lineTo(w - pad, pad + topH * 0.70f)
        }
        drawPath(path = mountainPath, color = inkColor.copy(alpha = 0.55f), style = strokePanel)

        // Middle Asymmetric Diagonal Panels
        val midTop = pad * 1.8f + topH
        val midH = h * 0.34f
        val splitX = w * (if (pageNumber % 2 == 0) 0.56f else 0.44f)

        drawRect(
            color = inkColor.copy(alpha = 0.8f),
            topLeft = Offset(pad, midTop),
            size = Size(splitX - pad * 1.4f, midH),
            style = strokePanel
        )
        drawRect(
            color = inkColor.copy(alpha = 0.8f),
            topLeft = Offset(splitX, midTop),
            size = Size(w - splitX - pad, midH),
            style = strokePanel
        )

        // Concentric magic circle / speed lines inside middle right panel
        val mCx = splitX + (w - splitX - pad) / 2f
        val mCy = midTop + midH / 2f
        drawCircle(
            color = inkColor.copy(alpha = 0.4f),
            radius = (w - splitX - pad) * 0.34f,
            center = Offset(mCx, mCy),
            style = strokeFine
        )
        drawCircle(
            color = inkColor.copy(alpha = 0.25f),
            radius = (w - splitX - pad) * 0.20f,
            center = Offset(mCx, mCy),
            style = strokeFine
        )

        // Bottom Wide Panel
        val botTop = midTop + midH + pad * 0.8f
        val botH = (h - botTop - pad).coerceAtLeast(40f)
        drawRect(
            color = inkColor.copy(alpha = 0.8f),
            topLeft = Offset(pad, botTop),
            size = Size(w - pad * 2, botH),
            style = strokePanel
        )
    }
}
