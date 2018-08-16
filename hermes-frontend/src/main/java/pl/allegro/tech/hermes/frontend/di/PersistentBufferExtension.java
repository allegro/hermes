package pl.allegro.tech.hermes.frontend.di;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.hook.HooksHandler;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.buffer.BackupFilesManager;
import pl.allegro.tech.hermes.frontend.buffer.BackupMessage;
import pl.allegro.tech.hermes.frontend.buffer.BackupMessagesLoader;
import pl.allegro.tech.hermes.frontend.buffer.BrokerListener;
import pl.allegro.tech.hermes.frontend.buffer.MessageRepository;
import pl.allegro.tech.hermes.frontend.buffer.chronicle.ChronicleMapMessageRepository;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.List;

import static java.util.stream.Collectors.joining;
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGES_LOCAL_STORAGE_AVERAGE_MESSAGE_SIZE;
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGES_LOCAL_STORAGE_DIRECTORY;
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGES_LOCAL_STORAGE_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGES_LOCAL_STORAGE_SIZE_MB;
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGES_LOCAL_STORAGE_SIZE_REPORTING_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGES_LOCAL_STORAGE_V2_MIGRATION_ENABLED;

public class PersistentBufferExtension {

    private static final Logger logger = LoggerFactory.getLogger(PersistentBufferExtension.class);

    private static final int MEGABYTES_TO_BYTES_MULTIPLIER = 1024 * 1024;
    private static final String OLD_BACKFILE_SUFFIX = "-v2-old.tmp";

    private final ConfigFactory config;

    private final Clock clock;

    private final BrokerListeners listeners;

    private final HooksHandler hooksHandler;

    private final BackupMessagesLoader backupMessagesLoader;
    private final HermesMetrics hermesMetrics;

    private File file;

    @Inject
    public PersistentBufferExtension(ConfigFactory configFactory,
                                     Clock clock,
                                     BrokerListeners listeners,
                                     HooksHandler hooksHandler,
                                     BackupMessagesLoader backupMessagesLoader,
                                     HermesMetrics hermesMetrics) {
        this.config = configFactory;
        this.clock = clock;
        this.listeners = listeners;
        this.hooksHandler = hooksHandler;
        this.backupMessagesLoader = backupMessagesLoader;
        this.hermesMetrics = hermesMetrics;
    }

    public void extend() {
        BackupFilesManager backupFilesManager = new BackupFilesManager(
                config.getStringProperty(MESSAGES_LOCAL_STORAGE_DIRECTORY),
                clock);

        if (config.getBooleanProperty(MESSAGES_LOCAL_STORAGE_V2_MIGRATION_ENABLED)) {
            loadOldV2Messages(config.getStringProperty(MESSAGES_LOCAL_STORAGE_DIRECTORY));
        }
        backupFilesManager.rolloverBackupFileIfExists();
        List<File> rolledBackupFiles = backupFilesManager.getRolledBackupFiles();
        if (!rolledBackupFiles.isEmpty()) {
            logger.info("Backup files were found. Number of files: {}. Files: {}",
                    rolledBackupFiles.size(),
                    rolledBackupFiles.stream().map(File::getName).collect(joining(", ")));

            hooksHandler.addStartupHook((s) -> {
                rolledBackupFiles.forEach(f -> loadOldMessages(backupFilesManager, f));
                backupMessagesLoader.clearTopicsAvailabilityCache();
            });
        }

        if (config.getBooleanProperty(MESSAGES_LOCAL_STORAGE_ENABLED)) {
            int backupStorageSizeInBytes = config.getIntProperty(MESSAGES_LOCAL_STORAGE_SIZE_MB) * MEGABYTES_TO_BYTES_MULTIPLIER;
            int entries = backupStorageSizeInBytes / config.getIntProperty(MESSAGES_LOCAL_STORAGE_AVERAGE_MESSAGE_SIZE);
            int avgMessageSize = config.getIntProperty(MESSAGES_LOCAL_STORAGE_AVERAGE_MESSAGE_SIZE);

            MessageRepository repository = config.getBooleanProperty(MESSAGES_LOCAL_STORAGE_SIZE_REPORTING_ENABLED)
                    ? ChronicleMapMessageRepository.create(backupFilesManager.getCurrentBackupFile(), hermesMetrics, entries, avgMessageSize)
                    : ChronicleMapMessageRepository.create(backupFilesManager.getCurrentBackupFile(), entries, avgMessageSize);

            BrokerListener brokerListener = new BrokerListener(repository);

            listeners.addAcknowledgeListener(brokerListener);
            listeners.addErrorListener(brokerListener);
            listeners.addTimeoutListener(brokerListener);
        }
    }

    private void loadOldMessages(BackupFilesManager backupFilesManager, File oldBackup) {
        logger.info("Loading messages from backup file: {}", oldBackup.getName());
        MessageRepository oldMessageRepository = ChronicleMapMessageRepository.recover(oldBackup);
        backupMessagesLoader.loadMessages(oldMessageRepository.findAll());
        oldMessageRepository.close();
        backupFilesManager.delete(oldBackup);
    }

    private void loadOldV2Messages(String rootDir) {
        try {
            Path dir = Paths.get(rootDir);
            Files.list(dir)
                    .filter(file -> file.getFileName().toString().endsWith(OLD_BACKFILE_SUFFIX))
                    .forEach(file -> {
                        try (
                                FileInputStream fileInputStream = new FileInputStream(file.getFileName().toString());
                                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                        ) {
                            List<BackupMessage> messages = (List<BackupMessage>) objectInputStream.readObject();
                            backupMessagesLoader.loadMessages(messages);

                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                            logger.info("Loading messages from temporary v2 backup file: {}", file.getFileName().toString());
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
