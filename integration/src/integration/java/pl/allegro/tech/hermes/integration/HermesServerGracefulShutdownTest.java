package pl.allegro.tech.hermes.integration;

import jakarta.ws.rs.core.Response;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class HermesServerGracefulShutdownTest extends AbstractFrontendShutdownTest {

    @Test
    public void shouldShutdownGracefully() throws Throwable {
        //given
        hermesServer.prepareForGracefulShutdown();

        //when
        Response response = publisher.publish("topic", TestMessage.of("hello", "world").body());

        //then
        assertThat(response).hasStatus(Response.Status.SERVICE_UNAVAILABLE);
    }
}
