package com.mitra.engine.access

import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.mitra.engine.Provenance
import com.mitra.engine.Rect
import com.mitra.engine.ScreenElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NodeMapperTest {
    private class FakeNode(
        override val className: CharSequence? = "android.widget.TextView",
        override val text: CharSequence? = null,
        override val contentDescription: CharSequence? = null,
        override val viewIdResourceName: String? = null,
        override val packageName: CharSequence? = "com.example.app",
        override val isClickable: Boolean = false,
        override val isEditable: Boolean = false,
        override val isPassword: Boolean = false,
        val children: List<FakeNode> = emptyList(),
        override val bounds: Rect = Rect(0, 0, 100, 100),
    ) : AccessibleNode {
        override val childCount: Int get() = children.size

        override fun getChild(index: Int): AccessibleNode? = children.getOrNull(index)

        override val rawNode: AccessibilityNodeInfo? get() = null
    }

    private class FakeWindow(
        override val type: Int,
        override val root: AccessibleNode?,
    ) : AccessibleWindow

    @Test
    fun `password field hides text and sets secure flag true`() {
        val root =
            FakeNode(
                className = "android.widget.EditText",
                text = "SuperSecretPassword123",
                isPassword = true,
                isEditable = true,
            )
        val window = FakeWindow(AccessibilityWindowInfo.TYPE_APPLICATION, root)
        val mapper = NodeMapper(targetPackage = "com.example.app")
        val out = mutableListOf<ScreenElement>()

        mapper.map(window, out)

        assertEquals(1, out.size)
        val elem = out[0]
        assertTrue(elem.secure)
        assertNull(elem.text, "Password text must be redacted (null)")
        assertEquals("EditText", elem.role)
    }

    @Test
    fun `normal field retains text and sets secure flag false`() {
        val root =
            FakeNode(
                className = "android.widget.TextView",
                text = "Welcome to Mitra",
                isPassword = false,
            )
        val window = FakeWindow(AccessibilityWindowInfo.TYPE_APPLICATION, root)
        val mapper = NodeMapper(targetPackage = "com.example.app")
        val out = mutableListOf<ScreenElement>()

        mapper.map(window, out)

        assertEquals(1, out.size)
        val elem = out[0]
        assertFalse(elem.secure)
        assertEquals("Welcome to Mitra", elem.text)
        assertEquals("TextView", elem.role)
    }

    @Test
    fun `application layout tags provenance APP_LAYOUT`() {
        val root =
            FakeNode(
                packageName = "com.example.app",
                className = "android.widget.Button",
                text = "Submit",
            )
        val window = FakeWindow(AccessibilityWindowInfo.TYPE_APPLICATION, root)
        val mapper = NodeMapper(targetPackage = "com.example.app")
        val out = mutableListOf<ScreenElement>()

        mapper.map(window, out)

        assertEquals(1, out.size)
        assertEquals(Provenance.APP_LAYOUT, out[0].provenance)
    }

    @Test
    fun `system window tags provenance SYSTEM_UI`() {
        val root =
            FakeNode(
                packageName = "com.android.systemui",
                className = "android.widget.FrameLayout",
            )
        val window = FakeWindow(AccessibilityWindowInfo.TYPE_SYSTEM, root)
        val mapper = NodeMapper(targetPackage = "com.example.app")
        val out = mutableListOf<ScreenElement>()

        mapper.map(window, out)

        assertEquals(1, out.size)
        assertEquals(Provenance.SYSTEM_UI, out[0].provenance)
    }

    @Test
    fun `input method window tags provenance SYSTEM_UI`() {
        val root =
            FakeNode(
                packageName = "com.google.android.inputmethod.latin",
                className = "android.inputmethodservice.SoftInputWindow",
            )
        val window = FakeWindow(AccessibilityWindowInfo.TYPE_INPUT_METHOD, root)
        val mapper = NodeMapper(targetPackage = "com.example.app")
        val out = mutableListOf<ScreenElement>()

        mapper.map(window, out)

        assertEquals(1, out.size)
        assertEquals(Provenance.SYSTEM_UI, out[0].provenance)
    }

    @Test
    fun `foreign package window tags provenance OVERLAY`() {
        val root =
            FakeNode(
                packageName = "com.malicious.overlay",
                className = "android.view.View",
            )
        val window = FakeWindow(AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY, root)
        val mapper = NodeMapper(targetPackage = "com.example.app")
        val out = mutableListOf<ScreenElement>()

        mapper.map(window, out)

        assertEquals(1, out.size)
        assertEquals(Provenance.OVERLAY, out[0].provenance)
    }

    @Test
    fun `webView and its children are tagged WEB_CONTENT`() {
        val linkNode =
            FakeNode(
                className = "android.view.View",
                text = "Click here for offer",
            )
        val webViewNode =
            FakeNode(
                className = "android.webkit.WebView",
                children = listOf(linkNode),
            )
        val window = FakeWindow(AccessibilityWindowInfo.TYPE_APPLICATION, webViewNode)
        val mapper = NodeMapper(targetPackage = "com.example.app")
        val out = mutableListOf<ScreenElement>()

        mapper.map(window, out)

        assertEquals(2, out.size)
        assertEquals(Provenance.WEB_CONTENT, out[0].provenance)
        assertEquals(Provenance.WEB_CONTENT, out[1].provenance)
    }

    @Test
    fun `user content container and its children are tagged USER_CONTENT`() {
        val messageText =
            FakeNode(
                className = "android.widget.TextView",
                text = "Hey there, this is a user message",
            )
        val messageContainer =
            FakeNode(
                className = "android.widget.LinearLayout",
                viewIdResourceName = "com.example.app:id/chat_message_container",
                children = listOf(messageText),
            )
        val window = FakeWindow(AccessibilityWindowInfo.TYPE_APPLICATION, messageContainer)
        val mapper =
            NodeMapper(
                targetPackage = "com.example.app",
                userContentViewIds = setOf("com.example.app:id/chat_message_container"),
            )
        val out = mutableListOf<ScreenElement>()

        mapper.map(window, out)

        assertEquals(2, out.size)
        assertEquals(Provenance.USER_CONTENT, out[0].provenance)
        assertEquals(Provenance.USER_CONTENT, out[1].provenance)
    }
}
