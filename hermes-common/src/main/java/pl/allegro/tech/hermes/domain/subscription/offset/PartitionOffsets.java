package pl.allegro.tech.hermes.domain.subscription.offset;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class PartitionOffsets implements Iterable<PartitionOffset> {

    private final Map<Integer, PartitionOffset> offsets = new LinkedHashMap<>();

    public PartitionOffsets add(PartitionOffset offset) {
        offsets.put(offset.getPartition(), offset);
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
