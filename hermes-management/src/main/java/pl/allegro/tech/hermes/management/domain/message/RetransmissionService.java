package pl.allegro.tech.hermes.management.domain.message;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

import java.util.List;

public interface RetransmissionService {

    List<PartitionOffset> indicateOffsetChange(Topic topic, String subscription, String brokersClusterName,
                                                     long timestamp, boolean dryRun);

}
