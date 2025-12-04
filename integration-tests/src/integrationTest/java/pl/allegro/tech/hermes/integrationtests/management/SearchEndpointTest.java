package pl.allegro.tech.hermes.integrationtests.management;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.management.assertions.SearchResultsAssertion.assertThat;

import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.SearchResults;
import pl.allegro.tech.hermes.integrationtests.prometheus.PrometheusExtension;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
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
  private static final String FIRST_TOPIC_QUALIFIED_NAME =
      FIRST_GROUP_NAME + "." + FIRST_TOPIC_NAME;

  @BeforeEach
  public void setup() {
    hermes.clearManagementData();
  }

  @Test
  public void shouldReturnEmptyResultWhenCachesAreEmpty() {
    // when
    SearchResults results = search("");

    // then
    assertThat(results).hasNoResults();
  }

  @Test
  public void shouldFindTopicWhenOnlyTopicsCacheIsNotEmpty() {
    // given
    hermes
        .initHelper()
        .createTopic(TopicBuilder.topic(FIRST_TOPIC_QUALIFIED_NAME).build());

    // when
    SearchResults results = search(FIRST_TOPIC_QUALIFIED_NAME);

    // then
    Awaitility.waitAtMost(Duration.ofSeconds(5))
        .untilAsserted(() -> {
          assertThat(results)
              .containsSingleTopicItemWithName(FIRST_TOPIC_QUALIFIED_NAME);
        });

  }

