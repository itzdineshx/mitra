package com.mitra.trust

import com.mitra.engine.ActionResult
import com.mitra.engine.Direction
import com.mitra.engine.EngineKind
import com.mitra.engine.ExecutionEngine
import com.mitra.engine.GlobalAction
import com.mitra.engine.IntentSpec
import com.mitra.engine.Point
import com.mitra.engine.ScreenSnapshot
import com.mitra.trust.Fx.ctx
import com.mitra.trust.Fx.el
import com.mitra.trust.Fx.scope
import com.mitra.trust.Fx.tap
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TrustedExecutorTest {
    private class FakeEngine(
        var screen: ScreenSnapshot,
    ) : ExecutionEngine {
        val taps = mutableListOf<String>()
        var stopped = false
        override val kind = EngineKind.ACCESSIBILITY

        override suspend fun isAvailable() = true

        override suspend fun readScreen() = screen

        override suspend fun launchApp(packageName: String) = ActionResult.Ok()

        override suspend fun sendIntent(intent: IntentSpec) = ActionResult.Ok()

        override suspend fun tap(elementId: String): ActionResult {
            taps += elementId
            return ActionResult.Ok()
        }

        override suspend fun longPress(elementId: String) = ActionResult.Ok()

        override suspend fun type(
            elementId: String,
            text: String,
        ) = ActionResult.Ok()

        override suspend fun scroll(
            direction: Direction,
            elementId: String?,
        ) = ActionResult.Ok()

        override suspend fun swipe(
            from: Point,
            to: Point,
            durationMs: Long,
        ) = ActionResult.Ok()

        override suspend fun globalAction(action: GlobalAction) = ActionResult.Ok()

        override fun stop() {
            stopped = true
        }
    }

    private class FakeConfirmer(
        val sayYes: Boolean,
        val fingerprintOk: Boolean = true,
    ) : Confirmer {
        val asked = mutableListOf<String>()

        override suspend fun askVoice(question: String): Boolean {
            asked += question
            return sayYes
        }

        override suspend fun biometric(reason: String) = fingerprintOk

        override suspend fun speak(text: String) {}

        override suspend fun waitForUser(message: String) {}
    }

    private class MemoryAudit : AuditLog {
        val decisions = mutableListOf<Decision>()

        override suspend fun record(
            action: ProposedAction,
            decision: Decision,
        ) {
            decisions += decision
        }

        override suspend fun result(
            auditId: String,
            result: ActionResult,
        ) {}
    }

    private val send = el("send", "Send")

    private fun setup(
        yes: Boolean,
        fingerprintOk: Boolean = true,
    ): Triple<TrustedExecutor, FakeEngine, MemoryAudit> {
        val c = ctx(send)
        val engine = FakeEngine(c.screen)
        val audit = MemoryAudit()
        val exec = TrustedExecutor(engine, TrustEngine(), FakeConfirmer(yes, fingerprintOk), audit) { c }
        return Triple(exec, engine, audit)
    }

    @Test
    fun `send happens only after yes`() =
        runTest {
            val (exec, engine, audit) = setup(yes = true)
            assertTrue(exec.execute(tap("send"), scope()) is ActionResult.Ok)
            assertEquals(listOf("send"), engine.taps)
            assertEquals("R11", audit.decisions.single().rule)
        }

    @Test
    fun `no means nothing is tapped`() =
        runTest {
            val (exec, engine, _) = setup(yes = false)
            assertTrue(exec.execute(tap("send"), scope()) is ActionResult.Failed)
            assertTrue(engine.taps.isEmpty())
        }

    @Test
    fun `submit needs the fingerprint too`() =
        runTest {
            val c = ctx(el("ok", "Submit"))
            val engine = FakeEngine(c.screen)
            val exec = TrustedExecutor(engine, TrustEngine(), FakeConfirmer(sayYes = true, fingerprintOk = false), MemoryAudit()) { c }
            assertTrue(exec.execute(tap("ok"), scope(maxRisk = Risk.SUBMIT)) is ActionResult.Failed)
            assertTrue(engine.taps.isEmpty())
        }

    @Test
    fun `screen changed before tapping means re-plan`() =
        runTest {
            val (exec, engine, _) = setup(yes = true)
            engine.screen = ScreenSnapshot(foregroundPackage = Fx.WHATSAPP) // Send button vanished
            assertTrue(exec.execute(tap("send"), scope()) is ActionResult.Failed)
            assertTrue(engine.taps.isEmpty())
        }

    @Test
    fun `stop calls engine stop`() =
        runTest {
            val c = ctx(send).copy(stopPressed = true)
            val engine = FakeEngine(c.screen)
            val exec = TrustedExecutor(engine, TrustEngine(), FakeConfirmer(true), MemoryAudit()) { c }
            exec.execute(tap("send"), scope())
            assertTrue(engine.stopped)
        }

    @Test
    fun `no engine mutating call can happen without a Decision`() =
        runTest {
            // Test multiple refused/aborted conditions: engine taps/types must remain empty
            val c = ctx(el("x", "Display", pkg = "com.android.settings"), pkg = "com.android.settings")
            val engine = FakeEngine(c.screen)
            val audit = MemoryAudit()
            val exec = TrustedExecutor(engine, TrustEngine(), FakeConfirmer(sayYes = true), audit) { c }

            // 1. Refused by R2 (package outside scope)
            val res = exec.execute(tap("x"), scope())
            assertTrue(res is ActionResult.Failed)
            assertTrue(engine.taps.isEmpty(), "Engine must not perform any mutating action when refused")
            assertEquals("R2", audit.decisions.single().rule)

            // 2. Refused by R3 (action outside scope)
            val sRead = scope(packages = setOf("com.android.settings")).copy(allowedActions = setOf(ActionType.SCROLL))
            val res3 = exec.execute(tap("x"), sRead)
            assertTrue(res3 is ActionResult.Failed)
            assertTrue(engine.taps.isEmpty())
            assertEquals("R3", audit.decisions.last().rule)
        }

    @Test
    fun `every step appears in the audit log`() =
        runTest {
            val (exec, engine, audit) = setup(yes = true)

            // Step 1: Send
            exec.execute(tap("send"), scope())
            assertEquals(1, audit.decisions.size)
            assertEquals("R11", audit.decisions[0].rule)

            // Step 2: Harmless tap
            val c2 = ctx(el("chat", "Amma"))
            engine.screen = c2.screen
            val exec2 = TrustedExecutor(engine, TrustEngine(), FakeConfirmer(sayYes = true), audit) { c2 }
            exec2.execute(tap("chat"), scope())
            assertEquals(2, audit.decisions.size)
            assertEquals(null, audit.decisions[1].rule) // default allow
        }
}
