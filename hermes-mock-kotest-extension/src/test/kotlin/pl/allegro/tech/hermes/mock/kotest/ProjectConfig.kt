package pl.allegro.tech.hermes.mock.kotest

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import pl.allegro.tech.hermes.mock.HermesMock

object ProjectConfig : AbstractProjectConfig() {
    const val HERMES_MOCK_PORT = 9001

    val hermesMock = HermesMock.Builder().withPort(HERMES_MOCK_PORT).build()
    val hermesMockListener = HermesMockListener(hermesMock, ListenerMode.PER_PROJECT)

    override fun extensions(): List<Extension> = listOf(hermesMockListener)
}
