package pl.allegro.tech.hermes.frontend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
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
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGES_LOCAL_BUFFERED_STORAGE_SIZE;
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGES_LOCAL_STORAGE_AVERAGE_MESSAGE_SIZE;
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGES_LOCAL_STORAGE_DIRECTORY;
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGES_LOCAL_STORAGE_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGES_LOCAL_STORAGE_SIZE_REPORTING_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGES_LOCAL_STORAGE_TEMPORARY_DIRECTORY;
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGES_LOCAL_STORAGE_V2_MIGRATION_ENABLED;

public class PersistentBufferExtension {

    private static final Logger logger = LoggerFactory.getLogger(PersistentBufferExtension.class);

    private final ConfigFactory config;

    private final Clock clock;

    private final BrokerListeners listeners;

    private final BackupMessagesLoader backupMessagesLoader;
    private final HermesMetrics hermesMetrics;

    private int entries;
    private int avgMessageSize;

    @Inject
    public PersistentBufferExtension(ConfigFactory configFactory,
                                     Clock clock,
                                     BrokerListeners listeners,
                                     BackupMessagesLoader backupMessagesLoader,
                                     HermesMetrics hermesMetrics) {
        this.config = configFactory;
        this.clock = clock;
        this.listeners = listeners;
        this.backupMessagesLoader = backupMessagesLoader;
        this.hermesMetrics = hermesMetrics;
    }

    public void extend() {
        BackupFilesManager backupFilesManager = new BackupFilesManager(
                config.getStringProperty(MESSAGES_LOCAL_STORAGE_DIRECTORY),
                clock);

        long backupStorageSizeInBytes = config.getLongProperty(MESSAGES_LOCAL_BUFFERED_STORAGE_SIZE);
        entries = (int) (backupStorageSizeInBytes / config.getIntProperty(MESSAGES_LOCAL_STORAGE_AVERAGE_MESSAGE_SIZE));
        avgMessageSize = config.getIntProperty(MESSAGES_LOCAL_STORAGE_AVERAGE_MESSAGE_SIZE);

        if (config.getBooleanProperty(MESSAGES_LOCAL_STORAGE_V2_MIGRATION_ENABLED)) {
            loadTemporaryBackupV2Files(backupFilesManager);
        }

        backupFilesManager.rolloverBackupFileIfExists();
        List<File> rolledBackupFiles = backupFilesManager.getRolledBackupFiles();
        if (!rolledBackupFiles.isEmpty()) {
            rollBackupFiles(backupFilesManager, rolledBackupFiles);
        }

        if (config.getBooleanProperty(MESSAGES_LOCAL_STORAGE_ENABLED)) {
            enableLocalStorage(backupFilesManager);
        }
    }

    private void loadTemporaryBackupV2Files(BackupFilesManager backupFilesManager) {
        String temporaryDir = config.getStringProperty(MESSAGES_LOCAL_STORAGE_TEMPORARY_DIRECTORY);
        List<File> temporaryBackupV2Files = backupFilesManager.getTemporaryBackupV2Files(temporaryDir);
        temporaryBackupV2Files.forEach(f -> loadTemporaryBackupV2Messages(backupFilesManager, f));
        backupMessagesLoader.clearTopicsAvailabilityCache();
    }

    private void rollBackupFiles(BackupFilesManager backupFilesManager, List<File> rolledBackupFiles) {
        logger.info("Backup files were found. Number of files: {}. Files: {}",
                rolledBackupFiles.size(),
                rolledBackupFiles.stream().map(File::getName).collect(joining(", ")));
        rolledBackupFiles.forEach(f -> loadOldMessages(backupFilesManager, f));
            backupMessagesLoader.clearTopicsAvailabilityCache();
    }

    private void enableLocalStorage(BackupFilesManager backupFilesManager) {
        MessageRepository repository = config.getBooleanProperty(MESSAGES_LOCAL_STORAGE_SIZE_REPORTING_ENABLED)
                ? new ChronicleMapMessageRepository(backupFilesManager.getCurrentBackupFile(), entries, avgMessageSize, hermesMetrics)
                : new ChronicleMapMessageRepository(backupFilesManager.getCurrentBackupFile(), entries, avgMessageSize);

        BrokerListener brokerListener = new BrokerListener(repository);

        listeners.addAcknowledgeListener(brokerListener);
        listeners.addErrorListener(brokerListener);
        listeners.addTimeoutListener(brokerListener);
    }

    private void loadTemporaryBackupV2Messages(BackupFilesManager backupFilesManager, File temporaryBackup) {
        logger.info("Loading messages from temporary backup v2 file: {}", temporaryBackup.getName());
        backupMessagesLoader.loadFromTemporaryBackupV2File(temporaryBackup);
        backupFilesManager.delete(temporaryBackup);
    }

    private void loadOldMessages(BackupFilesManager backupFilesManager, File oldBackup) {
        logger.info("Loading messages from backup file: {}", oldBackup.getName());
        MessageRepository oldMessageRepository = new ChronicleMapMessageRepository(oldBackup, entries, avgMessageSize);
        backupMessagesLoader.loadMessages(oldMessageRepository.findAll());
        oldMessageRepository.close();
        backupFilesManager.delete(oldBackup);
    }
}
