package pl.allegro.tech.hermes.integrationtests.management;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;

public class ListSubscriptionForOwnerTest {

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  private static Topic topic;

  @BeforeAll
  public static void createTopic() {
    topic = hermes.initHelper().createTopic(topicWithRandomName().build());
  }

  @Test
  public void shouldListSubscriptionsForOwnerId() {
    // given
    Subscription subscription1 =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName())
                    .withOwner(new OwnerId("Plaintext", "ListSubForOwner - Team A"))
                    .build());
    Subscription subscription2 =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName())
                    .withOwner(new OwnerId("Plaintext", "ListSubForOwner - Team A"))
                    .build());
    Subscription subscription3 =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName())
                    .withOwner(new OwnerId("Plaintext", "ListSubForOwner - Team B"))
                    .build());

    // then
    assertThat(listSubscriptionsForOwner("ListSubForOwner - Team A"))
        .containsOnly(subscription1.getName(), subscription2.getName());
    assertThat(listSubscriptionsForOwner("ListSubForOwner - Team B"))
        .containsExactly(subscription3.getName());
  }

  @Test
  public void shouldListSubscriptionAfterNewSubscriptionIsAdded() {
    // given
    assertThat(listSubscriptionsForOwner("ListSubForOwner - Team C")).isEmpty();

    // when
    Subscription subscription =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName())
                    .withOwner(new OwnerId("Plaintext", "ListSubForOwner - Team C"))
                    .build());

    // then
    assertThat(listSubscriptionsForOwner("ListSubForOwner - Team C"))
        .containsExactly(subscription.getName());
  }

  @Test
  public void shouldListSubscriptionAfterOwnerIsChanged() {
    // given
    Subscription subscription =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName())
                    .withOwner(new OwnerId("Plaintext", "ListSubForOwner - Team D"))
                    .build());

    // then
    assertThat(listSubscriptionsForOwner("ListSubForOwner - Team D"))
        .containsExactly(subscription.getName());
    assertThat(listSubscriptionsForOwner("ListSubForOwner - Team E")).isEmpty();

    // when
    hermes
        .api()
        .updateSubscription(
            topic,
            subscription.getName(),
            PatchData.patchData()
                .set("owner", new OwnerId("Plaintext", "ListSubForOwner - Team E"))
                .build());

    // then
    assertThat(listSubscriptionsForOwner("ListSubForOwner - Team D")).isEmpty();
    assertThat(listSubscriptionsForOwner("ListSubForOwner - Team E"))
        .containsExactly(subscription.getName());
  }

  @Test
  public void shouldNotListTopicAfterIsDeleted() {
    // given
    Subscription subscription1 =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName())
                    .withOwner(new OwnerId("Plaintext", "ListSubForOwner - Team F"))
                    .build());
    Subscription subscription2 =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName())
                    .withOwner(new OwnerId("Plaintext", "ListSubForOwner - Team G"))
                    .build());

    // then
    assertThat(listSubscriptionsForOwner("ListSubForOwner - Team F"))
        .containsExactly(subscription1.getName());
    assertThat(listSubscriptionsForOwner("ListSubForOwner - Team G"))
        .containsExactly(subscription2.getName());

    // when
    hermes
        .api()
        .deleteSubscription(topic.getQualifiedName(), subscription1.getName())
        .expectStatus()
        .is2xxSuccessful();

    // then
    assertThat(listSubscriptionsForOwner("ListSubForOwner - Team F")).isEmpty();
    assertThat(listSubscriptionsForOwner("ListSubForOwner - Team G"))
        .containsExactly(subscription2.getName());
  }

  private List<String> listSubscriptionsForOwner(String ownerId) {
    return Objects.requireNonNull(
            hermes
                .api()
                .getSubscriptionsForOwner("Plaintext", ownerId)
                .expectStatus()
                .is2xxSuccessful()
                .expectBodyList(Subscription.class)
                .returnResult()
                .getResponseBody())
        .stream()
        .map(Subscription::getName)
        .collect(Collectors.toList());
  }
}
