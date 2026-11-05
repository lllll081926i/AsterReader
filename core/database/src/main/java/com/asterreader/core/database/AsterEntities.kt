package com.asterreader.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index

@Entity(tableName = "books", indices = [Index("filePath"), Index("lastReadTimestamp"), Index("addedAt"), Index("repositoryId")])
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val series: String,
    val format: String, // 漫画容器格式。
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
    val lastReadTimestamp: Long = 0L,
    @ColumnInfo(defaultValue = "0") val sizeBytes: Long = 0L,
    @ColumnInfo(defaultValue = "0") val addedAt: Long = 0L,
    @ColumnInfo(defaultValue = "0") val sourceModifiedAt: Long = 0L,
    val repositoryId: String = "",
    val sourceStatus: String = "AVAILABLE",
    val contentFingerprint: String = "",
    val coverPage: Int = 1,
    val seriesOrder: Int = 0
)

@Entity(tableName = "chapters", indices = [Index(value = ["bookId", "chapterNumber"])])
data class ChapterEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val chapterNumber: Int,
    val title: String,
    val startPage: Int,
    val endPage: Int,
    val isCompleted: Boolean,
    val isCurrent: Boolean
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

@Entity(tableName = "annotations", indices = [Index(value = ["bookId", "pageNumber", "type"])])
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

@Entity(tableName = "reader_states", foreignKeys = [androidx.room.ForeignKey(
    entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = androidx.room.ForeignKey.CASCADE)])
data class ReaderStateEntity(
    @PrimaryKey val bookId: String,
    val page: Int = 1,
    val offsetFraction: Float = 0f,
    val scale: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val layoutMode: String? = null,
    val fitMode: String? = null,
    val coverSingle: Boolean? = null,
    val rightToLeft: Boolean? = null
)

@Entity(tableName = "comic_pages", primaryKeys = ["bookId", "number"], foreignKeys = [androidx.room.ForeignKey(
    entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = androidx.room.ForeignKey.CASCADE)])
data class ComicPageEntity(val bookId: String, val number: Int, val uri: String, val name: String, val sizeBytes: Long, val modifiedAt: Long)
