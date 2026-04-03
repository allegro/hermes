package pl.allegro.tech.hermes.frontend.producer.kafka;

import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.common.PartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.Topic.Ack;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

/**
 * Manages pools of Kafka message senders and routes topics to specific senders within each pool.
 *
 * <p>Each {@link KafkaMessageSenderPool} contains multiple {@link KafkaMessageSender} instances for
 * a single ack configuration in a single datacenter. Topics are deterministically assigned to a
 * specific sender within the pool, ensuring that all messages for a given topic are always sent
 * through the same producer instance.
 *
 * @see KafkaMessageSenderPool
 */
// exposes kafka producer metrics, see:
// https://docs.confluent.io/platform/current/kafka/monitoring.html#producer-metrics
public class KafkaMessageSenders {

  private static final Logger logger = LoggerFactory.getLogger(KafkaMessageSenders.class);

  private static final String PRODUCER_METRICS_GROUP = "producer-metrics";

  private final KafkaMessageSenderPool localAckLeader;
  private final KafkaMessageSenderPool localAckAll;
  private final List<KafkaMessageSenderPool> remoteAckLeader;
  private final List<KafkaMessageSenderPool> remoteAckAll;

  private final MinInSyncReplicasLoader localMinInSyncReplicasLoader;
  private final TopicMetadataLoadingExecutor topicMetadataLoadingExecutor;
  private final MetricsFacade metricsFacade;
  private final List<TopicMetadataLoader> localDatacenterTopicMetadataLoaders;
  private final List<TopicMetadataLoader> kafkaProducerMetadataRefreshers;
  private final List<String> datacenters;

  KafkaMessageSenders(
      TopicMetadataLoadingExecutor topicMetadataLoadingExecutor,
      MinInSyncReplicasLoader localMinInSyncReplicasLoader,
      MetricsFacade metricsFacade,
      SenderPair localSenders,
      List<SenderPair> remoteSenders) {
    this.topicMetadataLoadingExecutor = topicMetadataLoadingExecutor;
    this.localMinInSyncReplicasLoader = localMinInSyncReplicasLoader;
    this.metricsFacade = metricsFacade;
    this.localAckLeader = localSenders.ackLeader;
    this.localAckAll = localSenders.ackAll;
    this.remoteAckLeader =
        remoteSenders.stream().map(it -> it.ackLeader).collect(Collectors.toList());
    this.remoteAckAll = remoteSenders.stream().map(it -> it.ackAll).collect(Collectors.toList());
    this.localDatacenterTopicMetadataLoaders =
        List.of(new LocalDatacenterTopicAvailabilityChecker());
    this.kafkaProducerMetadataRefreshers =
        Stream.concat(Stream.of(localSenders), remoteSenders.stream())
            .map(KafkaProducerMetadataRefresher::new)
            .collect(Collectors.toList());
    this.datacenters =
        Stream.concat(Stream.of(localSenders), remoteSenders.stream())
            .map(pair -> pair.ackAll.getDatacenter())
            .toList();
  }

  /**
   * Returns the sender assigned to the given topic based on its ack configuration and pool routing.
   *
   * @param topic the Hermes topic (used to determine ack=leader vs ack=all and routing)
   * @return the assigned {@link KafkaMessageSender} from the pool
   */
  KafkaMessageSender<byte[], byte[]> get(Topic topic) {
    KafkaMessageSenderPool pool =
        topic.isReplicationConfirmRequired() ? localAckAll : localAckLeader;
    return pool.get(topic.getQualifiedName());
  }

  /**
   * Returns the remote senders assigned to the given topic from each remote datacenter.
   *
   * @param topic the Hermes topic (used to determine ack=leader vs ack=all and routing)
   * @return a list of remote senders (one per remote datacenter) for the topic
   */
  List<KafkaMessageSender<byte[], byte[]>> getRemote(Topic topic) {
    List<KafkaMessageSenderPool> pools =
        topic.isReplicationConfirmRequired() ? remoteAckAll : remoteAckLeader;
    return pools.stream().map(pool -> pool.get(topic.getQualifiedName())).toList();
  }

  List<String> getDatacenters() {
    return datacenters;
  }

  void refreshTopicMetadata() {
    topicMetadataLoadingExecutor.execute(kafkaProducerMetadataRefreshers);
  }

  boolean areAllTopicsAvailable() {
    return topicMetadataLoadingExecutor.execute(localDatacenterTopicMetadataLoaders);
  }

