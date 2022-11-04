package pl.allegro.tech.hermes.integration.management;

import org.testng.annotations.Test;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.test.helper.endpoint.JerseyClientFactory;

import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static com.jayway.awaitility.Awaitility.await;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class HealthCheckTest extends IntegrationTest {

    @Test
    public void shouldManagementBeHealthy() {
        // given
        WebTarget client = JerseyClientFactory.create().target(MANAGEMENT_ENDPOINT_URL).path("status").path("health");

        // when & then
        await().atMost(5, TimeUnit.SECONDS).until(() ->
                assertThat(client.request().get()).hasStatus(Response.Status.OK));
    }
}
