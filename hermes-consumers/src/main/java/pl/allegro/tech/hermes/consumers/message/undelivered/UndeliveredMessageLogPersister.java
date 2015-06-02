package pl.allegro.tech.hermes.consumers.message.undelivered;

import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;

import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class UndeliveredMessageLogPersister {

    private final int periodMs;
    private final UndeliveredMessageLog undeliveredMessageLog;
    private ScheduledExecutorService scheduledExecutorService;

    @Inject
    public UndeliveredMessageLogPersister(UndeliveredMessageLog undeliveredMessageLog, ConfigFactory configFactory) {
        this.undeliveredMessageLog = undeliveredMessageLog;
        this.periodMs = configFactory.getIntProperty(Configs.UNDELIVERED_MESSAGE_LOG_PERSIST_PERIOD_MS);
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        scheduledExecutorService.scheduleAtFixedRate(undeliveredMessageLog::persist, periodMs, periodMs, MILLISECONDS);
    }

    public void shutdown() {
        scheduledExecutorService.shutdown();
    }

}
