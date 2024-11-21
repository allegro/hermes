package pl.allegro.tech.hermes.common.di.factories;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.common.cache.queue.LinkedHashSetBlockingQueue;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.infrastructure.zookeeper.cache.PathDepthAwareZookeeperCallbackRegistrar;

public class ZookeeperCallbaRegistrarFactory {

  private final CuratorFramework curator;

  private final MetricsFacade metricsFacade;

  private final ZookeeperParameters zookeeperParameters;

  private final String module;

  public ZookeeperCallbaRegistrarFactory(
      CuratorFramework curator,
      MetricsFacade metricsFacade,
      ZookeeperParameters zookeeperParameters,
      String module) {
    this.curator = curator;
    this.metricsFacade = metricsFacade;
    this.zookeeperParameters = zookeeperParameters;
    this.module = module;
  }

  public PathDepthAwareZookeeperCallbackRegistrar provide() {
    String rootPath = zookeeperParameters.getRoot();
    ExecutorService executor =
        createExecutor(rootPath, zookeeperParameters.getProcessingThreadPoolSize());
    PathDepthAwareZookeeperCallbackRegistrar callbackRegistrar =
        new PathDepthAwareZookeeperCallbackRegistrar(curator, executor, rootPath, module);
    try {
      callbackRegistrar.start();
    } catch (Exception e) {
      throw new IllegalStateException(
          "Unable to start Zookeeper callbackRegistrar for root path " + rootPath, e);
    }
    return callbackRegistrar;
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
