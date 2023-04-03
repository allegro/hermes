package pl.allegro.tech.hermes.consumers.consumer.offset;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

import java.util.List;
import java.util.Set;

public class OffsetsTracker {

    private final Table<KafkaTopicName, Integer, Long> inflightOffsets = HashBasedTable.create();

    public void register(List<SubscriptionPartitionOffset> partitionOffsets) {
        for (SubscriptionPartitionOffset offset : partitionOffsets) {
            register(offset.getKafkaTopicName(), offset.getPartition(), offset.getOffset());
        }
    }

    public void register(PartitionOffset partitionOffset) {
        register(partitionOffset.getTopic(), partitionOffset.getPartition(), partitionOffset.getOffset());
    }

    private void register(KafkaTopicName topic, int partition, long offset) {
        Long oldOffset = inflightOffsets.get(topic, partition);
        if (oldOffset == null || oldOffset < offset) {
            inflightOffsets.put(topic, partition, offset);
        }
    }

    public void unregister(Set<SubscriptionPartitionOffset> offsets) {
        for (SubscriptionPartitionOffset offset : offsets) {
            Long inflight = inflightOffsets.get(offset.getKafkaTopicName(), offset.getPartition());
            if (offset.getOffset() >= inflight) {
                inflightOffsets.remove(offset.getKafkaTopicName(), offset.getPartition());
            }
        }
    }

    public void remove(KafkaTopicName topic, int partition) {
        inflightOffsets.remove(topic, partition);
    }

    public boolean allCommitted() {
        return inflightOffsets.isEmpty();
    }
}
