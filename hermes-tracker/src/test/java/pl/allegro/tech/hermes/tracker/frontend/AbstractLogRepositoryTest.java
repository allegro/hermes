package pl.allegro.tech.hermes.tracker.frontend;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static java.lang.System.currentTimeMillis;

public abstract class AbstractLogRepositoryTest {

    private LogRepository logRepository;

    @Before
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
        String datacenter = "dc1";
        Map<String, String> extraRequestHeaders = ImmutableMap.of("header1", "value1", "header2", "value2");

        // when
        logRepository.logPublished(id, currentTimeMillis(), topic, hostname, datacenter, extraRequestHeaders);

        // then
        awaitUntilSuccessMessageIsPersisted(topic, id, hostname, datacenter, "header1", "value1", "header2", "value2");
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
        awaitUntilErrorMessageIsPersisted(topic, id, "reason", hostname, "header1", "value1", "header2", "value2");
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
        awaitUntilInflightMessageIsPersisted(topic, id, hostname, "header1", "value1", "header2", "value2");
    }

    protected abstract void awaitUntilSuccessMessageIsPersisted(
            String topic,
            String id,
            String remoteHostname,
            String storageDatacenter,
            String... extraRequestHeadersKeywords
    )
            throws Exception;

    protected abstract void awaitUntilInflightMessageIsPersisted(
            String topic,
            String id,
            String remoteHostname,
            String... extraRequestHeadersKeywords
    )
            throws Exception;


    protected abstract void awaitUntilErrorMessageIsPersisted(
            String topic,
            String id,
            String reason,
            String remoteHostname,
            String... extraRequestHeadersKeywords
    )
            throws Exception;
}