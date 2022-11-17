package pl.allegro.tech.hermes.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.Comparator.comparingLong;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class BatchRetryPolicyTest extends IntegrationTest {

    public static final String HEALTHY = "healthy";

    private WireMockServer remoteServer;
    private WireMock wireMock;

    String failedRequestBody = "{\"body\":\"failed\"}";
    String successfulRequestBody = "{\"body\":\"successful\"}";

    ObjectMapper mapper = new ObjectMapper();

    @BeforeMethod
    public void beforeMethod() {
        wireMock.resetScenarios();
    }

    @BeforeClass
    public void beforeClass() {
        remoteServer = new WireMockServer(Ports.nextAvailable());
        remoteServer.start();

        wireMock = new WireMock("localhost", remoteServer.port());
    }

    @AfterClass
    public void afterClass() {
        remoteServer.stop();
    }

    @Test
    public void shouldRetryUntilRequestSuccessfulAndSendRetryCounterInHeader() throws Throwable {
        //given
        Topic topic = operations.buildTopic(randomTopic("group", "retryUntilRequestSuccessful").build());
        createSingleMessageBatchSubscription(topic);

        wireMock.register(post(topicUrl(topic))
                .inScenario(topic.getQualifiedName())
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(SC_INTERNAL_SERVER_ERROR))
                .willSetStateTo(HEALTHY));

        wireMock.register(post(topicUrl(topic))
                .inScenario(topic.getQualifiedName())
                .whenScenarioStateIs(HEALTHY)
                .willReturn(aResponse().withStatus(SC_CREATED)));

        //when
        publish(topic, TestMessage.simple());

        //then
        wait.until(() -> {
            List<LoggedRequest> requests = recordedRequests(topic);
            assertThat(requests).hasSize(2);
            assertThat(requests.get(0).header("Hermes-Retry-Count").containsValue("0")).isTrue();
            assertThat(requests.get(1).header("Hermes-Retry-Count").containsValue("1")).isTrue();
        });
    }

    @Test
    public void shouldNotRetryIfRequestSuccessful() throws Throwable {
        //given
        Topic topic = operations.buildTopic(randomTopic("group", "notRetryIfRequestSuccessful").build());
        createSingleMessageBatchSubscription(topic);

        wireMock.register(post(topicUrl(topic)).willReturn(aResponse().withStatus(SC_CREATED)));

        //when
        publish(topic, TestMessage.simple());

        //then
        wait.until(() -> assertThat(recordedRequests(topic)).hasSize(1));
    }

    @Test
    public void shouldRetryUntilTtlExceeded() throws Throwable {
        //given
        Topic topic = operations.buildTopic(randomTopic("group", "retryUntilTtlExceeded").build());
        createSingleMessageBatchSubscription(topic, 1, 10);

        wireMock.register(post(topicUrl(topic))
                .withRequestBody(containing("failed"))
                .willReturn(aResponse().withStatus(SC_INTERNAL_SERVER_ERROR)));

        wireMock.register(post(topicUrl(topic))
                .withRequestBody(containing("successful"))
                .willReturn(aResponse().withStatus(SC_CREATED)));


        //when
        publishRequestThatIsExpectedToFail(topic);
        publishRequestThatIsExpectedToSucceed(topic);

        //then
        wait.until(() ->
            assertThat(recordedRequests(topic))
                    .extracting(LoggedRequest::getBodyAsString)
                    .extracting(this::readMessage)
                    .containsSequence(failedRequestBody, failedRequestBody, successfulRequestBody));
    }

    private void publishRequestThatIsExpectedToSucceed(Topic topic) {
        assertThat(publisher.publish(topic.getQualifiedName(), successfulRequestBody)).hasStatus(CREATED);
    }

    @Test
    public void shouldRetryOnClientErrors() throws Throwable {
        //given
        Topic topic = operations.buildTopic(randomTopic("group", "retryOnClientErrors").build());
        createSingleMessageBatchSubscription(topic, true);

        wireMock.register(post(topicUrl(topic))
                .inScenario(topic.getQualifiedName())
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(SC_BAD_REQUEST))
                .willSetStateTo(HEALTHY));

        wireMock.register(post(topicUrl(topic))
                .inScenario(topic.getQualifiedName())
                .whenScenarioStateIs(HEALTHY)
                .willReturn(aResponse().withStatus(SC_CREATED)));

        //when
        publish(topic, TestMessage.simple());

        //then
        wait.until(() -> assertThat(recordedRequests(topic)).hasSize(2));
    }

    @Test
    public void shouldNotRetryOnClientErrors() throws Throwable {
        //given
        Topic topic = operations.buildTopic(randomTopic("group", "notRetryOnClientErrors").build());
        createSingleMessageBatchSubscription(topic, false);

        wireMock.register(post(topicUrl(topic))
                .withRequestBody(containing("failed"))
                .willReturn(aResponse().withStatus(SC_BAD_REQUEST)));

        wireMock.register(post(topicUrl(topic))
                .withRequestBody(containing("successful"))
                .willReturn(aResponse().withStatus(SC_CREATED)));

        //when
        publishRequestThatIsExpectedToFail(topic);
        publishRequestThatIsExpectedToSucceed(topic);

        //then
        wait.until(() ->
                assertThat(recordedRequests(topic))
                        .extracting(LoggedRequest::getBodyAsString)
                        .extracting(this::readMessage)
                        .containsExactly(failedRequestBody, successfulRequestBody));
    }

    private void publishRequestThatIsExpectedToFail(Topic topic) {
        assertThat(publisher.publish(topic.getQualifiedName(), failedRequestBody)).hasStatus(CREATED);
        wait.until(() -> assertThat(recordedRequests(topic).size()).isPositive());
    }

    private UrlPattern topicUrl(Topic topic) {
        return topicUrl(topic.getName().getName());
    }

    private UrlPattern topicUrl(String topicName) {
        return urlEqualTo("/" + topicName);
    }

    private List<LoggedRequest> recordedRequests(Topic topic) {
        List<LoggedRequest> requests = wireMock.find(postRequestedFor(topicUrl(topic)));
        requests.sort(comparingLong(req -> req.getLoggedDate().getTime()));
        return requests;
    }

    private void publish(Topic topic, TestMessage m) {
        assertThat(publisher.publish(topic.getQualifiedName(), m.body())).hasStatus(CREATED);
    }

    private void createSingleMessageBatchSubscription(Topic topic) {
        createSingleMessageBatchSubscription(topic, false);
    }

    private void createSingleMessageBatchSubscription(Topic topic, int messageTtl, int messageBackoff) {
        operations.createBatchSubscription(
                topic, subscriptionEndpoint(topic.getName().getName()), messageTtl, messageBackoff, 1, 1, 200, false
        );
    }

    private void createSingleMessageBatchSubscription(Topic topic, boolean retryOnClientErrors) {
        operations.createBatchSubscription(topic, subscriptionEndpoint(topic.getName().getName()), 1, 10, 1, 1, 500, retryOnClientErrors);
    }

    private String subscriptionEndpoint(String topicName) {
        return "http://localhost:" + remoteServer.port() + "/" + topicName;
    }

    private String readMessage(String body) {
        try {
            return mapper.writeValueAsString(((Map) mapper.readValue(body, List.class).get(0)).get("message"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
