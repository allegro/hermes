package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import java.time.Duration;
import java.util.List;

public interface KafkaConsumerParameters {

  int getSendBufferBytes();

  int getReceiveBufferBytes();

  int getFetchMinBytes();

  Duration getFetchMaxWait();

  Duration getReconnectBackoff();

  Duration getRetryBackoff();

  boolean isCheckCrcs();

  Duration getMetricsSampleWindow();

  int getMetricsNumSamples();

  Duration getRequestTimeout();

  Duration getConnectionsMaxIdle();

  int getMaxPollRecords();

  Duration getMaxPollInterval();

  String getAutoOffsetReset();

  Duration getSessionTimeout();

  Duration getHeartbeatInterval();

  Duration getMetadataMaxAge();

  int getMaxPartitionFetchMin();

  int getMaxPartitionFetchMax();

  List<PartitionAssignmentStrategy> getPartitionAssignmentStrategies();
}
