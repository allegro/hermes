package pl.allegro.tech.hermes.management.infrastructure.metrics;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.retry.ExponentialBackoffRetry;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.ZookeeperCounterException;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

public class SummedSharedCounter {

    private final List<LoadingCache<String, DistributedAtomicLong>> distributedAtomicLongCaches;

    public SummedSharedCounter(List<CuratorFramework> curatorClients, int expireAfter,
                               int distributedLoaderBackoff, int distributedLoaderRetries) {
        this.distributedAtomicLongCaches = curatorClients.stream()
                .map(client -> buildLoadingCache(client, expireAfter, distributedLoaderBackoff, distributedLoaderRetries))
                .collect(toList());
    }

    public long getValue(String path) {
        return distributedAtomicLongCaches.stream()
                .map(distAtomicLong -> getValue(distAtomicLong, path))
                .reduce(0L, Long::sum);
    }

    private long getValue(LoadingCache<String, DistributedAtomicLong> distAtomicLong, String path) {
        try {
            return distAtomicLong.get(path).get().preValue();
        } catch (Exception e) {
            throw new ZookeeperCounterException(path, e);
        }
    }

    private LoadingCache<String, DistributedAtomicLong> buildLoadingCache(CuratorFramework curatorClient, int expireAfter,
                                                                          int distributedLoaderBackoff, int distributedLoaderRetries) {
        return CacheBuilder.newBuilder()
                .expireAfterAccess(expireAfter, TimeUnit.HOURS)
                .build(new CacheLoader<>() {
                           @Override
                           public DistributedAtomicLong load(String key) {
                               return new DistributedAtomicLong(curatorClient, key,
                                       new ExponentialBackoffRetry(distributedLoaderBackoff, distributedLoaderRetries));
                           }
                       }
                );
    }
}
