package pl.allegro.tech.hermes.consumers.consumer.offset;

import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;

import java.util.NavigableMap;
import java.util.TreeMap;


public class OffsetHelper {

    private NavigableMap<Long, Integer> offsets = new TreeMap<>();

    private volatile Long lastFullyRead = null;

    synchronized void put(Message message) {
        changeOffsetCount(message.getOffset(), 1);
    }

    synchronized void decrement(Long offset) {
        changeOffsetCount(offset, -1);
        while (!offsets.isEmpty() && offsets.firstEntry().getValue() == 0) {
            lastFullyRead = offsets.firstKey();
            offsets.remove(lastFullyRead);
        }
    }

    private void changeOffsetCount(Long offset, int change) {
        int current = offsets.containsKey(offset) ? offsets.get(offset) : 0;
        offsets.put(offset, current + change);
    }

    public Long getLastFullyRead() {
        return lastFullyRead;
    }

}
