package pl.allegro.tech.hermes.tracker.elasticsearch.management;

import org.junit.ClassRule;
import org.junit.Test;
import pl.allegro.tech.hermes.api.MessageTrace;
import pl.allegro.tech.hermes.api.PublishedMessageTrace;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;
import pl.allegro.tech.hermes.tracker.consumers.TestMessageMetadata;
import pl.allegro.tech.hermes.tracker.elasticsearch.DataInitializer;
import pl.allegro.tech.hermes.tracker.elasticsearch.ElasticsearchResource;
import pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware;
import pl.allegro.tech.hermes.tracker.management.LogRepository;

import java.util.ArrayList;
import java.util.List;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_MINUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.DISCARDED;

public class ElasticsearchLogRepositoryTest implements LogSchemaAware {

    private static final String CLUSTER_NAME = "primary";
    private static final String REASON_MESSAGE = "Bad Request";

    @ClassRule
    public static ElasticsearchResource elasticsearch = new ElasticsearchResource(SENT_INDEX, PUBLISHED_INDEX);

    private DataInitializer dataInitializer = new DataInitializer(elasticsearch.client(), CLUSTER_NAME);
    private LogRepository logRepository = new ElasticsearchLogRepository(elasticsearch.client());

    @Test
    public void shouldGetLastUndelivered() throws Exception {
        //given
        MessageMetadata messageMetadata = TestMessageMetadata.of("1234", "elasticsearch.lastUndelivered", "subscription");
        long timestamp = System.currentTimeMillis();

        dataInitializer.indexSentMessage(messageMetadata, timestamp, DISCARDED, REASON_MESSAGE);

        //when & then
        assertThat(fetchUndelivered(messageMetadata)).containsExactly(sentMessageTrace(messageMetadata, timestamp));
    }

    @Test
    public void shouldGetMessageStatus() throws Exception {
        //given
        MessageMetadata messageMetadata = TestMessageMetadata.of("1234", "elasticsearch.messageStatus", "subscription");
        long timestamp = System.currentTimeMillis();

        PublishedMessageTraceStatus publishedStatus = PublishedMessageTraceStatus.SUCCESS;
        dataInitializer.indexPublishedMessage(messageMetadata, timestamp, publishedStatus);

        SentMessageTraceStatus sentStatus = SentMessageTraceStatus.SUCCESS;
        dataInitializer.indexSentMessage(messageMetadata, timestamp, sentStatus, REASON_MESSAGE);

        //when
        assertThat(fetchMessageStatus(messageMetadata))
                .contains(publishedMessageTrace(messageMetadata, timestamp, publishedStatus))
                .contains(sentMessageTrace(messageMetadata, timestamp));
    }

    private List<SentMessageTrace> fetchUndelivered(MessageMetadata messageMetadata) {
        final List<SentMessageTrace> lastUndelivered = new ArrayList<>();

        await().atMost(ONE_MINUTE).until(() -> {
            lastUndelivered.clear();
            lastUndelivered.addAll(logRepository.getLastUndeliveredMessages(messageMetadata.getTopic(), messageMetadata.getSubscription(), 10));
            return lastUndelivered.size() == 1;
        });
        return lastUndelivered;
    }

    private List<MessageTrace> fetchMessageStatus(MessageMetadata messageMetadata) {
        List<MessageTrace> status = new ArrayList<>();

        await().atMost(ONE_MINUTE).until(() -> {
            status.clear();
            status.addAll(logRepository.getMessageStatus(messageMetadata.getTopic(), messageMetadata.getSubscription(), messageMetadata.getId()));
            return status.size() == 2;
        });

        return status;
    }

    private SentMessageTrace sentMessageTrace(MessageMetadata messageMetadata, long timestamp) {
        return new SentMessageTrace(messageMetadata.getId(),
                timestamp,
                messageMetadata.getSubscription(),
                messageMetadata.getTopic(),
                DISCARDED,
                REASON_MESSAGE,
                null,
                messageMetadata.getPartition(),
                messageMetadata.getOffset(),
                CLUSTER_NAME);
    }

    private PublishedMessageTrace publishedMessageTrace(MessageMetadata messageMetadata, long timestamp, PublishedMessageTraceStatus publishedStatus) {
        return new PublishedMessageTrace(messageMetadata.getId(),
                timestamp,
                messageMetadata.getTopic(),
                publishedStatus,
                null,
                null,
                CLUSTER_NAME);
    }
}