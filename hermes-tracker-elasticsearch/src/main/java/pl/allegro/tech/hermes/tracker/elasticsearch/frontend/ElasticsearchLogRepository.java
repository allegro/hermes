package pl.allegro.tech.hermes.tracker.elasticsearch.frontend;

import com.codahale.metrics.MetricRegistry;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.tracker.BatchingLogRepository;
import pl.allegro.tech.hermes.tracker.elasticsearch.ElasticsearchQueueCommitter;
import pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware;
import pl.allegro.tech.hermes.tracker.elasticsearch.metrics.Gauges;
import pl.allegro.tech.hermes.tracker.elasticsearch.metrics.Timers;
import pl.allegro.tech.hermes.tracker.frontend.LogRepository;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static pl.allegro.tech.hermes.api.PublishedMessageTraceStatus.*;
import static pl.allegro.tech.hermes.tracker.elasticsearch.DocumentBuilder.build;

public class ElasticsearchLogRepository extends BatchingLogRepository<XContentBuilder> implements LogRepository, LogSchemaAware {

    public ElasticsearchLogRepository(Client elasticClient, String clusterName,
                                      int queueSize, int commitInterval,
                                      MetricRegistry metricRegistry, PathsCompiler pathsCompiler) {
        super(queueSize, clusterName, metricRegistry, pathsCompiler);

        registerQueueSizeGauge(Gauges.PRODUCER_TRACKER_ELASTICSEARCH_QUEUE_SIZE);
        registerRemainingCapacityGauge(Gauges.PRODUCER_TRACKER_ELASTICSEARCH_REMAINING_CAPACITY);

        ElasticsearchQueueCommitter.scheduleCommitAtFixedRate(queue, PUBLISHED_INDEX, PUBLISHED_TYPE, elasticClient,
                metricRegistry.timer(pathsCompiler.compile(Timers.PRODUCER_TRACKER_ELASTICSEARCH_COMMIT_LATENCY)), commitInterval);
    }

    @Override
    public void logPublished(String messageId, long timestamp, String topicName) {
        queue.offer(build(() -> document(messageId, timestamp, topicName, SUCCESS)));
    }

    @Override
    public void logError(String messageId, long timestamp, String topicName, String reason) {
        queue.offer(build(() -> document(messageId, timestamp, topicName, ERROR, reason)));
    }

    @Override
    public void logInflight(String messageId, long timestamp, String topicName) {
        queue.offer(build(() -> document(messageId, timestamp, topicName, INFLIGHT)));
    }

    private XContentBuilder document(String messageId, long timestamp, String topicName, PublishedMessageTraceStatus status)
            throws IOException {
        return notEndedDocument(messageId, timestamp, topicName, status.toString()).endObject();
    }

    private XContentBuilder document(String messageId, long timestamp, String topicName, PublishedMessageTraceStatus status, String reason)
            throws IOException {
        return notEndedDocument(messageId, timestamp, topicName, status.toString()).field(REASON, reason).endObject();
    }

    protected XContentBuilder notEndedDocument(String messageId, long timestamp, String topicName, String status)
            throws IOException {
        return jsonBuilder()
                .startObject()
                .field(MESSAGE_ID, messageId)
                .field(TIMESTAMP, timestamp)
                .field(TOPIC_NAME, topicName)
                .field(STATUS, status)
                .field(CLUSTER, clusterName);
    }
}
