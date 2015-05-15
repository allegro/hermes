package pl.allegro.tech.hermes.consumers.consumer.offset;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PartitionOffsetHelper {

    private final LoadingCache<Integer, OffsetHelper> helpers;

    public PartitionOffsetHelper(long expirationMillis) {
        this.helpers = CacheBuilder.newBuilder()
                .expireAfterAccess(expirationMillis, TimeUnit.MILLISECONDS)
                .build(new OffsetHelperCacheLoader());
    }

    public void put(Message message) {
        OffsetHelper helper = helpers.getUnchecked(message.getPartition());
        helper.put(message);
    }

    public void decrement(int partition, long offset) {
        OffsetHelper offsetHelper = helpers.getUnchecked(partition);
        offsetHelper.decrement(offset);
    }

    public List<PartitionOffset> getAllLastFullyRead() {
        List<PartitionOffset> offsets = new ArrayList<>();
        for (Map.Entry<Integer, OffsetHelper> entry : helpers.asMap().entrySet()) {
            Integer partition = entry.getKey();
            OffsetHelper helper = entry.getValue();
            if (helper.getLastFullyRead() != null) {
                offsets.add(new PartitionOffset(helper.getLastFullyRead(), partition));
            }
        }
        return offsets;
    }

    private static final class OffsetHelperCacheLoader extends CacheLoader<Integer, OffsetHelper> {
        @Override
        public OffsetHelper load(Integer partition) throws Exception {
            return new OffsetHelper();
        }
    }

}
