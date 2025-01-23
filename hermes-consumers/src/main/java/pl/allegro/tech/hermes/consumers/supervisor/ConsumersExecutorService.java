package pl.allegro.tech.hermes.consumers.supervisor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.supervisor.process.ConsumerProcess;

public class ConsumersExecutorService {

  private static final Logger logger = LoggerFactory.getLogger(ConsumersExecutorService.class);
  private final ThreadPoolExecutor executor;

  public ConsumersExecutorService(int poolSize, MetricsFacade metrics) {
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder()
            .setNameFormat("Consumer-%d")
            .setUncaughtExceptionHandler(
                (t, e) -> logger.error("Exception from consumer with name {}", t.getName(), e))
            .build();

    executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize, threadFactory);

    metrics
        .consumer()
        .registerConsumerProcessesThreadsGauge(executor, ThreadPoolExecutor::getActiveCount);
  }

  public Future<?> execute(ConsumerProcess consumer) {
    return executor.submit(consumer);
  }

  public void shutdown() {
    executor.shutdownNow();
    try {
      executor.awaitTermination(1, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      logger.error("Termination of consumers executor service interrupted.", e);
    }
  }
}
