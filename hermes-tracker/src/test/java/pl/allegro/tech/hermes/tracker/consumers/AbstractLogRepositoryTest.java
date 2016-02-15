package pl.allegro.tech.hermes.tracker.consumers;

import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.test.helper.retry.Retry;
import pl.allegro.tech.hermes.test.helper.retry.RetryListener;

import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.DISCARDED;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.INFLIGHT;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.SUCCESS;

@Listeners({RetryListener.class})
public abstract class AbstractLogRepositoryTest {

    private static final String SUBSCRIPTION = "subscription";

    private LogRepository logRepository;

    @BeforeSuite
    public void setUpRetry(ITestContext context) {
        for (ITestNGMethod method : context.getAllTestMethods()) {
            method.setRetryAnalyzer(new Retry());
        }
    }

    @BeforeTest
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

    @Test
    public void shouldLogBatchIdInSentMessage() throws Exception {
        // given
        String messageId = "messageId";
        String batchId = "batchId";
        String topic = "group.sentBatchMessage";

        // when
        logRepository.logSuccessful(TestMessageMetadata.of(messageId, batchId, topic, SUBSCRIPTION), 1234L);

        // then
        awaitUntilBatchMessageIsPersisted(topic, SUBSCRIPTION, messageId, batchId, SUCCESS);
    }

    protected abstract void awaitUntilMessageIsPersisted(String topic, String subscription, String id, SentMessageTraceStatus status) throws Exception;

    protected abstract void awaitUntilBatchMessageIsPersisted(String topic, String subscription, String messageId, String batchId, SentMessageTraceStatus status) throws Exception;
}
