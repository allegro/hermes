package pl.allegro.tech.hermes.common.kafka.offset;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class PartitionOffsets implements Iterable<PartitionOffset> {

    private final Map<Integer, PartitionOffset> offsets = new LinkedHashMap<>();

    public PartitionOffsets add(PartitionOffset offset) {
        offsets.put(offset.getPartition(), offset);
        return this;
    }

    public PartitionOffsets addAll(PartitionOffsets offsets) {
        offsets.forEach(this::add);
        return this;
    }

    @Override
    public Iterator<PartitionOffset> iterator() {
        return offsets.values().iterator();
    }

    public PartitionOffset forPartition(int partition) {
        return offsets.get(partition);
    }
}
