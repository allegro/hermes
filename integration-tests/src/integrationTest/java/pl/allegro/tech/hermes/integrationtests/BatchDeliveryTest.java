package pl.allegro.tech.hermes.integrationtests;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.stream;
import static org.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.api.BatchSubscriptionPolicy.Builder.batchSubscriptionPolicy;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.integrationtests.assertions.HermesAssertions.assertThatMetrics;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.BatchSubscriptionPolicy;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

public class BatchDeliveryTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  @RegisterExtension
  public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

  static final AvroUser BOB = new AvroUser("Bob", 50, "blue");

  static final AvroUser ALICE = new AvroUser("Alice", 20, "magenta");

  private static final TestMessage[] SMALL_BATCH = TestMessage.simpleMessages(2);

  private static final TestMessage SINGLE_MESSAGE = TestMessage.simple();

  private static final TestMessage SINGLE_MESSAGE_FILTERED = BOB.asTestMessage();

  private static final MessageFilterSpecification MESSAGE_NAME_FILTER =
      new MessageFilterSpecification(of("type", "jsonpath", "path", ".name", "matcher", "^Bob.*"));

  @Test
  public void shouldDeliverMessagesInBatch() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    TestSubscriber subscriber = subscribers.createSubscriber();

    Subscription subscription =
        subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint())
            .withSubscriptionPolicy(
                buildBatchPolicy()
                    .withBatchSize(2)
                    .withBatchTime(Integer.MAX_VALUE)
                    .withBatchVolume(1024)
                    .build())
            .build();

    hermes.initHelper().createSubscription(subscription);

    // when
    publishAll(topic.getQualifiedName(), SMALL_BATCH);

    // then
    expectSingleBatch(subscriber, SMALL_BATCH);
  }

  @Test
  public void shouldFilterIncomingEventsForBatch() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    final Subscription subscription =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint())
                    .withSubscriptionPolicy(
                        buildBatchPolicy()
                            .withBatchSize(2)
                            .withBatchTime(3)
                            .withBatchVolume(1024)
                            .build())
                    .withFilter(MESSAGE_NAME_FILTER)
                    .build());

    // when
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), BOB.asJson());
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), ALICE.asJson());

    // then
    expectSingleBatch(subscriber, SINGLE_MESSAGE_FILTERED);
    waitAtMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                hermes
                    .api()
                    .getConsumersMetrics()
                    .expectStatus()
                    .isOk()
                    .expectBody(String.class)
                    .value(
                        (body) ->
                            assertThatMetrics(body)
                                .contains("hermes_consumers_subscription_filtered_out_total")
                                .withLabels(
                                    "group", topic.getName().getGroupName(),
                                    "subscription", subscription.getName(),
                                    "topic", topic.getName().getName())
                                .withValue(1.0)));
  }

  @Test
  public void shouldCommitFilteredMessagesForBatch() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    final Subscription subscription =
        hermes
            .initHelper()
            .createSubscription(
                subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint())
                    .withSubscriptionPolicy(
                        buildBatchPolicy()
                            .withBatchSize(10)
                            .withBatchTime(Integer.MAX_VALUE)
                            .withBatchVolume(1024)
                            .build())
                    .withFilter(MESSAGE_NAME_FILTER)
                    .build());

    // when
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), ALICE.asJson());
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), ALICE.asJson());
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), ALICE.asJson());

    // then
    waitAtMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                hermes
                    .api()
                    .getConsumersMetrics()
                    .expectStatus()
                    .isOk()
                    .expectBody(String.class)
                    .value(
                        (body) ->
                            assertThatMetrics(body)
                                .contains("hermes_consumers_subscription_filtered_out_total")
                                .withLabels(
                                    "group", topic.getName().getGroupName(),
                                    "subscription", subscription.getName(),
                                    "topic", topic.getName().getName())
                                .withValue(3.0)));
    hermes.api().waitUntilConsumerCommitsOffset(topic.getQualifiedName(), subscription.getName());
  }

  @Test
  public void shouldDeliverBatchInGivenTimePeriod() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    TestSubscriber subscriber = subscribers.createSubscriber();

    Subscription subscription =
        subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint())
            .withSubscriptionPolicy(
                buildBatchPolicy()
                    .withBatchSize(100)
                    .withBatchTime(1)
                    .withBatchVolume(1024)
                    .build())
            .build();

    hermes.initHelper().createSubscription(subscription);

    // when
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), SINGLE_MESSAGE.body());

    // then
    expectSingleBatch(subscriber, SINGLE_MESSAGE);
  }

  @Test
  public void shouldDeliverBatchInGivenVolume() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    TestSubscriber subscriber = subscribers.createSubscriber();

    int batchVolumeThatFitsOneMessageOnly = 150;

    Subscription subscription =
        subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint())
            .withSubscriptionPolicy(
                buildBatchPolicy()
                    .withBatchSize(100)
                    .withBatchTime(Integer.MAX_VALUE)
                    .withBatchVolume(batchVolumeThatFitsOneMessageOnly)
                    .build())
            .build();

    hermes.initHelper().createSubscription(subscription);

    // when publishing more than buffer capacity
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), SINGLE_MESSAGE.body());
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), SINGLE_MESSAGE.body());

    // then we expect to receive batch that has desired batch volume (one message only)
    expectSingleBatch(subscriber, SINGLE_MESSAGE);
  }

  @Test
  public void shouldDeliverAvroMessagesAsJsonBatch() {
    // given
    AvroUser user = new AvroUser("Bob", 50, "blue");

    Topic topic =
        hermes
            .initHelper()
            .createTopicWithSchema(
                topicWithSchema(topicWithRandomName().build(), user.getSchemaAsString()));

    TestSubscriber subscriber = subscribers.createSubscriber();

    Subscription subscription =
        subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint())
            .withSubscriptionPolicy(
                buildBatchPolicy()
                    .withBatchSize(2)
                    .withBatchTime(Integer.MAX_VALUE)
                    .withBatchVolume(1024)
                    .build())
            .build();

    hermes.initHelper().createSubscription(subscription);

    TestMessage[] avroBatch = {user.asTestMessage(), user.asTestMessage()};

    // when
    publishAll(topic.getQualifiedName(), avroBatch);

    // then
    expectSingleBatch(subscriber, avroBatch);
  }

  @Test
  public void shouldPassSubscriptionHeaders() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    TestSubscriber subscriber = subscribers.createSubscriber();

    BatchSubscriptionPolicy policy =
        buildBatchPolicy().withBatchSize(100).withBatchTime(1).withBatchVolume(1024).build();
    Subscription subscription =
        subscription(topic, "batchSubscription")
            .withEndpoint(subscriber.getEndpoint())
            .withContentType(ContentType.JSON)
            .withSubscriptionPolicy(policy)
            .withHeader("MY-HEADER", "myHeaderValue")
            .withHeader("MY-OTHER-HEADER", "myOtherHeaderValue")
            .build();

    hermes.initHelper().createSubscription(subscription);

    // when
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), SINGLE_MESSAGE.body());

    // then
    subscriber.waitUntilRequestReceived(
        request -> {
          Assertions.assertThat(request.getHeader("MY-HEADER")).isEqualTo("myHeaderValue");
          Assertions.assertThat(request.getHeader("MY-OTHER-HEADER"))
              .isEqualTo("myOtherHeaderValue");
        });
  }

  @Test
  public void shouldAttachSubscriptionIdentityHeadersWhenItIsEnabled() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

    TestSubscriber subscriber = subscribers.createSubscriber();

    BatchSubscriptionPolicy policy =
        buildBatchPolicy().withBatchSize(100).withBatchTime(1).withBatchVolume(1024).build();
    Subscription subscription =
        subscription(topic, "batchSubscription")
            .withEndpoint(subscriber.getEndpoint())
            .withContentType(ContentType.JSON)
            .withSubscriptionPolicy(policy)
            .withAttachingIdentityHeadersEnabled(true)
            .build();

    hermes.initHelper().createSubscription(subscription);

    // when
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), SINGLE_MESSAGE.body());

    // then
    subscriber.waitUntilRequestReceived(
        request -> {
          Assertions.assertThat(request.getHeader("Hermes-Topic-Name"))
              .isEqualTo(topic.getQualifiedName());
          Assertions.assertThat(request.getHeader("Hermes-Subscription-Name"))
              .isEqualTo("batchSubscription");
        });
  }

  @Test
  public void shouldNotAttachSubscriptionIdentityHeadersWhenItIsDisabled() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

    TestSubscriber subscriber = subscribers.createSubscriber();

    BatchSubscriptionPolicy policy =
        buildBatchPolicy().withBatchSize(100).withBatchTime(1).withBatchVolume(1024).build();

    Subscription subscription =
        subscription(topic, "batchSubscription")
            .withEndpoint(subscriber.getEndpoint())
            .withContentType(ContentType.JSON)
            .withSubscriptionPolicy(policy)
            .withAttachingIdentityHeadersEnabled(false)
            .build();

    hermes.initHelper().createSubscription(subscription);

    // when
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), SINGLE_MESSAGE.body());

    // then
    subscriber.waitUntilRequestReceived(
        request -> {
          Assertions.assertThat(request.getHeader("Hermes-Topic-Name")).isNull();
          Assertions.assertThat(request.getHeader("Hermes-Subscription-Name")).isNull();
        });
  }

  @Test
  public void shouldTimeoutRequestToSlowlyRespondingClient() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

    // response chunk every 500ms, total 5s
    int chunks = 10;
    int totalResponseDuration = 5000;

    TestSubscriber subscriber =
        subscribers.createSubscriber(
            (service, endpoint) -> {
              service.addStubMapping(
                  post(urlEqualTo(endpoint))
                      .inScenario("slowAndFast")
                      .whenScenarioStateIs(STARTED)
                      .willSetStateTo("slow")
                      .willReturn(
                          aResponse()
                              .withStatus(200)
                              .withBody("I am very slow!")
                              .withChunkedDribbleDelay(chunks, totalResponseDuration))
                      .build());

              service.addStubMapping(
                  post(urlEqualTo(endpoint))
                      .inScenario("slowAndFast")
                      .whenScenarioStateIs("slow")
                      .willReturn(aResponse().withStatus(200).withFixedDelay(0))
                      .build());
            });

    Subscription subscription =
        subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint())
            .withSubscriptionPolicy(
                buildBatchPolicy()
                    .withBatchSize(1)
                    .withBatchTime(1)
                    .withBatchVolume(1024)
                    .withRequestTimeout(1000)
                    .build())
            .build();

    hermes.initHelper().createSubscription(subscription);

    // when
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), SINGLE_MESSAGE.body());

    // then
    // first request is retried because of timeout (with socket / idle timeout only, the request
    // wouldn't be timed out because
    // there are chunks of response every 500ms which is smaller than 1s timeout)
    subscriber.waitUntilReceived(Duration.ofSeconds(5), 2);
    Assertions.assertThat(subscriber.getLastReceivedRequest().getHeader("Hermes-Retry-Count"))
        .isEqualTo("1");
  }

  private void publishAll(String topicQualifiedName, TestMessage... messages) {
    stream(messages)
        .forEach(message -> hermes.api().publishUntilSuccess(topicQualifiedName, message.body()));
  }

  private void expectSingleBatch(TestSubscriber subscriber, TestMessage... expectedContents) {
    subscriber.waitUntilRequestReceived(
        message -> {
          List<Map<String, Object>> batch = readBatch(message.getBodyAsString());
          Assertions.assertThat(batch).hasSize(expectedContents.length);
          for (int i = 0; i < expectedContents.length; i++) {
            Assertions.assertThat(batch.get(i).get("message"))
                .isEqualTo(expectedContents[i].getContent());
            Assertions.assertThat((String) ((Map) batch.get(i).get("metadata")).get("id"))
                .isNotEmpty();
          }
        });
  }

  private BatchSubscriptionPolicy.Builder buildBatchPolicy() {
    return batchSubscriptionPolicy()
        .applyDefaults()
        .withMessageTtl(100)
        .withRequestTimeout(100)
        .withMessageBackoff(10);
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> readBatch(String message) {
    try {
      return mapper.readValue(message, List.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
