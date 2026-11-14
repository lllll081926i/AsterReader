package com.example.data.local

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 在主线程接收阅读窗口；窗口变化时保留仍有用的在途解码。 */
internal class ReaderPrefetchQueue(
    private val scope: CoroutineScope,
    private val load: suspend (String, ReaderPrefetchPlan) -> Unit
) {
    private var bookId: String? = null
    private var plan: ReaderPrefetchPlan? = null
    private var job: Job? = null

    fun update(id: String, pages: List<Int>, visiblePage: Int) {
        val current = plan
        if (bookId == id && current != null && job?.isActive == true && current.update(pages, visiblePage)) return
        cancel()
        val next = ReaderPrefetchPlan(pages)
        bookId = id
        plan = next
        job = scope.launch {
            try {
                do {
                    val revision = next.revision()
                    load(id, next)
                    // 解码返回与窗口更新可能同时发生，退出前处理最后一次更新。
                } while (next.revision() != revision)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // 可见页负责显示读取错误。
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        plan = null
        bookId = null
    }
}

/** 解码线程与主线程之间只交换页码，不持有位图或文件资源。 */
internal class ReaderPrefetchPlan(pages: List<Int>) {
    private var pages = pages.filter { it > 0 }.distinct()
    private var activePage: Int? = null
    private var revision = 0L

    @Synchronized fun revision(): Long = revision

    @Synchronized fun update(nextPages: List<Int>, visiblePage: Int): Boolean {
        val next = nextPages.filter { it > 0 }.distinct()
        if (next != pages) {
            pages = next
            revision++
        }
        return activePage == null || activePage == visiblePage || activePage in pages
    }

    @Synchronized fun nextPage(attempted: Set<Int>): Int? =
        pages.firstOrNull { it !in attempted }.also { activePage = it }

    @Synchronized fun finishPage(page: Int) {
        if (activePage == page) activePage = null
    }
}
