package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.AsterNavigationLayout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 35])
class AsterNavigationLayoutTest {
    @get:Rule val compose = createComposeRule()

    private fun show(rail: Boolean, reserve: Boolean = true) {
        compose.setContent {
            Box(Modifier.size(300.dp, 400.dp)) {
                AsterNavigationLayout(reserve, rail,
                    content = { Box(Modifier.fillMaxSize().testTag("body")) },
                    navigation = { Box(Modifier.size(if (rail) 80.dp else 280.dp, if (rail) 400.dp else 96.dp)) },
                    overlay = {})
            }
        }
    }

    @Test fun bottomNavigationReservesItsActualHeightWithoutStateRoundTrip() {
        show(false)
        compose.onNodeWithTag("body").assertHeightIsEqualTo(304.dp).assertWidthIsEqualTo(300.dp)
    }

    @Test fun railReservesActualWidth() {
        show(true)
        compose.onNodeWithTag("body").assertWidthIsEqualTo(220.dp).assertHeightIsEqualTo(400.dp)
    }

    @Test fun readerUsesFullAreaWhenNavigationIsNotReserved() {
        show(false, false)
        compose.onNodeWithTag("body").assertHeightIsEqualTo(400.dp).assertWidthIsEqualTo(300.dp)
    }
}
