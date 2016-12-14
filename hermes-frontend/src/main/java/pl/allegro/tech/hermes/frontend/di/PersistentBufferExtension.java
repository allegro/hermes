package pl.allegro.tech.hermes.frontend.di;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.List;

import static java.util.stream.Collectors.joining;
import static pl.allegro.tech.hermes.common.config.Configs.*;

public class PersistentBufferExtension {

    private static final Logger logger = LoggerFactory.getLogger(PersistentBufferExtension.class);
    private static final int MEGABYTES_TO_BYTES_MULTIPLIER = 1024 * 1024;

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

        backupFilesManager.rolloverBackupFileIfExists();
        List<File> rolledBackupFiles = backupFilesManager.getRolledBackupFiles();
        if (!rolledBackupFiles.isEmpty()) {
            logger.info("Backup files were found. Number of files: {}. Files: {}",
                    rolledBackupFiles.size(),
                    rolledBackupFiles.stream().map(f -> f.getName()).collect(joining(", ")));

            hooksHandler.addStartupHook((s) -> {
                rolledBackupFiles.forEach(f -> loadOldMessages(backupFilesManager, f));
                backupMessagesLoader.clearTopicsAvailabilityCache();
            });
        }

        if (config.getBooleanProperty(MESSAGES_LOCAL_STORAGE_ENABLED)) {
            int backupStorageSizeInBytes = config.getIntProperty(MESSAGES_LOCAL_STORAGE_SIZE_MB) * MEGABYTES_TO_BYTES_MULTIPLIER;
            int entries = backupStorageSizeInBytes / config.getIntProperty(MESSAGES_LOCAL_STORAGE_AVERAGE_MESSAGE_SIZE);

            MessageRepository repository = ChronicleMapMessageRepository.create(
                    backupFilesManager.getCurrentBackupFile(),
                    entries,
                    config.getIntProperty(MESSAGES_LOCAL_STORAGE_AVERAGE_MESSAGE_SIZE));
            BrokerListener brokerListener = new BrokerListener(repository);

            listeners.addAcknowledgeListener(brokerListener);
            listeners.addErrorListener(brokerListener);
            listeners.addTimeoutListener(brokerListener);
        }
    }

    private void loadOldMessages(BackupFilesManager backupFilesManager, File oldBackup) {
        logger.info("Loading messages from backup file: {}", oldBackup.getName());
        MessageRepository oldMessageRepository = ChronicleMapMessageRepository.recover(oldBackup);
        backupMessagesLoader.loadMessages(oldMessageRepository);
        oldMessageRepository.close();
        backupFilesManager.delete(oldBackup);
    }

}
