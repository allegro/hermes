package pl.allegro.tech.hermes.benchmark.consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;

public class InstrumentedExecutorServiceFactoryWrapper extends InstrumentedExecutorServiceFactory {
  private final List<ExecutorService> createdExecutors = new ArrayList<>();

  public InstrumentedExecutorServiceFactoryWrapper(MetricsFacade metricsFacade) {
    super(metricsFacade);
  }

  @Override
  public ExecutorService getExecutorService(String name, int size, boolean monitoringEnabled) {
    ExecutorService executorService = super.getExecutorService(name, size, monitoringEnabled);
    createdExecutors.add(executorService);
    return executorService;
  }

  @Override
  public ExecutorService getExecutorService(
      String name, int size, boolean monitoringEnabled, int queueCapacity) {
    ExecutorService executorService =
        super.getExecutorService(name, size, monitoringEnabled, queueCapacity);
    createdExecutors.add(executorService);
    return executorService;
  }

  public void shutdownAll() {
    createdExecutors.forEach(ExecutorService::shutdown);
    createdExecutors.clear();
  }
}
