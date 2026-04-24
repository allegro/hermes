package pl.allegro.tech.hermes.frontend.producer.kafka;

import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

/**
 * Periodically logs the topic distribution across producers in a {@link
 * KafkaMessageSenderPoolRouter}.
 */
class KafkaMessageSenderPoolReporter {

  private static final Logger logger = getLogger(KafkaMessageSenderPoolReporter.class);
  private static final long REPORT_INTERVAL_HOURS = 1;

  private final String poolName;
  private final KafkaMessageSenderPoolRouter router;
  private final ScheduledExecutorService scheduler;

  KafkaMessageSenderPoolReporter(String poolName, KafkaMessageSenderPoolRouter router) {
    this.poolName = poolName;
    this.router = router;
    this.scheduler =
        Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder()
                .setNameFormat("pool-reporter-" + poolName + "-%d")
                .setDaemon(true)
                .build());
    this.scheduler.scheduleAtFixedRate(
        this::logDistribution, REPORT_INTERVAL_HOURS, REPORT_INTERVAL_HOURS, TimeUnit.HOURS);
  }

  private void logDistribution() {
    try {
      int[] distribution = router.getDistribution();
      int totalTopics = Arrays.stream(distribution).sum();
      logger.info(
          "Producer pool status [pool={}, poolSize={}, totalTopics={}]: distribution={}",
          poolName,
          router.poolSize(),
          totalTopics,
          Arrays.toString(distribution));
    } catch (Exception e) {
      logger.warn("Failed to log producer pool distribution for pool={}", poolName, e);
    }
  }

  void close() {
    scheduler.shutdownNow();
  }
}
