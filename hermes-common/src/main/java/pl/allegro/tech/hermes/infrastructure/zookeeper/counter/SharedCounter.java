package pl.allegro.tech.hermes.infrastructure.zookeeper.counter;

import com.codahale.metrics.Meter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.AtomicStats;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.retry.ExponentialBackoffRetry;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static pl.allegro.tech.hermes.common.metric.Meters.SHARED_COUNTERS_OPTIMISTIC_INCREMENT_METER;
import static pl.allegro.tech.hermes.common.metric.Meters.SHARED_COUNTERS_PESSIMISTIC_INCREMENT_METER;

public class SharedCounter {

    private final LoadingCache<String, DistributedAtomicLong> distributedAtomicLongs;
    private final Meter optimisticIncrementMeter;
    private final Meter pessimisticIncrementMeter;

    public SharedCounter(CuratorFramework curatorClient,
                         Duration expireAfter,
                         Duration distributedLoaderBackoff,
                         int distributedLoaderRetries,
                         HermesMetrics metrics) {
        distributedAtomicLongs = CacheBuilder.newBuilder()
                .expireAfterAccess(expireAfter.toHours(), TimeUnit.HOURS)
                .build(new DistributedAtomicLongLoader(
                                curatorClient,
                                new ExponentialBackoffRetry((int) distributedLoaderBackoff.toMillis(), distributedLoaderRetries))
                );
        optimisticIncrementMeter = metrics.meter(SHARED_COUNTERS_OPTIMISTIC_INCREMENT_METER);
        pessimisticIncrementMeter = metrics.meter(SHARED_COUNTERS_PESSIMISTIC_INCREMENT_METER);
    }

    public boolean increment(String path, long count) {
        try {
            AtomicValue<Long> result = distributedAtomicLongs.get(path).add(count);
            AtomicStats stats = result.getStats();
            optimisticIncrementMeter.mark(stats.getOptimisticTries());
            pessimisticIncrementMeter.mark(stats.getPromotedLockTries());
            return result.succeeded();
        } catch (Exception e) {
            throw new ZookeeperCounterException(path, e);
        }
    }

    public long getValue(String path) {
        try {
            return distributedAtomicLongs.get(path).get().preValue();
        } catch (Exception e) {
            throw new ZookeeperCounterException(path, e);
        }
    }

    private static final class DistributedAtomicLongLoader extends CacheLoader<String, DistributedAtomicLong> {

        private final CuratorFramework client;

        private final RetryPolicy retryPolicy;

        DistributedAtomicLongLoader(CuratorFramework client, RetryPolicy retryPolicy) {
            this.client = client;
            this.retryPolicy = retryPolicy;
        }

        @Override
        public DistributedAtomicLong load(String key) throws Exception {
            return new DistributedAtomicLong(client, key, retryPolicy);
        }
    }
}
