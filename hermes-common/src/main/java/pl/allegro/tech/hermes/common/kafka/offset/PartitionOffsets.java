package pl.allegro.tech.hermes.common.kafka.offset;

import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class PartitionOffsets implements Iterable<PartitionOffset> {

    private final List<PartitionOffset> offsets = new ArrayList<>();

    public PartitionOffsets add(PartitionOffset offset) {
        offsets.add(offset);
        return this;
    }

    public PartitionOffsets addAll(PartitionOffsets offsets) {
        offsets.forEach(this::add);
        return this;
    }

    @Override
    public Iterator<PartitionOffset> iterator() {
        return offsets.iterator();
    }

    public Optional<PartitionOffset> findForTopicAndPartition(KafkaTopicName topicName, int partition) {
        return offsets
                .stream()
                .filter( offset -> topicName.equals(offset.getTopic()) && partition == offset.getPartition())
                .findFirst();
    }

}
