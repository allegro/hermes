package pl.allegro.tech.hermes.integration;

import org.testng.annotations.Test;
import pl.allegro.tech.hermes.test.helper.endpoint.JerseyClientFactory;

import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.WebTarget;

import static com.jayway.awaitility.Awaitility.await;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class HealthCheckTest extends AbstractFrontendShutdownTest {

    @Test
    public void shouldReturnCorrectHealthStatus() throws InterruptedException {
        // given
        WebTarget client = JerseyClientFactory.create().target(FRONTEND_URL).path("status").path("ping");

        // when
        hermesServer.prepareForGracefulShutdown();

        // then
        await().atMost(5, TimeUnit.SECONDS).until(() -> assertThat(client.request().get()).hasStatus(SERVICE_UNAVAILABLE));
    }

}
