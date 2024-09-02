package pl.allegro.tech.hermes.integrationtests;

import static org.awaitility.Awaitility.waitAtMost;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.integrationtests.setup.HermesFrontendTestApp;
import pl.allegro.tech.hermes.integrationtests.setup.InfrastructureExtension;
import pl.allegro.tech.hermes.test.helper.client.integration.FrontendTestClient;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

public class HermesServerGracefulShutdownTest {

  @RegisterExtension public static InfrastructureExtension infra = new InfrastructureExtension();

  private HermesFrontendTestApp frontend;
  private HermesServer hermesServer;
  FrontendTestClient frontendClient;

  @BeforeEach
  public void beforeEach() {
    frontend =
        new HermesFrontendTestApp(infra.hermesZookeeper(), infra.kafka(), infra.schemaRegistry());
    frontend.start();
    hermesServer = frontend.getBean(HermesServer.class);
    frontendClient = new FrontendTestClient(frontend.getPort());
  }

  @AfterEach
  public void afterEach() {
    frontend.stop();
  }

  @Test
  public void shouldShutdownGracefully() throws Throwable {
    // given
    hermesServer.prepareForGracefulShutdown();

    // when
    WebTestClient.ResponseSpec response =
        frontendClient.publish("topic", TestMessage.of("hello", "world").body());

    // then
    response.expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  public void shouldReturnCorrectHealthStatus() throws InterruptedException {
    // when
    hermesServer.prepareForGracefulShutdown();

    // then
    waitAtMost(5, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                frontendClient
                    .getStatusPing()
                    .expectStatus()
                    .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
  }
}
