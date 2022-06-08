package pl.allegro.tech.hermes.frontend.publishing.preview;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreviewRepository;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_MESSAGE_PREVIEW_ENABLED;

public class DefaultMessagePreviewPersister implements MessagePreviewPersister {

    private final int period;

    private final MessagePreviewLog messagePreviewLog;

    private final MessagePreviewRepository repository;

    private final Optional<ScheduledExecutorService> scheduledExecutorService;

    public DefaultMessagePreviewPersister(MessagePreviewLog messagePreviewLog, MessagePreviewRepository repository, ConfigFactory configFactory) {
        this.messagePreviewLog = messagePreviewLog;
        this.repository = repository;
        this.period = configFactory.getIntProperty(Configs.FRONTEND_MESSAGE_PREVIEW_LOG_PERSIST_PERIOD);

        boolean previewEnabled = configFactory.getBooleanProperty(FRONTEND_MESSAGE_PREVIEW_ENABLED);
        this.scheduledExecutorService = previewEnabled ? Optional.of(Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("message-preview-persister-%d").build())) : Optional.empty();
    }

    @Override
    public void start() {
        scheduledExecutorService.ifPresent(s -> s.scheduleAtFixedRate(this::persist, period, period, TimeUnit.SECONDS));
    }

    private void persist() {
        repository.persist(messagePreviewLog.snapshotAndClean());
    }

    @Override
    public void shutdown() {
        scheduledExecutorService.ifPresent(ExecutorService::shutdown);
    }

}
