package pl.allegro.tech.hermes.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;

public class PublishingTimeoutTest {

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  @Test
  public void shouldHandleRequestTimeout() throws IOException, InterruptedException {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    int clientTimeout = 5000;
    int pauseTimeBetweenChunks = 300;
    int delayBeforeSendingFirstData = 0;

    // when
    long start = System.currentTimeMillis();
    String response =
        hermes
            .api()
            .publishSlowly(
                clientTimeout,
                pauseTimeBetweenChunks,
                delayBeforeSendingFirstData,
                topic.getQualifiedName());
    long elapsed = System.currentTimeMillis() - start;

    // then
    assertThat(response).contains("408 Request Time-out");
    assertThat(elapsed).isLessThan(2500);
  }

  @Test
  public void shouldCloseConnectionAfterSendingDelayData() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    int clientTimeout = 5000;
    int pauseTimeBetweenChunks = 0;
    int delayBeforeSendingFirstData = 3000;

    // when
    Exception thrown =
        assertThrows(
            Exception.class,
            () ->
                hermes
                    .api()
                    .publishSlowly(
                        clientTimeout,
                        pauseTimeBetweenChunks,
                        delayBeforeSendingFirstData,
                        topic.getQualifiedName()));

    // then
    LoggerFactory.getLogger(PublishingTimeoutTest.class).error("Caught exception", thrown);
    assertThat(thrown.getMessage()).containsAnyOf("Broken pipe", "Connection reset by peer");
  }

  @Test
  public void shouldHandleTimeoutForSlowRequestWithChunkedEncoding()
      throws IOException, InterruptedException {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    int clientTimeout = 5000;
    int pauseTimeBetweenChunks = 300;
    int delayBeforeSendingFirstData = 0;
    boolean chunkedEncoding = true;

    // when
    long start = System.currentTimeMillis();
    String response =
        hermes
            .api()
            .publishSlowly(
                clientTimeout,
                pauseTimeBetweenChunks,
                delayBeforeSendingFirstData,
                topic.getQualifiedName(),
                chunkedEncoding);
    long elapsed = System.currentTimeMillis() - start;

    // then
    assertThat(response).contains("408 Request Time-out");
    assertThat(elapsed).isLessThan(2500);
  }
}
