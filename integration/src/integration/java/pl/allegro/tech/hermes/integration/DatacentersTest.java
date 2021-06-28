package pl.allegro.tech.hermes.integration;

import com.google.common.collect.Lists;
import java.util.List;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.test.helper.endpoint.JerseyClientFactory;

import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class DatacentersTest extends IntegrationTest {
    @Test
    public void shouldReturnAvailableDatacenters() {
        // given
        WebTarget client = JerseyClientFactory.create().target(MANAGEMENT_ENDPOINT_URL).path("datacenters");

        // when
        List<String> datacenters = client.request().get().readEntity(new GenericType<List<String>>() {
        });

        // then
        assertThat(datacenters).isEqualTo(Lists.newArrayList("dc"));
    }
}
