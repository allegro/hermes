package pl.allegro.tech.hermes.frontend.di;

import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.hook.HooksHandler;
import pl.allegro.tech.hermes.frontend.buffer.BackupFilesManager;
import pl.allegro.tech.hermes.frontend.buffer.BackupMessagesLoader;
import pl.allegro.tech.hermes.frontend.buffer.BrokerListener;
import pl.allegro.tech.hermes.frontend.buffer.MessageRepository;
import pl.allegro.tech.hermes.frontend.buffer.chronicle.ChronicleMapMessageRepository;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;

import javax.inject.Inject;
import java.io.File;
import java.time.Clock;
import java.util.Optional;

import static pl.allegro.tech.hermes.common.config.Configs.MESSAGES_LOCAL_STORAGE_DIRECTORY;
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGES_LOCAL_STORAGE_ENABLED;

public class PersistentBufferExtension {

    private final ConfigFactory config;

    private final Clock clock;

    private final BrokerListeners listeners;

    private final HooksHandler hooksHandler;

    private final BackupMessagesLoader backupMessagesLoader;

    @Inject
    public PersistentBufferExtension(ConfigFactory configFactory,
                                     Clock clock,
                                     BrokerListeners listeners,
                                     HooksHandler hooksHandler,
                                     BackupMessagesLoader backupMessagesLoader) {
        this.config = configFactory;
        this.clock = clock;
        this.listeners = listeners;
        this.hooksHandler = hooksHandler;
        this.backupMessagesLoader = backupMessagesLoader;
    }

    public void extend() {
        BackupFilesManager backupFilesManager = new BackupFilesManager(
                config.getStringProperty(MESSAGES_LOCAL_STORAGE_DIRECTORY),
                clock);

        Optional<File> optionalOldBackup = backupFilesManager.rolloverBackupFileIfExists();
        optionalOldBackup.ifPresent(f ->
                hooksHandler.addStartupHook((s) -> loadOldMessages(backupFilesManager, f))
        );

        if (config.getBooleanProperty(MESSAGES_LOCAL_STORAGE_ENABLED)) {
            MessageRepository repository = new ChronicleMapMessageRepository(backupFilesManager.getCurrentBackupFile());
            BrokerListener brokerListener = new BrokerListener(repository);

            listeners.addAcknowledgeListener(brokerListener);
            listeners.addErrorListener(brokerListener);
            listeners.addTimeoutListener(brokerListener);
        }
    }

    private void loadOldMessages(BackupFilesManager backupFilesManager, File oldBackup) {
        MessageRepository oldMessageRepository = new ChronicleMapMessageRepository(oldBackup);
        backupMessagesLoader.loadMessages(oldMessageRepository);
        oldMessageRepository.close();
        backupFilesManager.delete(oldBackup);
    }

}
