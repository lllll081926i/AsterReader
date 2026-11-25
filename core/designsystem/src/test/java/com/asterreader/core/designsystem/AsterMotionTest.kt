package com.asterreader.core.designsystem

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AsterMotionTest {
    @Test
    fun reducedMotionDegradesAllNavigationTransitionsToNone() {
        val none = EnterTransition.None to ExitTransition.None

        listOf(
            AsterMotion.tabTransition(reduceMotion = true, direction = 1),
            AsterMotion.tabTransition(reduceMotion = true, direction = -1),
            AsterMotion.pageTransition(reduceMotion = true, forward = true),
            AsterMotion.pageTransition(reduceMotion = true, forward = false),
            AsterMotion.predictivePopTransition(reduceMotion = true, fromLeftEdge = true),
            AsterMotion.readerEnterTransition(reduceMotion = true),
            AsterMotion.readerExitTransition(reduceMotion = true),
            AsterMotion.authUnlockTransition(reduceMotion = true),
            AsterMotion.sheetTransition(reduceMotion = true)
        ).forEach { transform ->
            assertEquals(none.first, transform.targetContentEnter)
            assertEquals(none.second, transform.initialContentExit)
        }
    }

    @Test
    fun fullMotionProducesDistinctActiveTransitions() {
        val forward = AsterMotion.pageTransition(reduceMotion = false, forward = true)
        val backward = AsterMotion.pageTransition(reduceMotion = false, forward = false)
        assertNotEquals(EnterTransition.None, forward.targetContentEnter)
        assertNotEquals(ExitTransition.None, forward.initialContentExit)
        assertNotEquals(EnterTransition.None, backward.targetContentEnter)
        assertNotEquals(ExitTransition.None, backward.initialContentExit)

        val tabNext = AsterMotion.tabTransition(reduceMotion = false, direction = 1)
        val tabPrev = AsterMotion.tabTransition(reduceMotion = false, direction = -1)
        assertNotEquals(EnterTransition.None, tabNext.targetContentEnter)
        assertNotEquals(ExitTransition.None, tabNext.initialContentExit)
        assertNotEquals(tabNext.targetContentEnter, tabPrev.targetContentEnter)
        assertNotEquals(tabNext.initialContentExit, tabPrev.initialContentExit)

        val predictive = AsterMotion.predictivePopTransition(reduceMotion = false, fromLeftEdge = true)
        assertNotEquals(EnterTransition.None, predictive.targetContentEnter)
        assertNotEquals(ExitTransition.None, predictive.initialContentExit)
    }

    @Test
    fun systemReduceMotionChecksAnimatorScaleAndGlobalToggle() {
        assertEquals(false, isSystemReduceMotionEnabled(animatorScale = 1f, animatorsEnabled = true))
        assertEquals(false, isSystemReduceMotionEnabled(animatorScale = 0.5f, animatorsEnabled = true))
        assertEquals(true, isSystemReduceMotionEnabled(animatorScale = 0f, animatorsEnabled = true))
        assertEquals(true, isSystemReduceMotionEnabled(animatorScale = 1f, animatorsEnabled = false))
    }
}
