package pl.allegro.tech.hermes.tracker.elasticsearch.frontend;

import com.codahale.metrics.MetricRegistry;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.tracker.BatchingLogRepository;
import pl.allegro.tech.hermes.tracker.elasticsearch.*;
import pl.allegro.tech.hermes.tracker.elasticsearch.metrics.Gauges;
import pl.allegro.tech.hermes.tracker.elasticsearch.metrics.Timers;
import pl.allegro.tech.hermes.tracker.frontend.LogRepository;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static pl.allegro.tech.hermes.api.PublishedMessageTraceStatus.ERROR;
import static pl.allegro.tech.hermes.api.PublishedMessageTraceStatus.INFLIGHT;
import static pl.allegro.tech.hermes.api.PublishedMessageTraceStatus.SUCCESS;
import static pl.allegro.tech.hermes.tracker.elasticsearch.ElasticsearchDocument.build;

public class FrontendElasticsearchLogRepository extends BatchingLogRepository<ElasticsearchDocument> implements LogRepository, LogSchemaAware {

    private static final int DOCUMENT_EXPECTED_SIZE = 1024;

    private FrontendElasticsearchLogRepository(Client elasticClient,
                                               String clusterName,
                                               String hostname,
                                               int queueSize,
                                               int commitInterval,
                                               IndexFactory indexFactory,
                                               String typeName,
                                               MetricRegistry metricRegistry,
                                               PathsCompiler pathsCompiler) {
        super(queueSize, clusterName, hostname, metricRegistry, pathsCompiler);

        registerQueueSizeGauge(Gauges.PRODUCER_TRACKER_ELASTICSEARCH_QUEUE_SIZE);
        registerRemainingCapacityGauge(Gauges.PRODUCER_TRACKER_ELASTICSEARCH_REMAINING_CAPACITY);

        ElasticsearchQueueCommitter.scheduleCommitAtFixedRate(queue, indexFactory, typeName, elasticClient,
                metricRegistry.timer(pathsCompiler.compile(Timers.PRODUCER_TRACKER_ELASTICSEARCH_COMMIT_LATENCY)), commitInterval);
    }

    @Override
    public void logPublished(String messageId, long timestamp, String topicName, String hostname) {
        queue.offer(build(() -> document(messageId, timestamp, topicName, SUCCESS, hostname)));
    }

    @Override
    public void logError(String messageId, long timestamp, String topicName, String reason, String hostname) {
        queue.offer(build(() -> document(messageId, timestamp, topicName, ERROR, reason, hostname)));
    }

    @Override
    public void logInflight(String messageId, long timestamp, String topicName, String hostname) {
        queue.offer(build(() -> document(messageId, timestamp, topicName, INFLIGHT, hostname)));
    }

    private XContentBuilder document(String messageId,
                                     long timestamp,
                                     String topicName,
                                     PublishedMessageTraceStatus status,
                                     String hostname)
            throws IOException {
        return notEndedDocument(messageId, timestamp, topicName, status.toString(), hostname).endObject();
    }

    private XContentBuilder document(String messageId,
                                     long timestamp,
                                     String topicName,
                                     PublishedMessageTraceStatus status,
                                     String reason,
                                     String hostname)
            throws IOException {
        return notEndedDocument(messageId, timestamp, topicName, status.toString(), hostname).field(REASON, reason).endObject();
    }

    protected XContentBuilder notEndedDocument(String messageId, long timestamp, String topicName, String status, String hostname)
            throws IOException {
        return jsonBuilder(new BytesStreamOutput(DOCUMENT_EXPECTED_SIZE))
                .startObject()
                .field(MESSAGE_ID, messageId)
                .field(TIMESTAMP, timestamp)
                .field(TOPIC_NAME, topicName)
                .field(STATUS, status)
                .field(CLUSTER, clusterName)
                .field(SOURCE_HOSTNAME, this.hostname)
                .field(REMOTE_HOSTNAME, hostname);
    }

    public static class Builder {

        private Client elasticClient;
        private String clusterName = "primary";
        private String hostName = "unknown";
        private int queueSize = 1000;
        private int commitInterval = 100;
        private FrontendIndexFactory indexFactory = new FrontendDailyIndexFactory();
        private String typeName = SchemaManager.PUBLISHED_TYPE;

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

        public Builder withHostName(String hostName) {
            this.hostName = hostName;
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

        public Builder withIndexFactory(FrontendIndexFactory indexFactory) {
            this.indexFactory = indexFactory;
            return this;
        }

        public FrontendElasticsearchLogRepository build() {
            return new FrontendElasticsearchLogRepository(elasticClient,
                    clusterName,
                    hostName,
                    queueSize,
                    commitInterval,
                    indexFactory,
                    typeName,
                    metricRegistry,
                    pathsCompiler);
        }
    }
}