//  @Test
//  public void shouldFindSubscriptionWhenOnlySubscriptionsCacheIsNotEmpty() {
//    // given
//    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
//    Subscription subscription = hermes.initHelper().createSubscription(topic, "subscription");
//
//    // when
//    List<Map> results =
//        hermes
//            .api()
//            .search(subscription.getName())
//            .expectStatus()
//            .isOk()
//            .expectBodyList(Map.class)
//            .returnResult()
//            .getResponseBody();
//
//    // then
//    assertThat(results.stream().map(it -> it.get("name")).collect(Collectors.toList()))
//        .containsOnly(subscription.getQualifiedName());
//    assertThat(results.stream().map(it -> it.get("type")).collect(Collectors.toList()))
//        .containsOnly("subscription");
//  }
//
//  @Test
//  public void shouldFindTopicAndSubscriptionWhenBothCachesAreNotEmpty() {
//    // given
//    Topic topic = hermes.initHelper().createTopic(TopicBuilder.topic("group", "testTopic").build());
//    Subscription subscription = hermes.initHelper().createSubscription(topic, "testSubscription");
//
//    // when
//    List<Map> results =
//        hermes
//            .api()
//            .search("test")
//            .expectStatus()
//            .isOk()
//            .expectBodyList(Map.class)
//            .returnResult()
//            .getResponseBody();
//
//    // then
//    assertThat(results.stream().map(it -> it.get("name")).collect(Collectors.toList()))
//        .containsOnly(topic.getQualifiedName(), subscription.getQualifiedName());
//    assertThat(results.stream().map(it -> it.get("type")).collect(Collectors.toList()))
//        .containsOnly("topic", "subscription");
//  }
//
//  @Test
//  public void shouldNotFindAnythingForNonExistingQuery() {
//    // given
//    hermes.initHelper().createTopic(topicWithRandomName().build());
//    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
//    hermes.initHelper().createSubscription(topic, "subscription");
//
//    // when
//    List<Map> results =
//        hermes
//            .api()
//            .search("non-existing-query")
//            .expectStatus()
//            .isOk()
//            .expectBodyList(Map.class)
//            .returnResult()
//            .getResponseBody();
//
//    // then
//    assertThat(results).isEmpty();
//  }
//
//  @Test
//  public void shouldFindTopicAfterItWasCreated() {
//    // given
//    String topicName = "my-topic";
//    assertThat(
//            hermes
//                .api()
//                .search(topicName)
//                .expectStatus()
//                .isOk()
//                .expectBodyList(Map.class)
//                .returnResult()
//                .getResponseBody())
//        .isEmpty();
//
//    // when
//    Topic topic = hermes.initHelper().createTopic(TopicBuilder.topic("group", topicName).build());
//
//    // then
//    List<Map> results =
//        hermes
//            .api()
//            .search(topicName)
//            .expectStatus()
//            .isOk()
//            .expectBodyList(Map.class)
//            .returnResult()
//            .getResponseBody();
//    assertThat(results.stream().map(it -> it.get("name")).collect(Collectors.toList()))
//        .containsOnly(topic.getQualifiedName());
//  }
//
//  @Test
//  public void shouldNotFindTopicAfterItWasDeleted() {
//    // given
//    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
//    assertThat(
//            hermes
//                .api()
//                .search(topic.getName().getTopicName())
//                .expectStatus()
//                .isOk()
//                .expectBodyList(Map.class)
//                .returnResult()
//                .getResponseBody())
//        .hasSize(1);
//
//    // when
//    hermes.api().deleteTopic(topic.getQualifiedName()).expectStatus().isOk();
//
//    // then
//    assertThat(
//            hermes
//                .api()
//                .search(topic.getName().getTopicName())
//                .expectStatus()
//                .isOk()
//                .expectBodyList(Map.class)
//                .returnResult()
//                .getResponseBody())
//        .isEmpty();
//  }
//
//  @Test
//  public void shouldFindSubscriptionAfterItWasCreated() {
//    // given
//    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
//    String subscriptionName = "my-subscription";
//    assertThat(
//            hermes
//                .api()
//                .search(subscriptionName)
//                .expectStatus()
//                .isOk()
//                .expectBodyList(Map.class)
//                .returnResult()
//                .getResponseBody())
//        .isEmpty();
//
//    // when
//    Subscription subscription = hermes.initHelper().createSubscription(topic, subscriptionName);
//
//    // then
//    List<Map> results =
//        hermes
//            .api()
//            .search(subscriptionName)
//            .expectStatus()
//            .isOk()
//            .expectBodyList(Map.class)
//            .returnResult()
//            .getResponseBody();
//    assertThat(results.stream().map(it -> it.get("name")).collect(Collectors.toList()))
//        .containsOnly(subscription.getQualifiedName());
//  }
//
//  @Test
//  public void shouldNotFindSubscriptionAfterItWasDeleted() {
//    // given
//    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
//    Subscription subscription = hermes.initHelper().createSubscription(topic, "subscription");
//    assertThat(
//            hermes
//                .api()
//                .search(subscription.getName())
//                .expectStatus()
//                .isOk()
//                .expectBodyList(Map.class)
//                .returnResult()
//                .getResponseBody())
//        .hasSize(1);
//
//    // when
//    hermes
//        .api()
//        .deleteSubscription(topic.getQualifiedName(), subscription.getName())
//        .expectStatus()
//        .isOk();
//
//    // then
//    assertThat(
//            hermes
//                .api()
//                .search(subscription.getName())
//                .expectStatus()
//                .isOk()
//                .expectBodyList(Map.class)
//                .returnResult()
//                .getResponseBody())
//        .isEmpty();
//  }
//
//  @Test
//  public void shouldFindSubscriptionByItsNewNameAfterItWasUpdated() {
//    // given
//    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
//    Subscription subscription = hermes.initHelper().createSubscription(topic, "old-name");
//    assertThat(
//            hermes
//                .api()
//                .search("old-name")
//                .expectStatus()
//                .isOk()
//                .expectBodyList(Map.class)
//                .returnResult()
//                .getResponseBody())
//        .hasSize(1);
//    assertThat(
//            hermes
//                .api()
//                .search("new-name")
//                .expectStatus()
//                .isOk()
//                .expectBodyList(Map.class)
//                .returnResult()
//                .getResponseBody())
//        .isEmpty();
//
//    // when
//    Subscription updatedSubscription = Subscription.from(subscription).withName("new-name").build();
//    hermes
//        .api()
//        .updateSubscription(topic.getQualifiedName(), "old-name", updatedSubscription)
//        .expectStatus()
//        .isOk();
//
//    // then
//    assertThat(
//            hermes
//                .api()
//                .search("old-name")
//                .expectStatus()
//                .isOk()
//                .expectBodyList(Map.class)
//                .returnResult()
//                .getResponseBody())
//        .isEmpty();
//    List<Map> results =
//        hermes
//            .api()
//            .search("new-name")
//            .expectStatus()
//            .isOk()
//            .expectBodyList(Map.class)
//            .returnResult()
//            .getResponseBody();
//    assertThat(results.stream().map(it -> it.get("name")).collect(Collectors.toList()))
//        .containsOnly(updatedSubscription.getQualifiedName());
//  }

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
