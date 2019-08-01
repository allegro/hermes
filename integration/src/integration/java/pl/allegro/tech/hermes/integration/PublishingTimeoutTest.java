package pl.allegro.tech.hermes.integration;

import com.googlecode.catchexception.CatchException;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.client.SlowClient;

import java.io.IOException;

import static com.googlecode.catchexception.CatchException.catchException;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class PublishingTimeoutTest extends IntegrationTest {

    private SlowClient client;

    @BeforeClass
    public void initialize() {
        this.client = new SlowClient();
    }

    @Test
    public void shouldHandleRequestTimeout() throws IOException, InterruptedException {
        // given
        Topic topic = operations.buildTopic(randomTopic("timeoutGroup", "timeoutTopic").build());
        int clientTimeout = 5000;
        int pauseTimeBetweenChunks = 300;
        int delayBeforeSendingFirstData = 0;

        // when
        long start = System.currentTimeMillis();
        String response = client.slowEvent(
            clientTimeout, pauseTimeBetweenChunks, delayBeforeSendingFirstData, topic.getQualifiedName()
        );
        long elapsed = System.currentTimeMillis() - start;

        //then
        assertThat(response).contains("408 Request Time-out");
        assertThat(elapsed).isLessThan(2500);
    }

    @Test
    public void shouldCloseConnectionAfterSendingDelayData() throws IOException, InterruptedException {
        //given
        Topic topic = operations.buildTopic(randomTopic("timeoutGroup", "closeConnectionTopic").build());
        int clientTimeout = 5000;
        int pauseTimeBetweenChunks = 0;
        int delayBeforeSendingFirstData = 3000;

        //when
        catchException(client).slowEvent(
            clientTimeout, pauseTimeBetweenChunks, delayBeforeSendingFirstData, topic.getQualifiedName()
        );

        //then
        LoggerFactory.getLogger(PublishingTimeoutTest.class).error("Caught exception", CatchException.<Exception>caughtException());
        assertThat(CatchException.<Exception>caughtException()).hasMessageContaining("Broken pipe");
    }

    @Test
    public void shouldHandleTimeoutForSlowRequestWithChunkedEncoding() throws IOException, InterruptedException {
        // given
        Topic topic = operations.buildTopic(randomTopic("slowGroup", "chunkedEncoding").build());
        int clientTimeout = 5000;
        int pauseTimeBetweenChunks = 300;
        int delayBeforeSendingFirstData = 0;
        boolean chunkedEncoding = true;

        // when
        long start = System.currentTimeMillis();
        String response = client.slowEvent(
                clientTimeout, pauseTimeBetweenChunks, delayBeforeSendingFirstData, topic.getQualifiedName(), chunkedEncoding);
        long elapsed = System.currentTimeMillis() - start;

        // then
        assertThat(response).contains("408 Request Time-out");
        assertThat(elapsed).isLessThan(2500);
    }

}
