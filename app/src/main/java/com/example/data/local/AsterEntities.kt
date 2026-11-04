package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val series: String,
    val format: String, // "CBZ", "EPUB", "PDF", "MOBI"
    val coverUrl: String,
    val altCoverUrl: String = "",
    val currentPage: Int,
    val totalPages: Int,
    val progressPercent: Int, // 0..100
    val currentChapterTitle: String,
    val statusBadge: String, // "未读", "在读", "已读完"
    val seriesBadge: String, // e.g., "连载中 · 卷 11"
    val fileSizeText: String,
    val filePath: String,
    val repositoryName: String, // "内部存储 / Books", "SD 扩展卡 / Comics", "备用仓库"
    val updatedTimeText: String,
    val synopsis: String,
    val tagsCsv: String, // e.g. "奇幻,冒险,日漫"
    val shelfIdsCsv: String, // e.g. "shelf_fantasy,shelf_favorites"
    val isFavorite: Boolean = false,
    val isVaultProtected: Boolean = false,
    val isVaultEncrypted: Boolean = false,
    val lastReadTimestamp: Long = 0L
)

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val chapterNumber: Int,
    val title: String,
    val startPage: Int,
    val endPage: Int,
    val isCompleted: Boolean,
    val isCurrent: Boolean,
    val textContent: String // Rich text for EPUB/TXT/PDF reading & in-book search
)

@Entity(tableName = "shelves")
data class ShelfEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subtitle: String,
    val itemCountDisplay: Int,
    val updatedLabel: String,
    val coverUrlsCsv: String,
    val isProtected: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(tableName = "annotations")
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: String,
    val chapterTitle: String,
    val pageNumber: Int,
    val quoteText: String,
    val noteText: String,
    val colorName: String, // "YELLOW", "GREEN", "BLUE", "PINK"
    val type: String, // "BOOKMARK", "HIGHLIGHT", "NOTE"
    val createdAtText: String
)

@Entity(tableName = "repositories")
data class RepositoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val pathUri: String,
    val storageType: String, // "SMARTPHONE", "SD_CARD", "CLOUD"
    val bookCount: Int,
    val sizeText: String,
    val lastScanText: String,
    val scanRule: String,
    val isHealthy: Boolean = true
)
