package pl.allegro.tech.hermes.frontend.publishing.message.preview;

import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_MESSAGE_PREVIEW_ENABLED;

public class PreviewMessagePersister {
    private final int periodMs;
    private final PreviewMessageLog previewMessageLog;
    private ScheduledExecutorService scheduledExecutorService;
    private final boolean previewEnabled;

    @Inject
    public PreviewMessagePersister(PreviewMessageLog previewMessageLog, ConfigFactory configFactory) {
        this.previewMessageLog = previewMessageLog;
        this.previewEnabled = configFactory.getBooleanProperty(FRONTEND_MESSAGE_PREVIEW_ENABLED);
        this.periodMs = configFactory.getIntProperty(Configs.FRONTEND_MESSAGE_PREVIEW_LOG_PERSIST_PERIOD_MS);
        if (previewEnabled) {
            this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        } else {
            this.scheduledExecutorService = null;
        }
    }

    public void start() {
        if (previewEnabled) {
            scheduledExecutorService.scheduleAtFixedRate(previewMessageLog::persist, periodMs, periodMs, MILLISECONDS);
        }
    }

    public void shutdown() {
        if (previewEnabled) {
            scheduledExecutorService.shutdown();
        }
    }

}
