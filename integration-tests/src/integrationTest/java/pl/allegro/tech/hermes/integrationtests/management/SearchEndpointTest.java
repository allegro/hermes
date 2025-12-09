package pl.allegro.tech.hermes.integrationtests.management;

import static pl.allegro.tech.hermes.test.helper.assertions.SearchResultsAssertion.assertThat;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.SearchResults;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.integrationtests.prometheus.PrometheusExtension;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder;

public class SearchEndpointTest {
  @Order(0)
  @RegisterExtension
  public static final PrometheusExtension prometheus = new PrometheusExtension();

  @Order(1)
  @RegisterExtension
  public static final HermesExtension hermes = new HermesExtension().withPrometheus(prometheus);

  private static final String FIRST_TOPIC_NAME = "SearchFirstTopic";
  private static final String SECOND_TOPIC_NAME = "SearchSecondTopic";
  private static final String FIRST_SUBSCRIPTION_NAME = "search-first-subscription";
  private static final String SECOND_SUBSCRIPTION_NAME = "search-second-subscription";

  @Test
  public void shouldFindSingleTopic() {
    // given
    Topic topic = createTopicWithNameContaining(FIRST_TOPIC_NAME);

    // when
    SearchResults results = search(topic.getQualifiedName());

    // then
    assertThat(results).containsOnlySingleItemForTopic(topic);
  }

  @Test
  public void shouldFindSingleSubscription() {
    // given
    Topic topic = createTopicWithNameContaining(FIRST_TOPIC_NAME);
    Subscription subscription =
        createSubscriptionWithNameContaining(topic.getName(), FIRST_SUBSCRIPTION_NAME);

    // when
    SearchResults results = search(subscription.getName());

    // then
    assertThat(results).containsOnlySingleItemForSubscription(subscription);
  }

  @Test
  public void shouldFindTopicAndSubscriptionWhenQueryMatchesBoth() {
    // given
    String commonPart = "search-common";
    String subscriptionName = commonPart + "-subscription";
    Topic topic = createTopicWithNameContaining(commonPart);
    Subscription subscription =
        createSubscriptionWithNameContaining(topic.getName(), subscriptionName);

    // when
    SearchResults results = search(commonPart);

    // then
    assertThat(results)
        .hasExactNumberOfResults(2)
        .containsItemForTopic(topic)
        .containsItemForSubscription(subscription);
  }

  @Test
  public void shouldNotFindAnythingForQueryThatDoesNotMatchAnything() {
    // given
    Topic topic = createTopicWithNameContaining(FIRST_TOPIC_NAME);
    createSubscriptionWithNameContaining(topic.getName(), FIRST_SUBSCRIPTION_NAME);

    // when
    SearchResults results = search("non-existing-topic-or-subscription");

    // then
    assertThat(results).hasNoResults();
  }

  @Test
  public void shouldNotFindTopicAfterItWasDeleted() {
    // given
    Topic topic1 = createTopicWithNameContaining(FIRST_TOPIC_NAME);
    Topic topic2 = createTopicWithNameContaining(SECOND_TOPIC_NAME);

    // when
    SearchResults resultsBeforeDeletion = search("Topic");

    // then: verify topic is found
    assertThat(resultsBeforeDeletion)
        .containsItemWithName(topic1.getQualifiedName())
        .containsItemWithName(topic2.getQualifiedName());

    // when
    hermes.api().deleteTopic(topic2.getQualifiedName()).expectStatus().isOk();
    SearchResults resultsAfterDeletion = search("Topic");

    // then: verify only the first topic is found
    assertThat(resultsAfterDeletion)
        .doesNotContainItemWithName(topic2.getQualifiedName())
        .containsItemWithName(topic1.getQualifiedName());
  }

