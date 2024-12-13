package pl.allegro.tech.hermes.mock.kotest

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.net.HttpURLConnection
import java.net.URL

class HermesMockListenerPerProjectTest : FunSpec({

    test("should have started HermesMock") {
        val connection = URL("http://localhost:${ProjectConfig.HERMES_MOCK_PORT}").openConnection() as HttpURLConnection

        connection.responseCode shouldBe 404
    }
})