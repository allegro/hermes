package pl.allegro.tech.hermes.frontend.publishing.handlers;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter.QuotaInsight.quotaConfirmed;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.frontend.metric.ThroughputRegistry;

public class ThroughputLimiterFactory {

  private final ThroughputParameters throughputParameters;

  private final ThroughputRegistry throughputRegistry;

  private enum ThroughputLimiterType {
    UNLIMITED,
    FIXED,
    DYNAMIC
  }

  public ThroughputLimiterFactory(
      ThroughputParameters throughputParameters, ThroughputRegistry throughputRegistry) {
    this.throughputParameters = throughputParameters;
    this.throughputRegistry = throughputRegistry;
  }

  public ThroughputLimiter provide() {
    switch (ThroughputLimiterType.valueOf(throughputParameters.getType().toUpperCase())) {
      case UNLIMITED:
        return (a, b) -> quotaConfirmed();
      case FIXED:
        return new FixedThroughputLimiter(throughputParameters.getFixedMax());
      case DYNAMIC:
        return new DynamicThroughputLimiter(
            throughputParameters.getDynamicMax(),
            throughputParameters.getDynamicThreshold(),
            throughputParameters.getDynamicDesired(),
            throughputParameters.getDynamicIdle(),
            throughputParameters.getDynamicCheckInterval(),
            throughputRegistry::getGlobalThroughputOneMinuteRate,
            getExecutor());
      default:
        throw new IllegalArgumentException("Unknown throughput limiter type.");
    }
  }

  private ScheduledExecutorService getExecutor() {
    Logger logger = LoggerFactory.getLogger(ThroughputLimiterFactory.class);
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder()
            .setNameFormat("ThroughputLimiterExecutor-%d")
            .setUncaughtExceptionHandler(
                (t, e) -> logger.error("ThroughputLimiterExecutor failed {}", t.getName(), e))
            .build();
    return newScheduledThreadPool(1, threadFactory);
  }
}
