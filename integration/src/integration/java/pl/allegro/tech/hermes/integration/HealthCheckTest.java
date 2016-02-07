package pl.allegro.tech.hermes.integration;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.testng.annotations.Test;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class HealthCheckTest extends AbstractFrontendShutdownTest {

    @Test
    public void shouldReturnCorrectHealthStatus() throws InterruptedException {
        // given
        ClientConfig configuration = new ClientConfig();
        configuration = configuration.property(ClientProperties.CONNECT_TIMEOUT, 1000);
        configuration = configuration.property(ClientProperties.READ_TIMEOUT, 1000);
        WebTarget client = ClientBuilder.newClient(configuration).target(FRONTEND_URL).path("status").path("ping");

        assertThat(client.request().get()).hasStatus(Response.Status.OK);

        // when
        hermesServer.gracefulShutdown();

        // then
        assertThat(client.request().get()).hasStatus(Response.Status.SERVICE_UNAVAILABLE);
    }

}
