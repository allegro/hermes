package pl.allegro.tech.hermes.integration;

import org.testng.annotations.Test;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class HermesServerGracefulShutdownTest extends AbstractFrontendShutdownTest {

    @Test
    public void shouldShutdownGracefully() throws Throwable {
        //given
        hermesServer.gracefulShutdown();

        //when
        Response response = publisher.publish("topic", TestMessage.of("hello", "world").body());

        //then
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }
}
