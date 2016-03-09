package pl.allegro.tech.hermes.tracker.elasticsearch.consumers;

import com.codahale.metrics.MetricRegistry;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.tracker.BatchingLogRepository;
import pl.allegro.tech.hermes.tracker.consumers.LogRepository;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;
import pl.allegro.tech.hermes.tracker.elasticsearch.*;
import pl.allegro.tech.hermes.tracker.elasticsearch.metrics.Gauges;
import pl.allegro.tech.hermes.tracker.elasticsearch.metrics.Timers;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.DISCARDED;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.FAILED;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.INFLIGHT;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.SUCCESS;
import static pl.allegro.tech.hermes.tracker.elasticsearch.ElasticsearchDocument.build;

public class ConsumersElasticsearchLogRepository extends BatchingLogRepository<ElasticsearchDocument> implements LogRepository, LogSchemaAware {

    private static final int DOCUMENT_EXPECTED_SIZE = 1024;

    private ConsumersElasticsearchLogRepository(Client elasticClient, String clusterName, int queueSize, int commitInterval,
                                                IndexFactory indexFactory, String typeName, MetricRegistry metricRegistry, PathsCompiler pathsCompiler) {
        super(queueSize, clusterName, metricRegistry, pathsCompiler);

        registerQueueSizeGauge(Gauges.CONSUMER_TRACKER_ELASTICSEARCH_QUEUE_SIZE);
        registerRemainingCapacityGauge(Gauges.CONSUMER_TRACKER_ELASTICSEARCH_REMAINING_CAPACITY);

        ElasticsearchQueueCommitter.scheduleCommitAtFixedRate(queue, indexFactory, typeName, elasticClient,
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

    private ElasticsearchDocument document(MessageMetadata message, long createdAt, SentMessageTraceStatus status) {
        return build(() -> notEndedDocument(message, createdAt, status.toString()).endObject());
    }

    private ElasticsearchDocument document(MessageMetadata message, long timestamp, SentMessageTraceStatus status, String reason) {
        return build(() -> notEndedDocument(message, timestamp, status.toString()).field(REASON, reason).endObject());
    }

    protected XContentBuilder notEndedDocument(MessageMetadata message, long timestamp, String status)
            throws IOException {
        return jsonBuilder(new BytesStreamOutput(DOCUMENT_EXPECTED_SIZE))
                .startObject()
                .field(MESSAGE_ID, message.getMessageId())
                .field(BATCH_ID, message.getBatchId())
                .field(TIMESTAMP, timestamp)
                .field(PUBLISH_TIMESTAMP, message.getPublishingTimestamp())
                .field(TOPIC_NAME, message.getTopic())
                .field(SUBSCRIPTION, message.getSubscription())
                .field(STATUS, status)
                .field(OFFSET, message.getOffset())
                .field(PARTITION, message.getPartition())
                .field(CLUSTER, clusterName);
    }

    public static class Builder {

        private Client elasticClient;
        private String clusterName = "primary";
        private int queueSize = 1000;
        private int commitInterval = 100;
        private ConsumersIndexFactory indexFactory = new ConsumersDailyIndexFactory();
        private String typeName = SchemaManager.SENT_TYPE;

        private final MetricRegistry metricRegistry;
        private final PathsCompiler pathsCompiler;

        public Builder(Client elasticClient, PathsCompiler pathsCompiler, MetricRegistry metricRegistry) {
            this.elasticClient = elasticClient;
            this.pathsCompiler = pathsCompiler;
            this.metricRegistry = metricRegistry;
        }

        public Builder withElasticClient(Client elasticClient) {
            this.elasticClient = elasticClient;
            return this;
        }

        public Builder withClusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        public Builder withQueueSize(int queueSize) {
            this.queueSize = queueSize;
            return this;
        }

        public Builder withCommitInterval(int commitInterval) {
            this.commitInterval = commitInterval;
            return this;
        }

        public Builder withTypeName(String typeName) {
            this.typeName = typeName;
            return this;
        }

        public Builder withIndexFactory(ConsumersIndexFactory indexFactory) {
            this.indexFactory = indexFactory;
            return this;
        }

        public ConsumersElasticsearchLogRepository build() {
            return new ConsumersElasticsearchLogRepository(elasticClient,
                    clusterName,
                    queueSize,
                    commitInterval,
                    indexFactory,
                    typeName,
                    metricRegistry,
                    pathsCompiler);
        }
    }
}
