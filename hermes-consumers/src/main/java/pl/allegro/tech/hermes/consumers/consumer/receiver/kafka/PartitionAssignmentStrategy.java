package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import org.apache.kafka.clients.consumer.ConsumerPartitionAssignor;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.consumer.RangeAssignor;
import org.apache.kafka.clients.consumer.StickyAssignor;

public enum PartitionAssignmentStrategy {
  RANGE(RangeAssignor.class),
  STICKY(StickyAssignor.class),
  COOPERATIVE(CooperativeStickyAssignor.class);

  private final Class<? extends ConsumerPartitionAssignor> assignorClass;

  PartitionAssignmentStrategy(Class<? extends ConsumerPartitionAssignor> assignorClass) {
    this.assignorClass = assignorClass;
  }

  Class<? extends ConsumerPartitionAssignor> getAssignorClass() {
    return assignorClass;
  }
}
