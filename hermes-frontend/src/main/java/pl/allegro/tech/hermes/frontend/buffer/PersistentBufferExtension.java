package pl.allegro.tech.hermes.frontend.buffer;

import static java.util.stream.Collectors.joining;

import java.io.File;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.buffer.chronicle.ChronicleMapMessageRepository;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;

public class PersistentBufferExtension {

  private static final Logger logger = LoggerFactory.getLogger(PersistentBufferExtension.class);

  private final PersistentBufferExtensionParameters persistentBufferExtensionParameters;

  private final Clock clock;

  private final BrokerListeners listeners;

  private final BackupMessagesLoader backupMessagesLoader;
  private final MetricsFacade metricsFacade;

  private int entries;
  private int avgMessageSize;

  public PersistentBufferExtension(
      PersistentBufferExtensionParameters persistentBufferExtensionParameters,
      Clock clock,
      BrokerListeners listeners,
      BackupMessagesLoader backupMessagesLoader,
      MetricsFacade metricsFacade) {
    this.persistentBufferExtensionParameters = persistentBufferExtensionParameters;
    this.clock = clock;
    this.listeners = listeners;
    this.backupMessagesLoader = backupMessagesLoader;
    this.metricsFacade = metricsFacade;
  }

  public void extend() {
    BackupFilesManager backupFilesManager =
        new BackupFilesManager(persistentBufferExtensionParameters.getDirectory(), clock);

    long backupStorageSizeInBytes = persistentBufferExtensionParameters.getBufferedSizeBytes();
    avgMessageSize = persistentBufferExtensionParameters.getAverageMessageSize();

    entries = (int) (backupStorageSizeInBytes / avgMessageSize);

    if (persistentBufferExtensionParameters.isV2MigrationEnabled()) {
      loadTemporaryBackupV2Files(backupFilesManager);
    }

    backupFilesManager.rolloverBackupFileIfExists();
    List<File> rolledBackupFiles = backupFilesManager.getRolledBackupFiles();
    if (!rolledBackupFiles.isEmpty()) {
      rollBackupFiles(backupFilesManager, rolledBackupFiles);
    }

    if (persistentBufferExtensionParameters.isEnabled()) {
      enableLocalStorage(backupFilesManager);
    }
  }

  private void loadTemporaryBackupV2Files(BackupFilesManager backupFilesManager) {
    String temporaryDir = persistentBufferExtensionParameters.getTemporaryDirectory();
    List<File> temporaryBackupV2Files = backupFilesManager.getTemporaryBackupV2Files(temporaryDir);
    temporaryBackupV2Files.forEach(f -> loadTemporaryBackupV2Messages(backupFilesManager, f));
    backupMessagesLoader.clearTopicsAvailabilityCache();
  }

  private void rollBackupFiles(
      BackupFilesManager backupFilesManager, List<File> rolledBackupFiles) {
    logger.info(
        "Backup files were found. Number of files: {}. Files: {}",
        rolledBackupFiles.size(),
        rolledBackupFiles.stream().map(File::getName).collect(joining(", ")));
    rolledBackupFiles.forEach(f -> loadOldMessages(backupFilesManager, f));
    backupMessagesLoader.clearTopicsAvailabilityCache();
  }

  private void enableLocalStorage(BackupFilesManager backupFilesManager) {
    MessageRepository repository =
        persistentBufferExtensionParameters.isSizeReportingEnabled()
            ? new ChronicleMapMessageRepository(
                backupFilesManager.getCurrentBackupFile(), entries, avgMessageSize, metricsFacade)
            : new ChronicleMapMessageRepository(
                backupFilesManager.getCurrentBackupFile(), entries, avgMessageSize);

    BrokerListener brokerListener = new BrokerListener(repository);

    listeners.addAcknowledgeListener(brokerListener);
    listeners.addErrorListener(brokerListener);
    listeners.addTimeoutListener(brokerListener);
  }

  private void loadTemporaryBackupV2Messages(
      BackupFilesManager backupFilesManager, File temporaryBackup) {
    logger.info("Loading messages from temporary backup v2 file: {}", temporaryBackup.getName());
    backupMessagesLoader.loadFromTemporaryBackupV2File(temporaryBackup);
    backupFilesManager.delete(temporaryBackup);
  }

  private void loadOldMessages(BackupFilesManager backupFilesManager, File oldBackup) {
    logger.info("Loading messages from backup file: {}", oldBackup.getName());
    MessageRepository oldMessageRepository =
        new ChronicleMapMessageRepository(oldBackup, entries, avgMessageSize);
    backupMessagesLoader.loadMessages(oldMessageRepository.findAll());
    oldMessageRepository.close();
    backupFilesManager.delete(oldBackup);
  }
}