  boolean isTopicAvailable(CachedTopic cachedTopic) {
    String kafkaTopicName = cachedTopic.getKafkaTopics().getPrimary().name().asString();

    try {
      List<PartitionInfo> partitionInfos =
          get(cachedTopic.getTopic()).loadPartitionMetadataFor(kafkaTopicName);
      if (anyPartitionWithoutLeader(partitionInfos)) {
        logger.warn("Topic {} has partitions without a leader.", kafkaTopicName);
        return false;
      }
      if (anyUnderReplicatedPartition(partitionInfos, kafkaTopicName)) {
        logger.warn("Topic {} has under replicated partitions.", kafkaTopicName);
        return false;
      }
      if (!partitionInfos.isEmpty()) {
        return true;
      }
    } catch (Exception e) {
      logger.warn(
          "Could not read information about partitions for topic {}. {}",
          kafkaTopicName,
          e.getMessage());
      return false;
    }

    logger.warn("No information about partitions for topic {}", kafkaTopicName);
    return false;
  }

  private boolean anyPartitionWithoutLeader(List<PartitionInfo> partitionInfos) {
    return partitionInfos.stream().anyMatch(p -> p.leader() == null);
  }

  private boolean anyUnderReplicatedPartition(
      List<PartitionInfo> partitionInfos, String kafkaTopicName) throws Exception {
    int minInSyncReplicas = localMinInSyncReplicasLoader.get(kafkaTopicName);
    return partitionInfos.stream().anyMatch(p -> p.inSyncReplicas().length < minInSyncReplicas);
  }

  /**
   * Registers composite Kafka producer metrics that aggregate values across all pool members.
   *
   * <p>For gauge-type metrics (buffer bytes, compression rate, metadata age, queue time), values
   * are aggregated using sum, average, or max as appropriate. For counter-type metrics (record send
   * total, failed batches), values are summed. This produces identical metric names and tags
   * regardless of pool size, making the pool transparent to monitoring dashboards.
   */
  public void registerSenderMetrics(String name) {
    registerPoolMetrics(localAckLeader, Ack.LEADER, name);
    registerPoolMetrics(localAckAll, Ack.ALL, name);
    remoteAckLeader.forEach(remotePool -> registerPoolMetrics(remotePool, Ack.LEADER, name));
    remoteAckAll.forEach(remotePool -> registerPoolMetrics(remotePool, Ack.ALL, name));
  }

  private void registerPoolMetrics(KafkaMessageSenderPool pool, Ack ack, String sender) {
    String datacenter = pool.getDatacenter();
    List<KafkaMessageSender<byte[], byte[]>> senders = pool.allSenders();

    // Sum metrics: total across all pool members
    ToDoubleFunction<List<KafkaMessageSender<byte[], byte[]>>> bufferTotalBytes =
        s -> sumMetric(s, "buffer-total-bytes");
    ToDoubleFunction<List<KafkaMessageSender<byte[], byte[]>>> bufferAvailableBytes =
        s -> sumMetric(s, "buffer-available-bytes");
    ToDoubleFunction<List<KafkaMessageSender<byte[], byte[]>>> failedBatches =
        s -> sumMetric(s, "record-error-total");
    ToDoubleFunction<List<KafkaMessageSender<byte[], byte[]>>> recordSendTotal =
        s -> sumMetric(s, "record-send-total");

    // Average metrics: averaged across pool members
    ToDoubleFunction<List<KafkaMessageSender<byte[], byte[]>>> compressionRate =
        s -> avgMetric(s, "compression-rate-avg");

    // Max metrics: worst case across pool members
    ToDoubleFunction<List<KafkaMessageSender<byte[], byte[]>>> metadataAge =
        s -> maxMetric(s, "metadata-age");
    ToDoubleFunction<List<KafkaMessageSender<byte[], byte[]>>> queueTimeMax =
        s -> maxMetric(s, "record-queue-time-max");

    if (ack == Ack.ALL) {
      metricsFacade
          .producer()
          .registerAckAllTotalBytesGauge(senders, bufferTotalBytes, sender, datacenter);
      metricsFacade
          .producer()
          .registerAckAllAvailableBytesGauge(senders, bufferAvailableBytes, sender, datacenter);
      metricsFacade
          .producer()
          .registerAckAllCompressionRateGauge(senders, compressionRate, sender, datacenter);
      metricsFacade
          .producer()
          .registerAckAllFailedBatchesGauge(senders, failedBatches, sender, datacenter);
      metricsFacade
          .producer()
          .registerAckAllMetadataAgeGauge(senders, metadataAge, sender, datacenter);
      metricsFacade
          .producer()
          .registerAckAllRecordQueueTimeMaxGauge(senders, queueTimeMax, sender, datacenter);
      metricsFacade
          .producer()
          .registerAckAllRecordSendCounter(senders, recordSendTotal, sender, datacenter);
    } else if (ack == Ack.LEADER) {
      metricsFacade
          .producer()
          .registerAckLeaderTotalBytesGauge(senders, bufferTotalBytes, sender, datacenter);
      metricsFacade
          .producer()
          .registerAckLeaderAvailableBytesGauge(senders, bufferAvailableBytes, sender, datacenter);
      metricsFacade
          .producer()
          .registerAckLeaderCompressionRateGauge(senders, compressionRate, sender, datacenter);
      metricsFacade
          .producer()
          .registerAckLeaderFailedBatchesGauge(senders, failedBatches, sender, datacenter);
      metricsFacade
          .producer()
          .registerAckLeaderMetadataAgeGauge(senders, metadataAge, sender, datacenter);
      metricsFacade
          .producer()
          .registerAckLeaderRecordQueueTimeMaxGauge(senders, queueTimeMax, sender, datacenter);
      metricsFacade
          .producer()
          .registerAckLeaderRecordSendCounter(senders, recordSendTotal, sender, datacenter);
    }
  }

