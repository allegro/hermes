package pl.allegro.tech.hermes.tracker.elasticsearch.consumers;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.DISCARDED;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.FAILED;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.FILTERED;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.INFLIGHT;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.SUCCESS;
import static pl.allegro.tech.hermes.tracker.elasticsearch.ElasticsearchDocument.build;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.common.metric.TrackerElasticSearchMetrics;
import pl.allegro.tech.hermes.tracker.BatchingLogRepository;
import pl.allegro.tech.hermes.tracker.consumers.LogRepository;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;
import pl.allegro.tech.hermes.tracker.elasticsearch.ElasticsearchDocument;
import pl.allegro.tech.hermes.tracker.elasticsearch.ElasticsearchQueueCommitter;
import pl.allegro.tech.hermes.tracker.elasticsearch.IndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware;
import pl.allegro.tech.hermes.tracker.elasticsearch.SchemaManager;

public class ConsumersElasticsearchLogRepository
    extends BatchingLogRepository<ElasticsearchDocument> implements LogRepository, LogSchemaAware {

  private static final int DOCUMENT_EXPECTED_SIZE = 1024;

  private final Client elasticClient;

  private ConsumersElasticsearchLogRepository(
      Client elasticClient,
      String clusterName,
      String hostname,
      int queueSize,
      int commitInterval,
      IndexFactory indexFactory,
      String typeName,
      MetricsFacade metricsFacade) {
    super(queueSize, clusterName, hostname);
    this.elasticClient = elasticClient;
    registerMetrics(commitInterval, indexFactory, typeName, metricsFacade.trackerElasticSearch());
  }

  @Override
  public void logSuccessful(MessageMetadata message, String hostname, long timestamp) {
    queue.offer(
        build(
            () ->
                notEndedDocument(message, timestamp, SUCCESS.toString())
                    .field(REMOTE_HOSTNAME, hostname)
                    .endObject()));
  }

  @Override
  public void logFailed(MessageMetadata message, String hostname, long timestamp, String reason) {
    queue.offer(
        build(
            () ->
                notEndedDocument(message, timestamp, FAILED.toString())
                    .field(REASON, reason)
                    .field(REMOTE_HOSTNAME, hostname)
                    .endObject()));
  }

  @Override
  public void logDiscarded(MessageMetadata message, long timestamp, String reason) {
    queue.offer(document(message, timestamp, DISCARDED, reason));
  }

  @Override
  public void logInflight(MessageMetadata message, long timestamp) {
    queue.offer(document(message, timestamp, INFLIGHT));
  }

  @Override
  public void logFiltered(MessageMetadata message, long timestamp, String reason) {
    queue.offer(document(message, timestamp, FILTERED, reason));
  }

  @Override
  public void close() {
    elasticClient.close();
  }

  private ElasticsearchDocument document(
      MessageMetadata message, long createdAt, SentMessageTraceStatus status) {
    return build(() -> notEndedDocument(message, createdAt, status.toString()).endObject());
  }

  private ElasticsearchDocument document(
      MessageMetadata message, long timestamp, SentMessageTraceStatus status, String reason) {
    return build(
        () ->
            notEndedDocument(message, timestamp, status.toString())
                .field(REASON, reason)
                .endObject());
  }

  protected XContentBuilder notEndedDocument(MessageMetadata message, long timestamp, String status)
      throws IOException {
    return jsonBuilder(new BytesStreamOutput(DOCUMENT_EXPECTED_SIZE))
        .startObject()
        .field(MESSAGE_ID, message.getMessageId())
        .field(BATCH_ID, message.getBatchId())
        .field(TIMESTAMP, timestamp)
        .field(TIMESTAMP_SECONDS, toSeconds(timestamp))
        .field(PUBLISH_TIMESTAMP, message.getPublishingTimestamp())
        .field(TOPIC_NAME, message.getTopic())
        .field(SUBSCRIPTION, message.getSubscription())
        .field(STATUS, status)
        .field(OFFSET, message.getOffset())
        .field(PARTITION, message.getPartition())
        .field(CLUSTER, clusterName)
        .field(SOURCE_HOSTNAME, hostname);
  }

  private void registerMetrics(
      int commitInterval,
      IndexFactory indexFactory,
      String typeName,
      TrackerElasticSearchMetrics trackerMetrics) {
    trackerMetrics.registerConsumerTrackerElasticSearchQueueSizeGauge(
        this.queue, BlockingQueue::size);
    trackerMetrics.registerConsumerTrackerElasticSearchRemainingCapacity(
        this.queue, BlockingQueue::size);

    ElasticsearchQueueCommitter.scheduleCommitAtFixedRate(
        this.queue,
        indexFactory,
        typeName,
        elasticClient,
        trackerMetrics.trackerElasticSearchCommitLatencyTimer(),
        commitInterval);
  }

  private long toSeconds(long millis) {
    return millis / 1000;
  }

  public static class Builder {

    private Client elasticClient;
    private String clusterName = "primary";
    private String hostName = "unknown";
    private int queueSize = 1000;
    private int commitInterval = 100;
    private ConsumersIndexFactory indexFactory = new ConsumersDailyIndexFactory();
    private String typeName = SchemaManager.SENT_TYPE;

    private final MetricsFacade metricsFacade;

    public Builder(Client elasticClient, MetricsFacade metricsFacade) {
      this.elasticClient = elasticClient;
      this.metricsFacade = metricsFacade;
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

    public Builder withIndexFactory(ConsumersIndexFactory indexFactory) {
      this.indexFactory = indexFactory;
      return this;
    }

    public ConsumersElasticsearchLogRepository build() {
      return new ConsumersElasticsearchLogRepository(
          elasticClient,
          clusterName,
          hostName,
          queueSize,
          commitInterval,
          indexFactory,
          typeName,
          metricsFacade);
    }
  }
}
