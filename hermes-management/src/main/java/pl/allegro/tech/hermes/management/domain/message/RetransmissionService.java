package pl.allegro.tech.hermes.management.domain.message;

import java.util.List;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

public interface RetransmissionService {

  List<PartitionOffset> fetchTopicOffsetsAt(Topic topic, Long timestamp);

  List<PartitionOffset> fetchTopicEndOffsets(Topic topic);

  void indicateOffsetChange(
      Topic topic,
      String subscription,
      String brokersClusterName,
      List<PartitionOffset> partitionOffsets);

  boolean areOffsetsMoved(Topic topic, String subscriptionName, String brokersClusterName);
}