  private static double sumMetric(
      List<KafkaMessageSender<byte[], byte[]>> senders, String metricName) {
    return senders.stream()
        .mapToDouble(s -> s.readProducerMetric(metricName, PRODUCER_METRICS_GROUP))
        .sum();
  }

  private static double avgMetric(
      List<KafkaMessageSender<byte[], byte[]>> senders, String metricName) {
    return senders.stream()
        .mapToDouble(s -> s.readProducerMetric(metricName, PRODUCER_METRICS_GROUP))
        .average()
        .orElse(0.0);
  }

  private static double maxMetric(
      List<KafkaMessageSender<byte[], byte[]>> senders, String metricName) {
    return senders.stream()
        .mapToDouble(s -> s.readProducerMetric(metricName, PRODUCER_METRICS_GROUP))
        .max()
        .orElse(0.0);
  }

  /**
   * A pair of {@link KafkaMessageSenderPool} instances for ack=leader and ack=all configurations in
   * a single datacenter. Used during construction of {@link KafkaMessageSenders}.
   */
  static class SenderPair {
    private final KafkaMessageSenderPool ackLeader;
    private final KafkaMessageSenderPool ackAll;

    SenderPair(KafkaMessageSenderPool ackLeader, KafkaMessageSenderPool ackAll) {
      this.ackLeader = ackLeader;
      this.ackAll = ackAll;
    }
  }

  public void close() {
    localAckAll.close();
    localAckLeader.close();
    remoteAckAll.forEach(KafkaMessageSenderPool::close);
    remoteAckLeader.forEach(KafkaMessageSenderPool::close);
  }

  private class KafkaProducerMetadataRefresher implements TopicMetadataLoader {

    private final KafkaMessageSenderPool ackLeader;
    private final KafkaMessageSenderPool ackAll;

    KafkaProducerMetadataRefresher(SenderPair pair) {
      this.ackLeader = pair.ackLeader;
      this.ackAll = pair.ackAll;
    }

    @Override
    public MetadataLoadingResult load(CachedTopic cachedTopic) {
      String kafkaTopicName = cachedTopic.getKafkaTopics().getPrimary().name().asString();
      var sender = getSender(cachedTopic.getTopic());
      var partitionInfos = sender.loadPartitionMetadataFor(kafkaTopicName);
      if (anyPartitionWithoutLeader(partitionInfos)) {
        logger.warn("Topic {} has partitions without a leader.", kafkaTopicName);
        return MetadataLoadingResult.failure(cachedTopic.getTopicName(), sender.getDatacenter());
      }
      if (partitionInfos.isEmpty()) {
        logger.warn("No information about partitions for topic {}", kafkaTopicName);
        return MetadataLoadingResult.failure(cachedTopic.getTopicName(), sender.getDatacenter());
      }
      return MetadataLoadingResult.success(cachedTopic.getTopicName(), sender.getDatacenter());
    }

    private KafkaMessageSender<byte[], byte[]> getSender(Topic topic) {
      KafkaMessageSenderPool pool = topic.isReplicationConfirmRequired() ? ackAll : ackLeader;
      return pool.get(topic.getQualifiedName());
    }
  }

  private class LocalDatacenterTopicAvailabilityChecker implements TopicMetadataLoader {

    @Override
    public MetadataLoadingResult load(CachedTopic topic) {
      String datacenter = get(topic.getTopic()).getDatacenter();
      if (isTopicAvailable(topic)) {
        return MetadataLoadingResult.success(topic.getTopicName(), datacenter);
      }
      return MetadataLoadingResult.failure(topic.getTopicName(), datacenter);
    }
  }
}
