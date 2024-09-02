package pl.allegro.tech.hermes.infrastructure.zookeeper.counter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.retry.ExponentialBackoffRetry;

public class SharedCounter {

  private final LoadingCache<String, DistributedAtomicLong> distributedAtomicLongs;

  public SharedCounter(
      CuratorFramework curatorClient,
      Duration expireAfter,
      Duration distributedLoaderBackoff,
      int distributedLoaderRetries) {
    distributedAtomicLongs =
        CacheBuilder.newBuilder()
            .expireAfterAccess(expireAfter.toHours(), TimeUnit.HOURS)
            .build(
                new DistributedAtomicLongLoader(
                    curatorClient,
                    new ExponentialBackoffRetry(
                        (int) distributedLoaderBackoff.toMillis(), distributedLoaderRetries)));
  }

  public boolean increment(String path, long count) {
    try {
      return distributedAtomicLongs.get(path).add(count).succeeded();
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

  private static final class DistributedAtomicLongLoader
      extends CacheLoader<String, DistributedAtomicLong> {

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
