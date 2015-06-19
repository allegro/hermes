package pl.allegro.tech.hermes.tracker.elasticsearch.consumers;

import com.codahale.metrics.MetricRegistry;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.tracker.BatchingLogRepository;
import pl.allegro.tech.hermes.tracker.consumers.LogRepository;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;
import pl.allegro.tech.hermes.tracker.elasticsearch.ElasticsearchQueueCommitter;
import pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware;
import pl.allegro.tech.hermes.tracker.elasticsearch.metrics.Gauges;
import pl.allegro.tech.hermes.tracker.elasticsearch.metrics.Timers;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.*;
import static pl.allegro.tech.hermes.tracker.elasticsearch.DocumentBuilder.build;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.TypedIndex.SENT_MESSAGES;

public class ElasticsearchLogRepository extends BatchingLogRepository<XContentBuilder> implements LogRepository, LogSchemaAware {

    public ElasticsearchLogRepository(Client elasticClient, String clusterName,
                                      int queueSize, int commitInterval,
                                      MetricRegistry metricRegistry, PathsCompiler pathsCompiler) {
        super(queueSize, clusterName, metricRegistry, pathsCompiler);

        registerQueueSizeGauge(Gauges.CONSUMER_TRACKER_ELASTICSEARCH_QUEUE_SIZE);
        registerRemainingCapacityGauge(Gauges.CONSUMER_TRACKER_ELASTICSEARCH_REMAINING_CAPACITY);

        ElasticsearchQueueCommitter.scheduleCommitAtFixedRate(queue, SENT_MESSAGES, elasticClient,
                metricRegistry.timer(pathsCompiler.compile(Timers.CONSUMER_TRACKER_ELASTICSEARCH_COMMIT_LATENCY)), commitInterval);
    }

    @Override
    public void logSuccessful(MessageMetadata message, long timestamp) {
        queue.offer(document(message, timestamp, SUCCESS));
    }

    @Override
    public void logFailed(MessageMetadata message, long timestamp, String reason) {
        queue.offer(document(message, timestamp, FAILED, reason));
    }

    @Override
    public void logDiscarded(MessageMetadata message, long timestamp, String reason) {
        queue.offer(document(message, timestamp, DISCARDED, reason));
    }

    @Override
    public void logInflight(MessageMetadata message, long timestamp) {
        queue.offer(document(message, timestamp, INFLIGHT));
    }

    private XContentBuilder document(MessageMetadata message, long createdAt, SentMessageTraceStatus status) {
        return build(() -> notEndedDocument(message, createdAt, status.toString()).endObject());
    }

    private XContentBuilder document(MessageMetadata message, long timestamp, SentMessageTraceStatus status, String reason) {
        return build(() -> notEndedDocument(message, timestamp, status.toString()).field(REASON, reason).endObject());
    }

    protected XContentBuilder notEndedDocument(MessageMetadata message, long timestamp, String status)
            throws IOException {
        return jsonBuilder()
                .startObject()
                .field(MESSAGE_ID, message.getId())
                .field(TIMESTAMP, timestamp)
                .field(PUBLISH_TIMESTAMP, message.getPublishingTimestamp().orElse(null))
                .field(TOPIC_NAME, message.getTopic())
                .field(SUBSCRIPTION, message.getSubscription())
                .field(STATUS, status)
                .field(OFFSET, message.getOffset())
                .field(PARTITION, message.getPartition())
                .field(CLUSTER, clusterName);
    }

}
