package pl.allegro.tech.hermes.integration;

import org.testng.annotations.Test;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class HealthCheckTest extends AbstractFrontendShutdownTest {

    WebTarget client = ClientBuilder.newClient().target(FRONTEND_URL).path("status").path("ping");

    @Test
    public void shouldReturnCorrectHealthStatus() throws InterruptedException {
        // given
        assertThat(client.request().get()).hasStatus(Response.Status.OK);

        // when
        hermesServer.gracefulShutdown();

        // then
        assertThat(client.request().get()).hasStatus(Response.Status.SERVICE_UNAVAILABLE);
    }

}
