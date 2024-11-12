package pl.allegro.tech.hermes.management.infrastructure.metrics;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.data.Stat;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.ZookeeperCounterException;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClient;

public class SummedSharedCounter {

  private final LoadingCache<String, CounterAggregator> counterAggregators;

  public SummedSharedCounter(
      List<ZookeeperClient> zookeeperClients,
      int expireAfter,
      int distributedLoaderBackoff,
      int distributedLoaderRetries) {
    this.counterAggregators =
        buildLoadingCache(
            zookeeperClients, expireAfter, distributedLoaderBackoff, distributedLoaderRetries);
  }

  public long getValue(String path) {
    try {
      return counterAggregators.get(path).aggregate();
    } catch (ZookeeperCounterException e) {
      throw e;
    } catch (Exception e) {
      throw new ZookeeperCounterException(path, e);
    }
  }

  public Optional<Instant> getLastModified(String path) {
    try {
      return counterAggregators.get(path).getLastModified();
    } catch (ZookeeperCounterException e) {
      throw e;
    } catch (Exception e) {
      throw new ZookeeperCounterException(path, e);
    }
  }

  private LoadingCache<String, CounterAggregator> buildLoadingCache(
      List<ZookeeperClient> zookeeperClients,
      int expireAfter,
      int distributedLoaderBackoff,
      int distributedLoaderRetries) {
    return CacheBuilder.newBuilder()
        .expireAfterAccess(expireAfter, TimeUnit.HOURS)
        .build(
            new CacheLoader<>() {
              @Override
              public CounterAggregator load(String key) {
                return new CounterAggregator(
                    key, zookeeperClients, distributedLoaderBackoff, distributedLoaderRetries);
              }
            });
  }

  private static class CounterAggregator {

    private final String counterName;
    private final Map<String, CuratorFramework> curatorPerDatacenter = new HashMap<>();
    private final Map<String, DistributedAtomicLong> counterPerDatacenter = new HashMap<>();

    CounterAggregator(
        String counterName,
        List<ZookeeperClient> zookeeperClients,
        int distributedLoaderBackoff,
        int distributedLoaderRetries) {
      this.counterName = counterName;
      for (ZookeeperClient zookeeperClient : zookeeperClients) {
        CuratorFramework curatorFramework = zookeeperClient.getCuratorFramework();
        curatorPerDatacenter.put(zookeeperClient.getDatacenterName(), curatorFramework);
        DistributedAtomicLong distributedAtomicLong =
            new DistributedAtomicLong(
                curatorFramework,
                counterName,
                new ExponentialBackoffRetry(distributedLoaderBackoff, distributedLoaderRetries));
        counterPerDatacenter.put(zookeeperClient.getDatacenterName(), distributedAtomicLong);
      }
    }

    long aggregate() throws Exception {
      long sum = 0;
      for (Map.Entry<String, DistributedAtomicLong> counterEntry :
          counterPerDatacenter.entrySet()) {
        ensureConnected(counterEntry.getKey());
        DistributedAtomicLong counter = counterEntry.getValue();
        sum += counter.get().preValue();
      }
      return sum;
    }

    Optional<Instant> getLastModified() throws Exception {
      Instant lastModified = null;
      for (Map.Entry<String, CuratorFramework> curatorClient : curatorPerDatacenter.entrySet()) {
        ensureConnected(curatorClient.getKey());
        Stat stat = curatorClient.getValue().checkExists().forPath(counterName);
        if (stat != null) {
          Instant nodeLastModified = Instant.ofEpochMilli(stat.getMtime());
          if (lastModified == null || nodeLastModified.isAfter(lastModified)) {
            lastModified = nodeLastModified;
          }
        }
      }
      return Optional.ofNullable(lastModified);
    }

    private void ensureConnected(String datacenter) {
      CuratorFramework curator = curatorPerDatacenter.get(datacenter);
      if (!curator.getZookeeperClient().isConnected()) {
        throw new ZookeeperCounterException(
            counterName,
            "Could not establish connection to a Zookeeper instance in " + datacenter + ".");
      }
    }
  }
}
