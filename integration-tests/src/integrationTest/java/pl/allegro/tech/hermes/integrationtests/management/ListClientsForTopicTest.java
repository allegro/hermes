package pl.allegro.tech.hermes.integrationtests.management;

import static pl.allegro.tech.hermes.integrationtests.assertions.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;

public class ListClientsForTopicTest {

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  @Test
  public void shouldListClientsForTopic() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    List<String> expectedResponse = Arrays.asList("Smurfs", "Admins");
    hermes
        .initHelper()
        .createSubscription(
            subscriptionWithRandomName(topic.getName())
                .withOwner(new OwnerId("Plaintext", "Smurfs"))
                .build());
    hermes
        .initHelper()
        .createSubscription(
            subscriptionWithRandomName(topic.getName())
                .withOwner(new OwnerId("Plaintext", "Admins"))
                .build());

    // then
    assertThat(listClientsForTopic(topic.getQualifiedName()))
        .containsExactlyInAnyOrderElementsOf(expectedResponse);
  }

  @Test
  public void shouldListClientsForTopicWithoutRepeating() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    hermes
        .initHelper()
        .createSubscription(
            subscriptionWithRandomName(topic.getName())
                .withOwner(new OwnerId("Plaintext", "Team A"))
                .build());
    hermes
        .initHelper()
        .createSubscription(
            subscriptionWithRandomName(topic.getName())
                .withOwner(new OwnerId("Plaintext", "Team A"))
                .build());
    hermes
        .initHelper()
        .createSubscription(
            subscriptionWithRandomName(topic.getName())
                .withOwner(new OwnerId("Plaintext", "Team B"))
                .build());
    hermes
        .initHelper()
        .createSubscription(
            subscriptionWithRandomName(topic.getName())
                .withOwner(new OwnerId("Plaintext", "Team B"))
                .build());
    List<String> expectedResponse = Arrays.asList("Team A", "Team B");

    // then
    assertThat(listClientsForTopic(topic.getQualifiedName()))
        .containsExactlyInAnyOrderElementsOf(expectedResponse);
  }

  private List<String> listClientsForTopic(String topicQualifiedName) {
    return Arrays.asList(
        Objects.requireNonNull(
            hermes
                .api()
                .getAllTopicClients(topicQualifiedName)
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(String[].class)
                .returnResult()
                .getResponseBody()));
  }
}
