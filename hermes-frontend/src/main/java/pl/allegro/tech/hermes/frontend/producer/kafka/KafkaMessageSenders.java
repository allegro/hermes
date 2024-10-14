package pl.allegro.tech.hermes.frontend.producer.kafka;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.common.PartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

// exposes kafka producer metrics, see:
// https://docs.confluent.io/platform/current/kafka/monitoring.html#producer-metrics
public class KafkaMessageSenders {

  private static final Logger logger = LoggerFactory.getLogger(KafkaMessageSenders.class);

  private final KafkaMessageSender<byte[], byte[]> ackLeader;
  private final KafkaMessageSender<byte[], byte[]> ackAll;

  private final List<KafkaMessageSender<byte[], byte[]>> remoteAckLeader;
  private final List<KafkaMessageSender<byte[], byte[]>> remoteAckAll;

  private final MinInSyncReplicasLoader localMinInSyncReplicasLoader;
  private final TopicMetadataLoadingExecutor topicMetadataLoadingExecutor;
  private final List<TopicMetadataLoader> localDatacenterTopicMetadataLoaders;
  private final List<TopicMetadataLoader> kafkaProducerMetadataRefreshers;
  private final List<String> datacenters;

  KafkaMessageSenders(
      TopicMetadataLoadingExecutor topicMetadataLoadingExecutor,
      MinInSyncReplicasLoader localMinInSyncReplicasLoader,
      Tuple localSenders,
      List<Tuple> remoteSenders) {
    this.topicMetadataLoadingExecutor = topicMetadataLoadingExecutor;
    this.localMinInSyncReplicasLoader = localMinInSyncReplicasLoader;
    this.ackLeader = localSenders.ackLeader;
    this.ackAll = localSenders.ackAll;
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
            .map(tuple -> tuple.ackAll)
            .map(KafkaMessageSender::getDatacenter)
            .toList();
  }

  KafkaMessageSender<byte[], byte[]> get(Topic topic) {
    return topic.isReplicationConfirmRequired() ? ackAll : ackLeader;
  }

  List<KafkaMessageSender<byte[], byte[]>> getRemote(Topic topic) {
    return topic.isReplicationConfirmRequired() ? remoteAckAll : remoteAckLeader;
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

  public void registerSenderMetrics(String name) {
    ackLeader.registerGauges(Topic.Ack.LEADER, name);
    ackAll.registerGauges(Topic.Ack.ALL, name);
    remoteAckLeader.forEach(sender -> sender.registerGauges(Topic.Ack.LEADER, name));
    remoteAckAll.forEach(sender -> sender.registerGauges(Topic.Ack.ALL, name));
  }

  static class Tuple {
    private final KafkaMessageSender<byte[], byte[]> ackLeader;
    private final KafkaMessageSender<byte[], byte[]> ackAll;

    Tuple(KafkaMessageSender<byte[], byte[]> ackLeader, KafkaMessageSender<byte[], byte[]> ackAll) {
      this.ackLeader = ackLeader;
      this.ackAll = ackAll;
    }
  }

  public void close() {
    ackAll.close();
    ackLeader.close();
  }

  private class KafkaProducerMetadataRefresher implements TopicMetadataLoader {

    private final KafkaMessageSender<byte[], byte[]> ackLeader;
    private final KafkaMessageSender<byte[], byte[]> ackAll;

    KafkaProducerMetadataRefresher(Tuple tuple) {
      this.ackLeader = tuple.ackLeader;
      this.ackAll = tuple.ackAll;
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
      return topic.isReplicationConfirmRequired() ? ackAll : ackLeader;
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
