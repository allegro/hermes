package pl.allegro.tech.hermes.integration;

import org.testng.annotations.Test;
import pl.allegro.tech.hermes.test.helper.endpoint.JerseyClientFactory;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class HealthCheckTest extends AbstractFrontendShutdownTest {

    @Test(enabled = false)
    public void shouldReturnCorrectHealthStatus() throws InterruptedException {
        // given
        WebTarget client = JerseyClientFactory.create().target(FRONTEND_URL).path("status").path("ping");
        assertThat(client.request().get()).hasStatus(Response.Status.OK);

        // when
        hermesServer.gracefulShutdown();

        // then
        assertThat(client.request().get()).hasStatus(Response.Status.SERVICE_UNAVAILABLE);
    }

}
