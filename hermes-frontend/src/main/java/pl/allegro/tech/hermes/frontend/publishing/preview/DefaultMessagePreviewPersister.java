package pl.allegro.tech.hermes.frontend.publishing.preview;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreviewRepository;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DefaultMessagePreviewPersister implements MessagePreviewPersister {

    private final int period;

    private final MessagePreviewLog messagePreviewLog;

    private final MessagePreviewRepository repository;

    private final Optional<ScheduledExecutorService> scheduledExecutorService;

    public DefaultMessagePreviewPersister(MessagePreviewLog messagePreviewLog, MessagePreviewRepository repository, int logPersistPeriod, boolean previewEnabled) {
        this.messagePreviewLog = messagePreviewLog;
        this.repository = repository;
        this.period = logPersistPeriod;

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
