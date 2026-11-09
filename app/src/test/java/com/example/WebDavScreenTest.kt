package com.example

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.ui.WebDavViewModel
import com.example.ui.screens.WebDavScreen
import com.example.ui.theme.AsterReaderTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 35], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WebDavScreenTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    private val model by lazy { WebDavViewModel(ApplicationProvider.getApplicationContext<Application>()).also { store.put("dav", it) } }
    private lateinit var root: android.view.View
    @After fun close() { store.clear() }

    private fun content(dark: Boolean = false, scale: Float = 1f) {
        compose.setContent {
            root = LocalView.current.rootView
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                AsterReaderTheme(darkTheme = dark) { WebDavScreen(onBack = {}, viewModel = model) }
            }
        }
        compose.waitUntil(10000) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            !model.state.value.busy
        }
    }

    @Test fun emptyConnectionsOfferWorkingAddAction() {
        content()
        compose.onNodeWithText("添加连接").performClick()
        compose.onNodeWithText("WebDAV 地址").assertIsDisplayed()
        compose.onNodeWithText("测试").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("保存").performScrollTo().assertIsNotEnabled()
        capture("editor-light")
    }

    @Test fun invalidAddressShowsRecoverableErrorWithoutSavingConnection() {
        content()
        compose.onNodeWithText("添加连接").performClick()
        compose.onNode(hasSetTextAction() and hasText("名称")).performTextInput("云端连接")
        compose.onNode(hasSetTextAction() and hasText("WebDAV 地址")).performTextReplacement("invalid-address")
        compose.onNodeWithText("测试").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(10000) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            model.state.value.error != null
        }
        compose.onNodeWithText("请输入有效的 WebDAV 地址").assertIsDisplayed()
        compose.onNode(hasSetTextAction() and hasText("名称")).assertTextContains("云端连接")
        compose.runOnIdle { check(model.state.value.connections.isEmpty()) }
        capture("error-light")
    }

    @Test fun darkLargeFontKeepsHttpWarningAndActionsReachable() {
        content(dark = true, scale = 2f)
        compose.onNodeWithText("添加连接").performClick()
        compose.onNodeWithText("允许 HTTP").performScrollTo().performClick()
        compose.onNodeWithText("HTTP 以明文传输账号和内容。").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("测试").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("保存").performScrollTo().assertIsDisplayed()
        capture("editor-dark-font-200")
    }

    @Test fun serviceSelectionFillsNutstoreAndClearsCredentialsOnSwitch() {
        content()
        compose.onNodeWithText("添加连接").performClick()
        compose.onNodeWithText("服务商").performClick()
        compose.onNodeWithText("坚果云").performClick()
        compose.onNode(hasSetTextAction() and hasText("WebDAV 地址")).assertTextContains("https://dav.jianguoyun.com/dav/")
        compose.onNode(hasSetTextAction() and hasText("邮箱")).performScrollTo().performTextInput("reader@example.com")
        compose.onNode(hasSetTextAction() and hasText("应用密码")).performScrollTo().performTextInput("temporary-test-credential")
        capture("nutstore-light")
        compose.onNodeWithText("服务商").performScrollTo().performClick()
        compose.onNodeWithText("Nextcloud").performClick()
        compose.onNode(hasSetTextAction() and hasText("WebDAV 地址")).assertTextContains("https://")
        compose.onNode(hasSetTextAction() and hasText("用户名")).assertTextContains("")
        compose.onNode(hasSetTextAction() and hasText("应用密码")).performScrollTo().assertTextEquals("应用密码", "")
        compose.runOnIdle { check(model.state.value.connections.isEmpty()) }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        compose.runOnIdle {
            val directory = File("build/ui-review").apply { mkdirs() }
            val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            root.draw(android.graphics.Canvas(bitmap))
            File(directory, "webdav-$name-api${android.os.Build.VERSION.SDK_INT}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
