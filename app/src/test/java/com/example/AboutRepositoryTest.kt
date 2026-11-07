package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AboutRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AboutRepositoryTest {
    @Test fun bundledNoticesCoverArchiveEnginesAndFontsWithoutDuplicateIds() = runBlocking {
        val entries = AboutRepository(ApplicationProvider.getApplicationContext<Application>()).notices()
        assertEquals(entries.size, entries.map { it.id }.distinct().size)
        assertTrue(entries.any { it.name == "org.apache.commons:commons-compress" })
        assertTrue(entries.any { it.name == "com.github.junrar:junrar" })
        assertTrue(entries.any { it.name == "Lora" && it.notice.contains("SIL OPEN FONT LICENSE") })
        assertTrue(entries.any { it.name == "Roboto Mono" && it.notice.contains("SIL OPEN FONT LICENSE") })
        assertTrue(entries.all { it.name.isNotBlank() && it.version.isNotBlank() && it.license.isNotBlank() })
        assertTrue(entries.all { it.url.isEmpty() || it.url.startsWith("https://") })
    }
}
