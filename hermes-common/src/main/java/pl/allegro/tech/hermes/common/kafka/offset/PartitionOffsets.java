package pl.allegro.tech.hermes.common.kafka.offset;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
}
