package pl.allegro.tech.hermes.consumers.message.undelivered;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class UndeliveredMessageLogPersister {

    private final int periodMs;
    private final UndeliveredMessageLog undeliveredMessageLog;
    private final ScheduledExecutorService scheduledExecutorService;

    public UndeliveredMessageLogPersister(UndeliveredMessageLog undeliveredMessageLog, int undeliveredMessageLogPersistPeriodMs) {
        this.undeliveredMessageLog = undeliveredMessageLog;
        this.periodMs = undeliveredMessageLogPersistPeriodMs;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("undelivered-message-log-persister-%d").build());
    }

    public void start() {
        scheduledExecutorService.scheduleAtFixedRate(undeliveredMessageLog::persist, periodMs, periodMs, MILLISECONDS);
    }

    public void shutdown() {
        scheduledExecutorService.shutdown();
    }

}
