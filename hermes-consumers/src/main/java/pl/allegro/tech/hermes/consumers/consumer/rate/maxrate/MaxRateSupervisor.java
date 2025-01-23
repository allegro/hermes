package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ClusterAssignmentCache;

public class MaxRateSupervisor implements Runnable {

  private final Set<NegotiatedMaxRateProvider> providers =
      Collections.newSetFromMap(new ConcurrentHashMap<>());
  private final Duration selfUpdateInterval;
  private final ScheduledExecutorService selfUpdateExecutor;
  private final MaxRateCalculatorJob calculatorJob;
  private final MaxRateRegistry maxRateRegistry;
  private ScheduledFuture<?> updateJob;

  public MaxRateSupervisor(
      MaxRateParameters maxRateParameters,
      ClusterAssignmentCache clusterAssignmentCache,
      MaxRateRegistry maxRateRegistry,
      ConsumerNodesRegistry consumerNodesRegistry,
      SubscriptionsCache subscriptionsCache,
      MetricsFacade metrics,
      Clock clock) {
    this.maxRateRegistry = maxRateRegistry;
    this.selfUpdateInterval = maxRateParameters.getUpdateInterval();

    this.selfUpdateExecutor =
        Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("max-rate-provider-%d").build());

    MaxRateBalancer balancer =
        new MaxRateBalancer(
            maxRateParameters.getBusyTolerance(),
            maxRateParameters.getMinMaxRate(),
            maxRateParameters.getMinAllowedChangePercent());

    this.calculatorJob =
        new MaxRateCalculatorJob(
            maxRateParameters.getBalanceInterval(),
            clusterAssignmentCache,
            consumerNodesRegistry,
            balancer,
            maxRateRegistry,
            subscriptionsCache,
            metrics,
            clock);
  }

  public void start() throws Exception {
    maxRateRegistry.start();
    calculatorJob.start();
    updateJob = startSelfUpdate();
  }

  public void stop() throws Exception {
    maxRateRegistry.stop();
    calculatorJob.stop();
    if (updateJob != null) {
      updateJob.cancel(false);
    }
    selfUpdateExecutor.shutdown();
    selfUpdateExecutor.awaitTermination(10, TimeUnit.SECONDS);
  }

  private ScheduledFuture<?> startSelfUpdate() {
    return selfUpdateExecutor.scheduleAtFixedRate(
        this, 0, selfUpdateInterval.toSeconds(), TimeUnit.SECONDS);
  }

  @Override
  public void run() {
    providers.forEach(NegotiatedMaxRateProvider::tickForHistory);
    maxRateRegistry.onAfterWriteRateHistories();
  }

  public void register(NegotiatedMaxRateProvider maxRateProvider) {
    providers.add(maxRateProvider);
  }

  public void unregister(NegotiatedMaxRateProvider maxRateProvider) {
    providers.remove(maxRateProvider);
  }
}
