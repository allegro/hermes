package pl.allegro.tech.hermes.tracker.frontend;

import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.test.helper.retry.RetryListener;
import pl.allegro.tech.hermes.test.helper.retry.Retry;

import static pl.allegro.tech.hermes.api.PublishedMessageTraceStatus.ERROR;
import static pl.allegro.tech.hermes.api.PublishedMessageTraceStatus.INFLIGHT;
import static pl.allegro.tech.hermes.api.PublishedMessageTraceStatus.SUCCESS;

@Listeners({RetryListener.class})
public abstract class AbstractLogRepositoryTest {

    private LogRepository logRepository;

    @BeforeSuite
    public void setUpRetry(ITestContext context) {
        for (ITestNGMethod method : context.getAllTestMethods()) {
            method.setRetryAnalyzer(new Retry());
        }
    }

    @BeforeTest
    public void setup() {
        logRepository = createRepository();
    }

    protected abstract LogRepository createRepository();

    @Test
    public void shouldLogPublished() throws Exception {
        // given
        String id = "publishedMessage";
        String topic = "group.sentMessage";

        // when
        logRepository.logPublished(id, 1234L, topic);

        // then
        awaitUntilMessageIsPersisted(topic, id, SUCCESS);
    }

    @Test
    public void shouldLogError() throws Exception {
        // given
        String id = "errorMessage";
        String topic = "group.sentMessage";

        // when
        logRepository.logError(id, 1234L, topic, "reason");

        // then
        awaitUntilMessageIsPersisted(topic, id, ERROR, "reason");
    }

    @Test
    public void shouldLogInflight() throws Exception {
        // given
        String id = "inflightMessage";
        String topic = "group.sentMessage";

        // when
        logRepository.logInflight(id, 1234L, topic);

        // then
        awaitUntilMessageIsPersisted(topic, id, INFLIGHT);
    }

    protected abstract void awaitUntilMessageIsPersisted(String topic, String id, PublishedMessageTraceStatus status) throws Exception;

    protected abstract void awaitUntilMessageIsPersisted(String topic, String id, PublishedMessageTraceStatus status, String reason)
            throws Exception;

}