  @Test
  public void shouldNotFindSubscriptionAfterItWasDeleted() {
    // given
    Topic topic = createTopicWithNameContaining(FIRST_TOPIC_NAME);
    Subscription subscription1 =
        createSubscriptionWithNameContaining(topic.getName(), FIRST_SUBSCRIPTION_NAME);
    Subscription subscription2 =
        createSubscriptionWithNameContaining(topic.getName(), SECOND_SUBSCRIPTION_NAME);

    // when
    SearchResults resultsBeforeDeletion = search("subscription");

    // then: verify subscription is found
    assertThat(resultsBeforeDeletion)
        .containsItemWithName(subscription1.getName())
        .containsItemWithName(subscription2.getName());

    // when
    hermes
        .api()
        .deleteSubscription(topic.getQualifiedName(), subscription2.getName())
        .expectStatus()
        .isOk();
    SearchResults resultsAfterDeletion = search("subscription");

    // then: verify only the first subscription is found
    assertThat(resultsAfterDeletion)
        .containsItemWithName(subscription1.getName())
        .doesNotContainItemWithName(subscription2.getName());
  }

  @Test
  public void shouldFindTopicByItsUpdatedOwnerIdAfterItWasUpdated() {
    // given
    OwnerId oldOwnerId = new OwnerId("Plaintext", "1234");
    Topic topic =
        TopicBuilder.topicWithRandomNameContaining(FIRST_TOPIC_NAME).withOwner(oldOwnerId).build();
    hermes.initHelper().createTopic(topic);

    // when
    SearchResults resultsBeforeUpdate = search(oldOwnerId.getId());

    // then
    assertThat(resultsBeforeUpdate)
        .containsTopicItemWithName(topic.getQualifiedName())
        .hasOwnerId(oldOwnerId.getId());

    // when
    OwnerId newOwnerId = new OwnerId("Plaintext", "5678");
    hermes
        .api()
        .updateTopic(
            topic.getQualifiedName(), PatchData.patchData().set("owner", newOwnerId).build())
        .expectStatus()
        .isOk();
    SearchResults resultsAfterUpdate = search(newOwnerId.getId());

    // then
    assertThat(resultsAfterUpdate)
        .containsTopicItemWithName(topic.getQualifiedName())
        .hasOwnerId(newOwnerId.getId());
  }

  @Test
  public void shouldFindSubscriptionByItsUpdatedEndpointAfterItWasUpdated() {
    // given
    String oldEndpoint = "https://sample-service/events/sample-event";
    Topic topic = createTopicWithNameContaining(FIRST_TOPIC_NAME);
    Subscription subscription =
        SubscriptionBuilder.subscriptionWithRandomNameContaining(
                topic.getName(), FIRST_SUBSCRIPTION_NAME)
            .withEndpoint(oldEndpoint)
            .build();
    hermes.initHelper().createSubscription(subscription);

    // when
    SearchResults resultsBeforeUpdate = search(oldEndpoint);

    // then
    assertThat(resultsBeforeUpdate)
        .containsSubscriptionItemWithName(subscription.getName())
        .hasEndpoint(oldEndpoint);

    // when
    String updatedEndpoint = "https://sample-service/events/sample-event-updated";
    hermes
        .api()
        .updateSubscription(
            topic,
            subscription.getName(),
            PatchData.patchData().set("endpoint", updatedEndpoint).build())
        .expectStatus()
        .isOk();
    SearchResults resultsAfterUpdate = search(updatedEndpoint);

    // then
    assertThat(resultsAfterUpdate)
        .containsSubscriptionItemWithName(subscription.getName())
        .hasEndpoint(updatedEndpoint);
  }

  private Topic createTopicWithNameContaining(String qualifiedName) {
    return hermes
        .initHelper()
        .createTopic(TopicBuilder.topicWithRandomNameContaining(qualifiedName).build());
  }

  private Subscription createSubscriptionWithNameContaining(
      TopicName topicQualifiedName, String subscriptionName) {
    return hermes
        .initHelper()
        .createSubscription(
            SubscriptionBuilder.subscriptionWithRandomNameContaining(
                    topicQualifiedName, subscriptionName)
                .build());
  }

  private SearchResults search(String query) {
    return hermes
        .api()
        .search(query)
        .expectStatus()
        .isOk()
        .expectBody(SearchResults.class)
        .returnResult()
        .getResponseBody();
  }
}
