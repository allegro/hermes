package pl.allegro.tech.hermes.management.domain.message;

import java.util.List;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

public interface RetransmissionService {

  List<PartitionOffset> indicateOffsetChange(
      Topic topic, String subscription, String brokersClusterName, long timestamp, boolean dryRun);

  boolean areOffsetsMoved(Topic topic, String subscriptionName, String brokersClusterName);
}
