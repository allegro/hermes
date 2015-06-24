package pl.allegro.tech.hermes.management.domain.message;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;

import java.util.List;

public interface RetransmissionService {

    List<PartitionOffset> indicateOffsetChange(TopicName topic, String subscription, String brokersClusterName,
                                                     long timestamp, boolean dryRun);

}
