package pl.allegro.tech.hermes.mock;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.awaitility.core.ConditionTimeoutException;
import org.apache.avro.Schema;

import java.util.List;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

class HermesMockExpect {
    private final HermesMockHelper hermesMockHelper;
    private final int awaitSeconds;

    public HermesMockExpect(HermesMockHelper hermesMockHelper, int awaitSeconds) {
        this.hermesMockHelper = hermesMockHelper;
        this.awaitSeconds = awaitSeconds;
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
        assertMessages(count, topicName, null);
    }

    public <T> void jsonMessagesOnTopicAs(int count, String topicName, Class<T> clazz) {
        assertMessages(count, topicName, () -> allJsonMessagesAs(topicName, clazz));
    }

    public void avroMessagesOnTopic(int count, String topicName, Schema schema) {
        assertMessages(count, topicName, () -> allAvroMessagesAs(topicName));
    }

    private <T> void assertMessages(int count, String topicName, Supplier<List<T>> obj) {
        try {
            await().atMost(awaitSeconds, SECONDS).until(() -> hermesMockHelper.verifyRequest(count, topicName));
        } catch (ConditionTimeoutException ex) {
            throw new HermesMockException("Hermes mock did not received " + count + " messages. ", ex);
        }

        if (obj != null) {
            assertMessagesCount(count, obj.get());
        }
    }

    private <T> void assertMessagesCount(int count, List<T> messages) {
        if (messages != null && messages.size() != count) {
            throw new HermesMockException("Hermes mock did not received " + count + " messages, got " + messages.size());
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
        return hermesMockHelper.findAll(postRequestedFor(urlEqualTo("/topics/" + topicName)));
    }
}
