package pl.allegro.tech.hermes.integrationtests;

import static jakarta.ws.rs.client.ClientBuilder.newClient;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.client.HermesClientBuilder.hermesClient;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import jakarta.ws.rs.core.UriBuilder;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.client.HermesClient;
import pl.allegro.tech.hermes.client.HermesResponse;
import pl.allegro.tech.hermes.client.jersey.JerseyHermesSender;
import pl.allegro.tech.hermes.client.okhttp.OkHttpHermesSender;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

public class HermesClientPublishingTest {

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();
  private static final String FRONTEND_URI = "http://localhost:{frontendPort}";
  private final TestMessage message = TestMessage.of("hello", "world");

  @Test
  public void shouldPublishUsingJerseyClient() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    HermesClient client =
        hermesClient(new JerseyHermesSender(newClient()))
            .withURI(UriBuilder.fromUri(FRONTEND_URI).build(hermes.getFrontendPort()))
            .build();

    // when & then
    runTestSuiteForHermesClient(client, topic);
  }

  @Test
  public void shouldPublishUsingOkHttpClient() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    HermesClient client =
        hermesClient(new OkHttpHermesSender(new OkHttpClient()))
            .withURI(UriBuilder.fromUri(FRONTEND_URI).build(hermes.getFrontendPort()))
            .build();

    // when & then
    runTestSuiteForHermesClient(client, topic);
  }

  private void runTestSuiteForHermesClient(HermesClient client, Topic topic) {
    shouldPublishUsingHermesClient(client, topic);
    shouldNotPublishUsingHermesClient(client);
  }

  private void shouldPublishUsingHermesClient(HermesClient client, Topic topic) {
    // when
    HermesResponse response = client.publish(topic.getQualifiedName(), message.body()).join();

    // then
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.getMessageId()).isNotEmpty();
    assertThat(response.getDebugLog()).contains("Sending message").contains("succeeded");
  }

  private void shouldNotPublishUsingHermesClient(HermesClient client) {
    // given
    TopicName topic = new TopicName("not", "existing");

    // when
    HermesResponse response = client.publish(topic.qualifiedName(), message.body()).join();

    // then
    assertThat(response.isFailure()).isTrue();
    assertThat(response.getDebugLog()).contains("Sending message").contains("failed");
  }
}
