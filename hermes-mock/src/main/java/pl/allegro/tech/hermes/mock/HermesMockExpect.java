package pl.allegro.tech.hermes.mock;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import static com.jayway.awaitility.Awaitility.await;
import com.jayway.awaitility.core.ConditionTimeoutException;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import org.apache.avro.Schema;

import java.util.List;
import java.util.function.Supplier;

public class HermesMockExpect {
    private final HermesMockHelper hermesMockHelper;
    private final int awaitSeconds;

    public HermesMockExpect(HermesMockHelper hermesMockHelper, int awaitSeconds) {
        this.hermesMockHelper = hermesMockHelper;
        this.awaitSeconds = awaitSeconds;
    }

    public void singleMessageOnTopic(String topicName) {
        messagesOnTopic(topicName, 1);
    }

    public <T> void singleJsonMessageOnTopicAs(String topicName, Class<T> clazz) {
        jsonMessagesOnTopicAs(topicName, 1, clazz);
    }

    public void singleAvroMessageOnTopic(String topicName, Schema schema) {
        avroMessagesOnTopic(topicName, 1, schema);
    }

    public void messagesOnTopic(String topicName, int count) {
        expectMessages(topicName, count);
    }

    public <T> void jsonMessagesOnTopicAs(String topicName, int count, Class<T> clazz) {
        assertMessages(topicName, count, () -> allJsonMessagesAs(topicName, clazz));
    }

    public void avroMessagesOnTopic(String topicName, int count, Schema schema) {
        assertMessages(topicName, count, () -> validateAvroMessages(topicName, schema));
    }

    private <T> void assertMessages(String topicName, int count, Supplier<List<T>> messages) {
        expectMessages(topicName, count);
        expectSpecificMessages(count, messages.get());
    }

    private void expectMessages(String topicName, int count) {
        try {
            await().atMost((long)awaitSeconds, SECONDS).until(() -> hermesMockHelper.verifyRequest(count, topicName));
        } catch (ConditionTimeoutException ex) {
            throw new HermesMockException("Hermes mock did not receive " + count + " messages.", ex);
        }
    }

    private <T> void expectSpecificMessages(int count, List<T> messages) {
        if (messages != null && messages.size() != count) {
            throw new HermesMockException("Hermes mock did not receive " + count + " messages, got " + messages.size());
        }
    }

    private <T> List<byte[]> validateAvroMessages(String topicName, Schema schema) {
        return getAllRequests(topicName).stream()
                .map(LoggedRequest::getBody)
                .peek(raw -> hermesMockHelper.validateAvroSchema(raw, schema))
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
