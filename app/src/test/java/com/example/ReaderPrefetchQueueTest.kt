package com.example

import com.example.data.local.ReaderPrefetchQueue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReaderPrefetchQueueTest {
    @Test fun visiblePageChangeReachesTheLoaderEvenWhenNeighborsStayTheSame() = runTest {
        val visiblePages = mutableListOf<Int?>()
        lateinit var queue: ReaderPrefetchQueue
        queue = ReaderPrefetchQueue(this) { _, plan ->
            visiblePages.add(plan.visiblePage())
            if (visiblePages.size == 1) queue.update("作品", listOf(2), 3)
        }
        queue.update("作品", listOf(2), 1)
        runCurrent()
        assertEquals(listOf(1, 3), visiblePages)
        queue.cancel()
    }

    @Test fun movingOntoTheLoadingPageKeepsItsDecodeAndUsesTheLatestNeighbors() = runTest {
        val release = CompletableDeferred<Unit>()
        val decoded = mutableListOf<Int>()
        val cached = mutableSetOf<Int>()
        var cancelled = false
        val queue = ReaderPrefetchQueue(this) { _, plan ->
            val attempted = mutableSetOf<Int>()
            while (true) {
                val page = plan.nextPage(attempted) ?: break
                attempted.add(page)
                try {
                    if (page == 2 && page !in cached) {
                        try { release.await() } finally { if (!release.isCompleted) cancelled = true }
                    }
                    if (cached.add(page)) decoded.add(page)
                } finally { plan.finishPage(page) }
            }
        }
        queue.update("作品", listOf(2, 3, 4), 1)
        runCurrent()
        queue.update("作品", listOf(5, 3), 2)
        runCurrent()
        assertFalse(cancelled)
        release.complete(Unit)
        runCurrent()
        assertEquals(listOf(2, 5, 3), decoded)
        queue.cancel()
    }

    @Test fun distantJumpCancelsTheOldDecodeAndStartsOnlyTheNewWindow() = runTest {
        val started = mutableListOf<Int>()
        val cancelled = mutableListOf<Int>()
        val queue = ReaderPrefetchQueue(this) { _, plan ->
            val page = plan.nextPage(emptySet())!!
            started.add(page)
            try { awaitCancellation() } finally { cancelled.add(page); plan.finishPage(page) }
        }
        queue.update("作品", listOf(2, 3), 1)
        runCurrent()
        queue.update("作品", listOf(8001, 7999), 8000)
        runCurrent()
        assertEquals(listOf(2), cancelled)
        assertEquals(listOf(2, 8001), started)
        queue.cancel()
        runCurrent()
        assertEquals(listOf(2, 8001), cancelled)
    }

    @Test fun changingBooksCancelsEvenWhenThePageNumberMatches() = runTest {
        val cancelled = CompletableDeferred<Unit>()
        val started = mutableListOf<String>()
        val queue = ReaderPrefetchQueue(this) { id, plan ->
            val page = plan.nextPage(emptySet())!!
            started.add(id)
            try { awaitCancellation() } finally { if (id == "旧书") cancelled.complete(Unit); plan.finishPage(page) }
        }
        queue.update("旧书", listOf(2), 1)
        runCurrent()
        queue.update("新书", listOf(2), 1)
        runCurrent()
        assertTrue(cancelled.isCompleted)
        assertEquals(listOf("旧书", "新书"), started)
        queue.cancel()
    }

    @Test fun aWindowUpdateAtBatchCompletionIsNotLost() = runTest {
        val decoded = mutableListOf<Int>()
        lateinit var queue: ReaderPrefetchQueue
        queue = ReaderPrefetchQueue(this) { _, plan ->
            val page = plan.nextPage(emptySet())!!
            decoded.add(page)
            plan.finishPage(page)
            if (page == 2) queue.update("作品", listOf(3), 2)
        }
        queue.update("作品", listOf(2), 1)
        runCurrent()
        assertEquals(listOf(2, 3), decoded)
        queue.cancel()
    }
}
