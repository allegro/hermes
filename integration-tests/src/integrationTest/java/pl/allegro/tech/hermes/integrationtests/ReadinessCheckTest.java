package pl.allegro.tech.hermes.integrationtests;

import static org.awaitility.Awaitility.waitAtMost;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static pl.allegro.tech.hermes.infrastructure.dc.DefaultDatacenterNameProvider.DEFAULT_DC_NAME;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;

public class ReadinessCheckTest {

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  @Test
  public void shouldRespectReadinessStatusSetByAdmin() {
    // when
    hermes.api().setReadiness(DEFAULT_DC_NAME, false).expectStatus().isAccepted();

    // then
    waitAtMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                hermes
                    .api()
                    .getFrontendReadiness()
                    .expectStatus()
                    .is5xxServerError()
                    .expectBody(String.class)
                    .isEqualTo("NOT_READY"));

    // when
    hermes.api().setReadiness(DEFAULT_DC_NAME, true).expectStatus().isAccepted();

    // then
    waitAtMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                hermes
                    .api()
                    .getFrontendReadiness()
                    .expectStatus()
                    .isOk()
                    .expectBody(String.class)
                    .isEqualTo("READY"));
  }

  @Test
  public void shouldRejectPublishingWhenFrontendIsNotReady() {
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

    try {
      hermes.api().setReadiness(DEFAULT_DC_NAME, false).expectStatus().isAccepted();

      waitAtMost(Duration.ofSeconds(5))
          .untilAsserted(
              () ->
                  hermes
                      .api()
                      .getFrontendReadiness()
                      .expectStatus()
                      .isEqualTo(SERVICE_UNAVAILABLE));

      hermes
          .api()
          .publish(topic.getQualifiedName(), "message")
          .expectStatus()
          .isEqualTo(SERVICE_UNAVAILABLE)
          .expectBody(String.class)
          .isEqualTo("NOT_READY");
    } finally {
      hermes.api().setReadiness(DEFAULT_DC_NAME, true).expectStatus().isAccepted();

      waitAtMost(Duration.ofSeconds(5))
          .untilAsserted(
              () -> hermes.api().getFrontendReadiness().expectStatus().is2xxSuccessful());
    }
  }
}
