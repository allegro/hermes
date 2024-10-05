package pl.allegro.tech.hermes.integrationtests;

import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

public class PublishingTest {
  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  @Test
  public void shouldReturn429ForQuotaViolation() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

    // Frontend is configured in integration test suite to block publisher after 50_000 kb/sec
    TestMessage message = TestMessage.of("content", StringUtils.repeat("X", 60_000));

    hermes
        .api()
        .publishUntilStatus(
            topic.getQualifiedName(), message.body(), HttpStatus.TOO_MANY_REQUESTS.value());
  }

  @Test
  public void shouldReturn4xxForTooLargeContent() {
    // given
    Topic topic =
        hermes.initHelper().createTopic(topicWithRandomName().withMaxMessageSize(2048).build());

    // when
    WebTestClient.ResponseSpec response =
        hermes.api().publish(topic.getQualifiedName(), StringUtils.repeat("X", 2555));

    // then
    response.expectStatus().isBadRequest();
  }
}
