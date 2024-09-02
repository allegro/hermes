package pl.allegro.tech.hermes.common.di.factories;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.common.cache.queue.LinkedHashSetBlockingQueue;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.infrastructure.zookeeper.cache.ModelAwareZookeeperNotifyingCache;

public class ModelAwareZookeeperNotifyingCacheFactory {

  private final CuratorFramework curator;

  private final MetricsFacade metricsFacade;

  private final ZookeeperParameters zookeeperParameters;

  public ModelAwareZookeeperNotifyingCacheFactory(
      CuratorFramework curator,
      MetricsFacade metricaFacade,
      ZookeeperParameters zookeeperParameters) {
    this.curator = curator;
    this.metricsFacade = metricaFacade;
    this.zookeeperParameters = zookeeperParameters;
  }

  public ModelAwareZookeeperNotifyingCache provide() {
    String rootPath = zookeeperParameters.getRoot();
    ExecutorService executor =
        createExecutor(rootPath, zookeeperParameters.getProcessingThreadPoolSize());
    ModelAwareZookeeperNotifyingCache cache =
        new ModelAwareZookeeperNotifyingCache(curator, executor, rootPath);
    try {
      cache.start();
    } catch (Exception e) {
      throw new IllegalStateException(
          "Unable to start Zookeeper cache for root path " + rootPath, e);
    }
    return cache;
  }

  private ExecutorService createExecutor(String rootPath, int processingThreadPoolSize) {
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat(rootPath + "-zk-cache-%d").build();
    ExecutorService executor =
        new ThreadPoolExecutor(
            1,
            processingThreadPoolSize,
            Integer.MAX_VALUE,
            TimeUnit.SECONDS,
            new LinkedHashSetBlockingQueue<>(),
            threadFactory);
    return metricsFacade.executor().monitor(executor, rootPath + "zk-cache");
  }
}
