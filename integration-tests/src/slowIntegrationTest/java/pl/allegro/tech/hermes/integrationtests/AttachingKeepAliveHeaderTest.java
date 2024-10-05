package pl.allegro.tech.hermes.integrationtests;

import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_KEEP_ALIVE_HEADER_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_KEEP_ALIVE_HEADER_TIMEOUT;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import java.util.function.Consumer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesFrontendTestApp;
import pl.allegro.tech.hermes.integrationtests.setup.HermesManagementExtension;
import pl.allegro.tech.hermes.integrationtests.setup.InfrastructureExtension;
import pl.allegro.tech.hermes.test.helper.client.integration.FrontendTestClient;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

public class AttachingKeepAliveHeaderTest {

  @Order(0)
  @RegisterExtension
  public static InfrastructureExtension infra = new InfrastructureExtension();

  @Order(1)
  @RegisterExtension
  public static HermesManagementExtension management = new HermesManagementExtension(infra);

  private static final String MESSAGE = TestMessage.of("hello", "world").body();

  @Test
  public void shouldAttachKeepAliveHeaderWhenEnabled() {
    // given
    HermesFrontendTestApp frontend =
        startFrontend(
            f -> {
              f.withProperty(FRONTEND_KEEP_ALIVE_HEADER_ENABLED, true);
              f.withProperty(FRONTEND_KEEP_ALIVE_HEADER_TIMEOUT, "2s");
            });

    Topic topic = management.initHelper().createTopic(topicWithRandomName().build());

    FrontendTestClient publisher = new FrontendTestClient(frontend.getPort());

    try {
      // when
      WebTestClient.ResponseSpec response = publisher.publish(topic.getQualifiedName(), MESSAGE);

      // then
      response.expectHeader().valueEquals("Keep-Alive", "timeout=2");
    } finally {
      frontend.stop();
    }
  }

  @Test
  public void shouldNotAttachKeepAliveHeaderWhenDisabled() {
    // given
    HermesFrontendTestApp frontend =
        startFrontend(f -> f.withProperty(FRONTEND_KEEP_ALIVE_HEADER_ENABLED, false));

    Topic topic = management.initHelper().createTopic(topicWithRandomName().build());

    FrontendTestClient publisher = new FrontendTestClient(frontend.getPort());

    try {
      // when
      WebTestClient.ResponseSpec response = publisher.publish(topic.getQualifiedName(), MESSAGE);

      // then
      response.expectHeader().doesNotExist("Keep-Alive");
    } finally {
      frontend.stop();
    }
  }

  private HermesFrontendTestApp startFrontend(
      Consumer<HermesFrontendTestApp> frontendConfigUpdater) {
    HermesFrontendTestApp frontend =
        new HermesFrontendTestApp(infra.hermesZookeeper(), infra.kafka(), infra.schemaRegistry());
    frontendConfigUpdater.accept(frontend);
    frontend.start();
    return frontend;
  }
}
