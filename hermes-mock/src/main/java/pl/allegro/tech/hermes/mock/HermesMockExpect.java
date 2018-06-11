package pl.allegro.tech.hermes.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.awaitility.core.ConditionTimeoutException;
import org.apache.avro.Schema;

import java.io.IOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

class HermesMockExpect {
    private WireMockServer wireMockServer;
    private int awaitSeconds;
    private HermesMockHelper hermesMockHelper;

    public HermesMockExpect(WireMockServer wireMockServer, int awaitSeconds, HermesMockHelper hermesMockHelper) {
        this.wireMockServer = wireMockServer;
        this.awaitSeconds = awaitSeconds;
        this.hermesMockHelper = hermesMockHelper;
    }

    public void singleMessageOnTopic(String topicName) {
        messagesOnTopic(1, topicName);
    }

    public <T> void singleJsonMessageOnTopicAs(String topicName, Class<T> clazz) {
        jsonMessagesOnTopicAs(1, topicName, clazz);
    }

    public void singleAvroMessageOnTopic(String topicName, Schema schema) {
        avroMessagesOnTopic(1, topicName, schema);
    }

    public void messagesOnTopic(int count, String topicName) {
        try {
            await().atMost(awaitSeconds, SECONDS).until(() ->
                    wireMockServer.verify(count, postRequestedFor(urlEqualTo("/topics/" + topicName)))
            );
        } catch (Exception ex) {
            throw new HermesMockException("Hermes mock did not receive " + count + " messages. ", ex);
        }
    }

    public <T> void jsonMessagesOnTopicAs(int count, String topicName, Class<T> clazz) {
        try {
            await().atMost(awaitSeconds, SECONDS).until(() ->
                    wireMockServer.verify(count, postRequestedFor(urlEqualTo("/topics/" + topicName)))
            );
        } catch (ConditionTimeoutException ex) {
            throw new HermesMockException("Hermes mock did not received " + count + " messages. ", ex);
        }
        List<T> allMessages = allJsonMessagesAs(topicName, clazz);
        if (allMessages.size() != count) {
            throw new HermesMockException("Hermes mock did not received " + count + " messages, got " + allMessages.size());
        }
    }

    public void avroMessagesOnTopic(int count, String topicName, Schema schema) {
        try {
            await().atMost(awaitSeconds, SECONDS).until(() ->
                    wireMockServer.verify(count, postRequestedFor(urlEqualTo("/topics/" + topicName)))
            );
        } catch (ConditionTimeoutException ex) {
            throw new HermesMockException("Hermes mock did not received " + count + " messages. ", ex);
        }
        List<byte[]> allAvroMessagesAs = allAvroMessagesAs(topicName);
        if (allAvroMessagesAs.size() != count) {
            throw new HermesMockException("Hermes mock did not received " + count + " messages, got " + allAvroMessagesAs.size());
        }
    }

    private List<byte[]> allAvroMessagesAs(String topicName) {
        return getAllRequests(topicName).stream()
                .map(LoggedRequest::getBody)
                .collect(toList());
    }

    private <T> List<T> allJsonMessagesAs(String topicName, Class<T> clazz) {
        return getAllRequests(topicName).stream()
                .map(req -> hermesMockHelper.deserializeJson(req.getBody(), clazz))
                .collect(toList());
    }

    private List<LoggedRequest> getAllRequests(String topicName) {
        return wireMockServer.findAll(postRequestedFor(urlEqualTo("/topics/" + topicName)));
    }
}
