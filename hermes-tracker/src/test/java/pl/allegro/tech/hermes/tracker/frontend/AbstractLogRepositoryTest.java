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

import static java.lang.System.currentTimeMillis;
import static pl.allegro.tech.hermes.api.PublishedMessageTraceStatus.ERROR;
import static pl.allegro.tech.hermes.api.PublishedMessageTraceStatus.INFLIGHT;
import static pl.allegro.tech.hermes.api.PublishedMessageTraceStatus.SUCCESS;

@Listeners({RetryListener.class})
public abstract class AbstractLogRepositoryTest {

    private LogRepository logRepository;
    
    @BeforeSuite
    public void setUpRetry(ITestContext context) {
        for (ITestNGMethod method : context.getAllTestMethods()) {
            method.setRetryAnalyzerClass(Retry.class);
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
        String hostname = "172.16.254.1";

        // when
        logRepository.logPublished(id, currentTimeMillis(), topic, hostname);

        // then
        awaitUntilMessageIsPersisted(topic, id, SUCCESS, hostname);
    }

    @Test
    public void shouldLogError() throws Exception {
        // given
        String id = "errorMessage";
        String topic = "group.sentMessage";
        String hostname = "172.16.254.1";

        // when
        logRepository.logError(id, currentTimeMillis(), topic, "reason", hostname);

        // then
        awaitUntilMessageIsPersisted(topic, id, ERROR, "reason", hostname);
    }

    @Test
    public void shouldLogInflight() throws Exception {
        // given
        String id = "inflightMessage";
        String topic = "group.sentMessage";
        String hostname = "172.16.254.1";

        // when
        logRepository.logInflight(id, currentTimeMillis(), topic, hostname);

        // then
        awaitUntilMessageIsPersisted(topic, id, INFLIGHT, hostname);
    }

    protected abstract void awaitUntilMessageIsPersisted(String topic, String id, PublishedMessageTraceStatus status, String remoteHostname) throws Exception;

    protected abstract void awaitUntilMessageIsPersisted(String topic, String id, PublishedMessageTraceStatus status, String reason, String remoteHostname)
            throws Exception;

}