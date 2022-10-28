package pl.allegro.tech.hermes.tracker.frontend;

import com.google.common.collect.ImmutableMap;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.test.helper.retry.Retry;
import pl.allegro.tech.hermes.test.helper.retry.RetryListener;

import java.util.Map;

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
        Map<String, String> extraRequestHeaders = ImmutableMap.of("header1", "value1", "header2", "value2");

        // when
        logRepository.logPublished(id, currentTimeMillis(), topic, hostname, extraRequestHeaders);

        // then
        awaitUntilMessageIsPersisted(topic, id, hostname, SUCCESS, "header1", "value1", "header2", "value2");
    }

    @Test
    public void shouldLogError() throws Exception {
        // given
        String id = "errorMessage";
        String topic = "group.sentMessage";
        String hostname = "172.16.254.1";
        Map<String, String> extraRequestHeaders = ImmutableMap.of("header1", "value1", "header2", "value2");

        // when
        logRepository.logError(id, currentTimeMillis(), topic, "reason", hostname, extraRequestHeaders);

        // then
        awaitUntilMessageIsPersisted(topic, id, "reason", hostname, ERROR, "header1", "value1", "header2", "value2");
    }

    @Test
    public void shouldLogInflight() throws Exception {
        // given
        String id = "inflightMessage";
        String topic = "group.sentMessage";
        String hostname = "172.16.254.1";
        Map<String, String> extraRequestHeaders = ImmutableMap.of("header1", "value1", "header2", "value2");

        // when
        logRepository.logInflight(id, currentTimeMillis(), topic, hostname, extraRequestHeaders);

        // then
        awaitUntilMessageIsPersisted(topic, id, hostname, INFLIGHT, "header1", "value1", "header2", "value2");
    }

    protected abstract void awaitUntilMessageIsPersisted(
        String topic,
        String id,
        String remoteHostname,
        PublishedMessageTraceStatus status,
        String... extraRequestHeadersKeywords
    )
        throws Exception;

    protected abstract void awaitUntilMessageIsPersisted(
        String topic,
        String id,
        String reason,
        String remoteHostname,
        PublishedMessageTraceStatus status,
        String... extraRequestHeadersKeywords
    )
        throws Exception;
}