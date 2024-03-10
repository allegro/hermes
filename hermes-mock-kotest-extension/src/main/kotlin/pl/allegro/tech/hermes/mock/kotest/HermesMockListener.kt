package pl.allegro.tech.hermes.mock.kotest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.listeners.ProjectListener
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import pl.allegro.tech.hermes.mock.HermesMock

private val logger = KotlinLogging.logger { }

class HermesMockListener(
    private val hermesMock: HermesMock,
    private val listenerMode: ListenerMode
) : TestListener, ProjectListener {
    override suspend fun beforeTest(testCase: TestCase) {
        if (listenerMode == ListenerMode.PER_TEST) {
            logger.debug { "Started Mock Hermes $listenerMode" }
            hermesMock.start()
        }
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        if (listenerMode == ListenerMode.PER_TEST) {
            logger.debug { "Stopped Mock Hermes $listenerMode" }
            hermesMock.stop()
        }
    }

    override suspend fun beforeSpec(spec: Spec) {
        if (listenerMode == ListenerMode.PER_SPEC) {
            logger.debug { "Started Mock Hermes $listenerMode" }
            hermesMock.start()
        }
    }

    override suspend fun afterSpec(spec: Spec) {
        if (listenerMode == ListenerMode.PER_SPEC) {
            logger.debug { "Stopped Mock Hermes $listenerMode" }
            hermesMock.stop()
        }
    }

    override suspend fun beforeProject() {
        if (listenerMode == ListenerMode.PER_PROJECT) {
            logger.debug { "Started Mock Hermes $listenerMode" }
            hermesMock.start()
        }
    }

    override suspend fun afterProject() {
        if (listenerMode == ListenerMode.PER_PROJECT) {
            logger.debug { "Stopped Mock Hermes $listenerMode" }
            hermesMock.stop()
        }
    }

    companion object {
        fun perSpec(hermesMock: HermesMock) = HermesMockListener(hermesMock, ListenerMode.PER_SPEC)
        fun perTest(hermesMock: HermesMock) = HermesMockListener(hermesMock, ListenerMode.PER_TEST)
        fun perProject(hermesMock: HermesMock) = HermesMockListener(hermesMock, ListenerMode.PER_PROJECT)
    }
}

enum class ListenerMode {
    PER_TEST,
    PER_SPEC,
    PER_PROJECT
}
