package pl.allegro.tech.hermes.integrationtests.management;

import static pl.allegro.tech.hermes.management.assertions.SearchResultsAssertion.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.SearchResults;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
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

  private static final String FIRST_GROUP_NAME = "pl.allegro";
  private static final String FIRST_TOPIC_NAME = "first-topic";
  private static final String SECOND_TOPIC_NAME = "second-topic";
  private static final String FIRST_TOPIC_QUALIFIED_NAME =
      FIRST_GROUP_NAME + "." + FIRST_TOPIC_NAME;
  private static final String SECOND_TOPIC_QUALIFIED_NAME =
      FIRST_GROUP_NAME + "." + SECOND_TOPIC_NAME;
  private static final String FIRST_SUBSCRIPTION_NAME = "first-subscription";
  private static final String SECOND_SUBSCRIPTION_NAME = "second-subscription";

  @BeforeEach
  public void setup() {
    hermes.clearManagementData();
  }

  @Test
  public void shouldReturnEmptyResultWhenCachesAreEmpty() {
    // when
    SearchResults results = search("abc");

    // then
    assertThat(results).hasNoResults();
  }

  @Test
  public void shouldFindTopicWhenOnlyTopicsCacheIsNotEmpty() {
    // given
    Topic topic = createTopic(FIRST_TOPIC_QUALIFIED_NAME);

    // when
    SearchResults results = search(FIRST_TOPIC_QUALIFIED_NAME);

    // then
    assertThat(results).containsOnlySingleItemForTopic(topic);
  }

  @Test
  public void shouldFindSingleSubscriptionWhenThereIsOneSubscription() {
    // given
    createTopic(FIRST_TOPIC_QUALIFIED_NAME);
    Subscription subscription =
        createSubscription(FIRST_TOPIC_QUALIFIED_NAME, FIRST_SUBSCRIPTION_NAME);

    // when
    SearchResults results = search(FIRST_SUBSCRIPTION_NAME);

    // then
    assertThat(results).containsOnlySingleItemForSubscription(subscription);
  }

  @Test
  public void shouldFindTopicAndSubscriptionWhenQueryMatchesBoth() {
    // given
    String commonPart = "common";
    String topicQualifiedName = FIRST_GROUP_NAME + "." + commonPart + "-topic";
    String subscriptionName = commonPart + "-subscription";
    Topic topic = createTopic(topicQualifiedName);
    Subscription subscription = createSubscription(topicQualifiedName, subscriptionName);

    // when
    SearchResults results = search(commonPart);

    // then
    assertThat(results)
        .hasExactNumberOfResults(2)
        .containsItemForTopic(topic)
        .containsItemForSubscription(subscription);
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        "topic|pl.first-topic,pl.second-topic,pl.hermes.third-topic",
        "top|pl.first-topic,pl.second-topic,pl.hermes.third-topic",
        "first|pl.first-topic",
        "second|pl.second-topic",
        "third|pl.hermes.third-topic",
        "hermes|pl.hermes.third-topic",
        "PL.FIRST-TOPIC|pl.first-topic",
        "non-existing|",
      },
      delimiter = '|')
  public void shouldFindTopicsByVariousNameQueries(String query, String expected) {
    // given
    String[] expectedItemNames = transformStringToArray(expected);
    createTopic("pl.first-topic");
    createTopic("pl.second-topic");
    createTopic("pl.hermes.third-topic");

    // when
    SearchResults results = search(query);

    // then
    assertThat(results).containsExactlyByNameInAnyOrder(expectedItemNames);
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        "1234|pl.first-topic",
        "3456|pl.second-topic",
        "34|pl.first-topic,pl.second-topic",
        "99|",
      },
      delimiter = '|')
  public void shouldFindTopicsByVariousOwnerQueries(String query, String expected) {
    // given
    String[] expectedItemNames = transformStringToArray(expected);
    Topic topic1 =
        TopicBuilder.topic("pl.first-topic").withOwner(new OwnerId("Plaintext", "1234")).build();
    Topic topic2 =
        TopicBuilder.topic("pl.second-topic").withOwner(new OwnerId("Plaintext", "3456")).build();
    hermes.initHelper().createTopic(topic1);
    hermes.initHelper().createTopic(topic2);

    // when
    SearchResults results = search(query);

    // then
    assertThat(results).containsExactlyByNameInAnyOrder(expectedItemNames);
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        "first-subscription|first-subscription",
        "first-sub|first-subscription",
        "second-subscription|second-subscription",
        "sub|first-subscription,second-subscription",
        "SUB|first-subscription,second-subscription",
        "non-existing|",
      },
      delimiter = '|')
  public void shouldFindSubscriptionByVariousNameQueries(String query, String expected) {
    // given
    String[] expectedItemNames = transformStringToArray(expected);
    createTopic(FIRST_TOPIC_QUALIFIED_NAME);
    createSubscription(FIRST_TOPIC_QUALIFIED_NAME, "first-subscription");
    createSubscription(FIRST_TOPIC_QUALIFIED_NAME, "second-subscription");

    // when
    SearchResults results = search(query);

    // then
    assertThat(results).containsExactlyByNameInAnyOrder(expectedItemNames);
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        "1234|first-subscription",
        "3456|second-subscription",
        "34|first-subscription,second-subscription",
        "99|",
      },
      delimiter = '|')
  public void shouldFindSubscriptionByVariousOwnerQueries(String query, String expected) {
    // given
    String[] expectedItemNames = transformStringToArray(expected);
    createTopic(FIRST_TOPIC_QUALIFIED_NAME);
    hermes
        .initHelper()
        .createSubscription(
            SubscriptionBuilder.subscription(FIRST_TOPIC_QUALIFIED_NAME, "first-subscription")
                .withOwner(new OwnerId("Plaintext", "1234"))
                .build());
    hermes
        .initHelper()
        .createSubscription(
            SubscriptionBuilder.subscription(FIRST_TOPIC_QUALIFIED_NAME, "second-subscription")
                .withOwner(new OwnerId("Plaintext", "3456"))
                .build());

    // when
    SearchResults results = search(query);

    // then
    assertThat(results).containsExactlyByNameInAnyOrder(expectedItemNames);
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        "https://localhost/ev/event1|first-subscription",
        "https://localhost/ev/event2|second-subscription",
        "localhost/ev/event1|first-subscription",
        "localhost|first-subscription,second-subscription",
        "localhost/non-existing|",
      },
      delimiter = '|')
  public void shouldFindSubscriptionByVariousEndpointQueries(String query, String expected) {
    // given
    String[] expectedItemNames = transformStringToArray(expected);
    createTopic(FIRST_TOPIC_QUALIFIED_NAME);
    hermes
        .initHelper()
        .createSubscription(
            SubscriptionBuilder.subscription(FIRST_TOPIC_QUALIFIED_NAME, "first-subscription")
                .withEndpoint("https://localhost/ev/event1")
                .build());
    hermes
        .initHelper()
        .createSubscription(
            SubscriptionBuilder.subscription(FIRST_TOPIC_QUALIFIED_NAME, "second-subscription")
                .withEndpoint("https://localhost/ev/event2")
                .build());

    // when
    SearchResults results = search(query);

    // then
    assertThat(results).containsExactlyByNameInAnyOrder(expectedItemNames);
  }

  @Test
  public void shouldNotFindAnythingForQueryThatDoesNotMatchAnything() {
    // given
    createTopic(FIRST_TOPIC_QUALIFIED_NAME);
    createSubscription(FIRST_TOPIC_QUALIFIED_NAME, FIRST_SUBSCRIPTION_NAME);

    // when
    SearchResults results = search("non-existing-topic-or-subscription");

    // then
    assertThat(results).hasNoResults();
  }

  @Test
  public void shouldNotFindTopicAfterItWasDeleted() {
    // given
    Topic topic1 = createTopic(FIRST_TOPIC_QUALIFIED_NAME);
    Topic topic2 = createTopic(SECOND_TOPIC_QUALIFIED_NAME);

    // when
    SearchResults resultsBeforeDeletion = search("topic");

    // then: verify topic is found
    assertThat(resultsBeforeDeletion)
        .hasExactNumberOfResults(2)
        .containsItemForTopic(topic1)
        .containsItemForTopic(topic2);

    // when
    hermes.api().deleteTopic(topic2.getQualifiedName()).expectStatus().isOk();
    SearchResults resultsAfterDeletion = search("topic");

    // then: verify only the first topic is found
    assertThat(resultsAfterDeletion)
        .hasExactNumberOfResults(1)
        .containsOnlySingleItemForTopic(topic1);
  }

  @Test
  public void shouldNotFindSubscriptionAfterItWasDeleted() {
    // given
    createTopic(FIRST_TOPIC_QUALIFIED_NAME);
    Subscription subscription1 =
        createSubscription(FIRST_TOPIC_QUALIFIED_NAME, FIRST_SUBSCRIPTION_NAME);
    Subscription subscription2 =
        createSubscription(FIRST_TOPIC_QUALIFIED_NAME, SECOND_SUBSCRIPTION_NAME);

    // when
    SearchResults resultsBeforeDeletion = search("subscription");

    // then: verify subscription is found
    assertThat(resultsBeforeDeletion)
        .hasExactNumberOfResults(2)
        .containsItemForSubscription(subscription1)
        .containsItemForSubscription(subscription2);

    // when
    hermes
        .api()
        .deleteSubscription(FIRST_TOPIC_QUALIFIED_NAME, SECOND_SUBSCRIPTION_NAME)
        .expectStatus()
        .isOk();
    SearchResults resultsAfterDeletion = search("subscription");

    // then: verify only the first subscription is found
    assertThat(resultsAfterDeletion).containsOnlySingleItemForSubscription(subscription1);
  }

  @Test
  public void shouldFindTopicByItsUpdatedOwnerIdAfterItWasUpdated() {
    // given
    OwnerId oldOwnerId = new OwnerId("Plaintext", "1234");
    Topic topic = TopicBuilder.topic(FIRST_TOPIC_QUALIFIED_NAME).withOwner(oldOwnerId).build();
    hermes.initHelper().createTopic(topic);

    // when
    SearchResults resultsBeforeUpdate = search(oldOwnerId.getId());

    // then
    assertThat(resultsBeforeUpdate).containsOnlySingleItemForTopic(topic);

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
    Topic topic = createTopic(FIRST_TOPIC_QUALIFIED_NAME);
    Subscription subscription =
        SubscriptionBuilder.subscription(FIRST_TOPIC_QUALIFIED_NAME, FIRST_SUBSCRIPTION_NAME)
            .withEndpoint(oldEndpoint)
            .build();
    hermes.initHelper().createSubscription(subscription);

    // when
    SearchResults resultsBeforeUpdate = search(oldEndpoint);

    // then
    assertThat(resultsBeforeUpdate).containsItemForSubscription(subscription);

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

  private Topic createTopic(String qualifiedName) {
    return hermes.initHelper().createTopic(TopicBuilder.topic(qualifiedName).build());
  }

  private Subscription createSubscription(String topicQualifiedName, String subscriptionName) {
    return hermes
        .initHelper()
        .createSubscription(
            SubscriptionBuilder.subscription(topicQualifiedName, subscriptionName).build());
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

  private String[] transformStringToArray(String str) {
    return str == null || str.isEmpty() ? new String[] {} : str.split(",");
  }
}
