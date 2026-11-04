package com.asterreader.core.database

import com.asterreader.core.database.BookEntity

fun comicFixture(id: String = "测试漫画") = BookEntity(
    id = id, title = "测试漫画", author = "作者", series = "系列", format = "CBZ", coverUrl = "",
    currentPage = 1, totalPages = 100, progressPercent = 0, currentChapterTitle = "第 1 页",
    statusBadge = "未读", seriesBadge = "", fileSizeText = "1 MB", filePath = "content://test/comic.cbz",
    repositoryName = "测试目录", updatedTimeText = "现在", synopsis = "", tagsCsv = "", shelfIdsCsv = ""
)
