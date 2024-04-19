package pl.allegro.tech.hermes.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.BatchSubscriptionPolicy;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.apache.hc.core5.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.hc.core5.http.HttpStatus.SC_CREATED;
import static org.apache.hc.core5.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static pl.allegro.tech.hermes.api.BatchSubscriptionPolicy.Builder.batchSubscriptionPolicy;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class BatchRetryPolicyTest {

    private static final String HEALTHY = "healthy";
    private static final String failedRequestBody = "{\"body\":\"failed\"}";
    private static final String successfulRequestBody = "{\"body\":\"successful\"}";

    private final ObjectMapper mapper = new ObjectMapper();

    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension();

    @RegisterExtension
    public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

    @Test
    public void shouldRetryUntilRequestSuccessfulAndSendRetryCounterInHeader() {
        //given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

        TestSubscriber subscriber = subscribers.createSubscriber((service, endpoint) -> {
            service.addStubMapping(post(endpoint)
                    .inScenario(topic.getQualifiedName())
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse().withStatus(SC_INTERNAL_SERVER_ERROR))
                    .willSetStateTo(HEALTHY)
                    .build()
            );

            service.addStubMapping(post(endpoint)
                    .inScenario(topic.getQualifiedName())
                    .whenScenarioStateIs(HEALTHY)
                    .willReturn(aResponse().withStatus(SC_CREATED)).build());
        });

        createSingleMessageBatchSubscription(topic, subscriber.getEndpoint());

        //when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), TestMessage.simple().body());

        //then
        subscriber.waitUntilRequestsReceived(requests -> {
            Assertions.assertThat(requests).hasSize(2);
            Assertions.assertThat(requests.get(0).header("Hermes-Retry-Count").containsValue("0")).isTrue();
            Assertions.assertThat(requests.get(1).header("Hermes-Retry-Count").containsValue("1")).isTrue();
        });
    }

    @Test
    public void shouldNotRetryIfRequestSuccessful() {
        //given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestMessage message = TestMessage.simple();

        TestSubscriber subscriber = subscribers.createSubscriber();

        createSingleMessageBatchSubscription(topic, subscriber.getEndpoint());

        //when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        //then
        subscriber.waitUntilReceived(Duration.ofSeconds(5), 1);
    }

    @Test
    public void shouldRetryUntilTtlExceeded() {
        //given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

        TestSubscriber subscriber = subscribers.createSubscriber((service, endpoint) -> {
            service.addStubMapping((post(endpoint))
                    .withRequestBody(containing("failed"))
                    .willReturn(aResponse().withStatus(SC_INTERNAL_SERVER_ERROR)).build());

            service.addStubMapping((post(endpoint))
                    .withRequestBody(containing("successful"))
                    .willReturn(aResponse().withStatus(SC_CREATED)).build());
        });

        createSingleMessageBatchSubscription(topic, subscriber.getEndpoint(), 1, 10);

        //when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), failedRequestBody);
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), successfulRequestBody);

        //then
        subscriber.waitUntilRequestsReceived(requests ->
                Assertions.assertThat(requests)
                        .extracting(LoggedRequest::getBodyAsString)
                        .extracting(this::readMessage)
                        .containsSequence(failedRequestBody, failedRequestBody, successfulRequestBody));
    }

    @Test
    public void shouldRetryOnClientErrors() {
        //given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

        TestSubscriber subscriber = subscribers.createSubscriber((service, endpoint) -> {
            service.addStubMapping(post(endpoint)
                    .inScenario(topic.getQualifiedName())
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse().withStatus(SC_BAD_REQUEST))
                    .willSetStateTo(HEALTHY).build());

            service.addStubMapping(post(endpoint)
                    .inScenario(topic.getQualifiedName())
                    .whenScenarioStateIs(HEALTHY)
                    .willReturn(aResponse().withStatus(SC_CREATED)).build());
        });

        createSingleMessageBatchSubscription(topic, subscriber.getEndpoint(), true);

        //when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), TestMessage.simple().body());

        //then
        subscriber.waitUntilRequestsReceived(requests -> Assertions.assertThat(requests.size()).isEqualTo(2));
    }

    @Test
    public void shouldNotRetryOnClientErrors() {
        //given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());


        TestSubscriber subscriber = subscribers.createSubscriber((service, endpoint) -> {

            service.addStubMapping(post(endpoint)
                    .withRequestBody(containing("failed"))
                    .willReturn(aResponse().withStatus(SC_BAD_REQUEST)).build());

            service.addStubMapping(post(endpoint)
                    .withRequestBody(containing("successful"))
                    .willReturn(aResponse().withStatus(SC_CREATED)).build());
        });

        createSingleMessageBatchSubscription(topic, subscriber.getEndpoint());

        //when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), failedRequestBody);
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), successfulRequestBody);

        //then
        subscriber.waitUntilRequestsReceived(requests ->
                Assertions.assertThat(requests)
                        .extracting(LoggedRequest::getBodyAsString)
                        .extracting(this::readMessage)
                        .containsExactly(failedRequestBody, successfulRequestBody));
    }


    private void createSingleMessageBatchSubscription(Topic topic, String endpoint, int messageTtl, int messageBackoff) {
        createBatchSubscription(
                topic, endpoint, messageTtl, messageBackoff, 1, 1, 200, false
        );
    }

    private void createSingleMessageBatchSubscription(Topic topic, String endpoint) {
        createSingleMessageBatchSubscription(topic, endpoint, false);
    }

    private void createSingleMessageBatchSubscription(Topic topic, String endpoint, boolean retryOnClientErrors) {
        createBatchSubscription(topic, endpoint, 1, 10, 1, 1, 500, retryOnClientErrors);
    }

    public void createBatchSubscription(Topic topic, String endpoint, int messageTtl, int messageBackoff, int batchSize, int batchTime,
                                        int batchVolume, boolean retryOnClientErrors) {
        BatchSubscriptionPolicy policy = batchSubscriptionPolicy()
                .applyDefaults()
                .withMessageTtl(messageTtl)
                .withMessageBackoff(messageBackoff)
                .withBatchSize(batchSize)
                .withBatchTime(batchTime)
                .withBatchVolume(batchVolume)
                .withClientErrorRetry(retryOnClientErrors)
                .withRequestTimeout(500)
                .build();

        createBatchSubscription(topic, endpoint, policy);
    }

    public void createBatchSubscription(Topic topic, String endpoint, BatchSubscriptionPolicy policy) {
        Subscription subscription = subscription(topic, "batchSubscription")
                .withEndpoint(endpoint)
                .withContentType(ContentType.JSON)
                .withSubscriptionPolicy(policy)
                .build();

        hermes.initHelper().createSubscription(subscription);
    }

    private String readMessage(String body) {
        try {
            return mapper.writeValueAsString(((Map) mapper.readValue(body, List.class).get(0)).get("message"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
