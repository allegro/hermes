package pl.allegro.tech.hermes.consumers.message.undelivered;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;

public class UndeliveredMessageLogPersister {

  private final Duration period;
  private final UndeliveredMessageLog undeliveredMessageLog;
  private final ScheduledExecutorService scheduledExecutorService;

  public UndeliveredMessageLogPersister(
      UndeliveredMessageLog undeliveredMessageLog, Duration undeliveredMessageLogPersistPeriod) {
    this.undeliveredMessageLog = undeliveredMessageLog;
    this.period = undeliveredMessageLogPersistPeriod;
    this.scheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder()
                .setNameFormat("undelivered-message-log-persister-%d")
                .build());
  }

  public void start() {
    scheduledExecutorService.scheduleAtFixedRate(
        undeliveredMessageLog::persist, period.toMillis(), period.toMillis(), MILLISECONDS);
  }

  public void shutdown() {
    scheduledExecutorService.shutdown();
  }
}
