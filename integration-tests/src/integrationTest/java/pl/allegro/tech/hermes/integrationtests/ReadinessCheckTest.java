package pl.allegro.tech.hermes.integrationtests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;

import java.time.Duration;

import static org.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.infrastructure.dc.DefaultDatacenterNameProvider.DEFAULT_DC_NAME;

public class ReadinessCheckTest {

    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension();

    @Test
    public void shouldRespectReadinessStatusSetByAdmin() {
        // when
        hermes.api().setReadiness(DEFAULT_DC_NAME, false).expectStatus().isAccepted();

        // then
        waitAtMost(Duration.ofSeconds(5)).untilAsserted(() ->
                hermes.api()
                        .getFrontendReadiness()
                        .expectStatus().is5xxServerError()
                        .expectBody(String.class).isEqualTo("NOT_READY")
        );

        // when
        hermes.api().setReadiness(DEFAULT_DC_NAME, true).expectStatus().isAccepted();

        // then
        waitAtMost(Duration.ofSeconds(5)).untilAsserted(() ->
                hermes.api()
                        .getFrontendReadiness()
                        .expectStatus().isOk()
                        .expectBody(String.class).isEqualTo("READY")
        );
    }
}
