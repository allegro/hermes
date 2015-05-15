package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class EndpointAddressTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void shouldDeserializeStringToSubscriptionEndpoint() throws IOException {
        // given
        String json = "\"endpoint definition\"";

        // when
        EndpointAddress endpoint = mapper.readValue(json.getBytes("UTF-8"), EndpointAddress.class);

        // then
        assertThat(endpoint.getEndpoint()).isEqualTo("endpoint definition");
    }

    @Test
    public void shouldSerializeSubscriptionEndpointToString() throws IOException {
        // given
        String expectedJson = "\"endpoint serialized\"";

        // when
        String json = mapper.writeValueAsString(EndpointAddress.of("endpoint serialized"));

        // then
        assertThat(json).isEqualTo(expectedJson);
    }

    @Test
    public void shouldReturnProtocol() {
        // given
        String url = "http://some.endpoint.com/test";

        // when
        EndpointAddress endpoint = EndpointAddress.of(url);

        // then
        assertThat(endpoint.getProtocol()).isEqualTo("http");
    }

    @Test
    public void shouldAnonymizePassword() {
        // given
        EndpointAddress endpointAddress = new EndpointAddress("xyz://a_b.c:xyz.%$#@some.endpoint.com/test");

        // when & then
        assertThat(endpointAddress.anonymizePassword().getEndpoint()).isEqualTo("xyz://a_b.c:*****@some.endpoint.com/test");
    }

}