package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import static java.util.Collections.newSetFromMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toMap;
import static pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths.CONSUMERS_WORKLOAD_PATH;
import static pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths.CONSUMER_LOAD_PATH;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.LongAdder;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.concurrent.ExecutorServiceFactory;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

public class ZookeeperConsumerNodeLoadRegistry implements ConsumerNodeLoadRegistry {

  private static final Logger logger =
      LoggerFactory.getLogger(ZookeeperConsumerNodeLoadRegistry.class);

  private final Duration interval;
  private final CuratorFramework curator;
  private final ZookeeperPaths zookeeperPaths;
  private final Clock clock;
  private final String basePath;
  private final String currentConsumerPath;
  private final ConsumerNodeLoadEncoder encoder;
  private final ConsumerNodeLoadDecoder decoder;
  private final ScheduledExecutorService executor;
  private final OperatingSystemMXBean platformMXBean =
      ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

  private final Set<ZookeeperSubscriptionLoadRecorder> subscriptionLoadRecorders =
      newSetFromMap(new ConcurrentHashMap<>());
  private volatile long lastReset;
  private volatile double currentOperationsPerSecond = 0d;
  private volatile double cpuUtilization = -1;

  public ZookeeperConsumerNodeLoadRegistry(
      CuratorFramework curator,
      SubscriptionIds subscriptionIds,
      ZookeeperPaths zookeeperPaths,
      String currentConsumerId,
      String clusterName,
      Duration interval,
      ExecutorServiceFactory executorServiceFactory,
      Clock clock,
      MetricsFacade metrics,
      int consumerLoadEncoderBufferSizeBytes) {
    this.curator = curator;
    this.zookeeperPaths = zookeeperPaths;
    this.clock = clock;
    this.basePath =
        zookeeperPaths.join(
            zookeeperPaths.basePath(), CONSUMERS_WORKLOAD_PATH, clusterName, CONSUMER_LOAD_PATH);
    this.currentConsumerPath = resolveConsumerLoadPath(currentConsumerId);
    this.interval = interval;
    this.encoder = new ConsumerNodeLoadEncoder(subscriptionIds, consumerLoadEncoderBufferSizeBytes);
    this.decoder = new ConsumerNodeLoadDecoder(subscriptionIds);
    this.executor =
        executorServiceFactory.createSingleThreadScheduledExecutor(
            "consumer-node-load-reporter-%d");
    this.lastReset = clock.millis();
    metrics
        .workload()
        .registerOperationsPerSecondGauge(this, registry -> registry.currentOperationsPerSecond);
    metrics.workload().registerCpuUtilizationGauge(this, registry -> registry.cpuUtilization);
    if (platformMXBean.getProcessCpuLoad() < 0d) {
      logger.warn("Process CPU load is not available.");
    }
  }

  @Override
  public void start() {
    executor.scheduleWithFixedDelay(this::report, 0, interval.toMillis(), MILLISECONDS);
  }

  @Override
  public void stop() {
    executor.shutdown();
  }

  private void report() {
    try {
      ConsumerNodeLoad consumerNodeLoad = calculateConsumerNodeLoad();
      currentOperationsPerSecond =
          consumerNodeLoad.getLoadPerSubscription().values().stream()
              .mapToDouble(SubscriptionLoad::getOperationsPerSecond)
              .sum();
      persist(consumerNodeLoad);
    } catch (Exception e) {
      logger.error("Error while reporting consumer node load", e);
    }
  }

  private ConsumerNodeLoad calculateConsumerNodeLoad() {
    long now = clock.millis();
    long elapsedMillis = now - lastReset;
    long elapsedSeconds = Math.max(MILLISECONDS.toSeconds(elapsedMillis), 1);
    lastReset = now;
    cpuUtilization = platformMXBean.getProcessCpuLoad();
    Map<SubscriptionName, SubscriptionLoad> loadPerSubscription =
        subscriptionLoadRecorders.stream()
            .collect(
                toMap(
                    ZookeeperSubscriptionLoadRecorder::getSubscriptionName,
                    recorder -> recorder.calculate(elapsedSeconds)));
    return new ConsumerNodeLoad(cpuUtilization, loadPerSubscription);
  }

  private void persist(ConsumerNodeLoad metrics) throws Exception {
    byte[] encoded = encoder.encode(metrics);
    try {
      curator.setData().forPath(currentConsumerPath, encoded);
    } catch (KeeperException.NoNodeException e) {
      try {
        curator
            .create()
            .creatingParentContainersIfNeeded()
            .withMode(CreateMode.EPHEMERAL)
            .forPath(currentConsumerPath, encoded);
      } catch (KeeperException.NodeExistsException ex) {
        // ignore
      }
    }
  }

  @Override
  public ConsumerNodeLoad get(String consumerId) {
    String consumerLoadPath = resolveConsumerLoadPath(consumerId);
    try {
      if (curator.checkExists().forPath(consumerLoadPath) != null) {
        byte[] bytes = curator.getData().forPath(consumerLoadPath);
        return decoder.decode(bytes);
      }
    } catch (Exception e) {
      logger.warn("Could not read node data on path " + consumerLoadPath, e);
    }
    return ConsumerNodeLoad.UNDEFINED;
  }

  private String resolveConsumerLoadPath(String consumerId) {
    return zookeeperPaths.join(basePath, consumerId);
  }

  @Override
  public SubscriptionLoadRecorder register(SubscriptionName subscriptionName) {
    return new ZookeeperSubscriptionLoadRecorder(subscriptionName);
  }

  private class ZookeeperSubscriptionLoadRecorder implements SubscriptionLoadRecorder {

    private final SubscriptionName subscriptionName;
    private final LongAdder operationsCounter = new LongAdder();

    ZookeeperSubscriptionLoadRecorder(SubscriptionName subscriptionName) {
      this.subscriptionName = subscriptionName;
    }

    @Override
    public void initialize() {
      operationsCounter.reset();
      subscriptionLoadRecorders.add(this);
    }

    @Override
    public void recordSingleOperation() {
      operationsCounter.increment();
    }

    @Override
    public void shutdown() {
      operationsCounter.reset();
      subscriptionLoadRecorders.remove(this);
    }

    SubscriptionName getSubscriptionName() {
      return subscriptionName;
    }

    SubscriptionLoad calculate(long elapsedSeconds) {
      double operationsPerSecond = (double) operationsCounter.sumThenReset() / elapsedSeconds;
      return new SubscriptionLoad(operationsPerSecond);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ZookeeperSubscriptionLoadRecorder that = (ZookeeperSubscriptionLoadRecorder) o;
      return subscriptionName.equals(that.subscriptionName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(subscriptionName);
    }
  }
}
