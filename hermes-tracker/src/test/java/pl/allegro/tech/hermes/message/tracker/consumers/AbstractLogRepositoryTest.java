package pl.allegro.tech.hermes.message.tracker.consumers;

import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;

import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.DISCARDED;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.INFLIGHT;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.SUCCESS;

public abstract class AbstractLogRepositoryTest {

    private static final String SUBSCRIPTION = "subscription";

    private LogRepository logRepository;

    @Before
    public void setUp() throws Exception {
        logRepository = createLogRepository();
    }

    protected abstract LogRepository createLogRepository();

    @Test
    public void shouldLogSentMessage() throws Exception {
        // given
        String id = "sentMessage";
        String topic = "group.sentMessage";

        // when
        logRepository.logSuccessful(TestMessageMetadata.of(id, topic, SUBSCRIPTION), 1234L);

        // then
        awaitUntilMessageIsPersisted(topic, SUBSCRIPTION, id, SUCCESS);
    }

    @Test
    public void shouldLogInflightMessage() throws Exception {
        // given
        String id = "inflightMessage";
        String topic = "group.inflightMessage";

        // when
        logRepository.logInflight(TestMessageMetadata.of(id, topic, SUBSCRIPTION), 1234L);

        // then
        awaitUntilMessageIsPersisted(topic, SUBSCRIPTION, id, INFLIGHT);
    }

    @Test
    public void shouldLogUndeliveredMessage() throws Exception {
        // given
        String id = "undeliveredMessage";
        String topic = "group.undeliveredMessage";

        // when
        logRepository.logDiscarded(TestMessageMetadata.of(id, topic, SUBSCRIPTION), 1234L, "reason");

        // then
        awaitUntilMessageIsPersisted(topic, SUBSCRIPTION, id, DISCARDED);
    }

    protected abstract void awaitUntilMessageIsPersisted(String topic, String subscription, String id, SentMessageTraceStatus status) throws Exception;
}
