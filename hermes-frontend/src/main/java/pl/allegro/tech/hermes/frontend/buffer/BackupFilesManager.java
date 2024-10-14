package pl.allegro.tech.hermes.frontend.buffer;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import com.google.common.io.PatternFilenameFilter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupFilesManager {

  private static final Logger logger = LoggerFactory.getLogger(BackupFilesManager.class);

  private static final String FILE_NAME = "hermes-buffer-v3";
  private static final String TIMESTAMPED_BACKUP_FILE_PATTERN = FILE_NAME + "-\\d+\\.dat";
  private static final String OLD_V2_BACKUP_PREFIX = "hermes-buffer";
  private static final String OLD_V2_BACKUP_SUFFIX = "-v2-old.tmp";

  private final String baseDir;
  private final Clock clock;
  private final File backupFile;

  public BackupFilesManager(String baseDir, Clock clock) {
    this.baseDir = baseDir;
    this.clock = clock;

    backupFile = getBackupFile();
  }

  public File getCurrentBackupFile() {
    return backupFile;
  }

  public Optional<File> rolloverBackupFileIfExists() {
    if (!backupFile.exists()) {
      logger.info("Backup file doesn't exist.");
      return Optional.empty();
    }

    File timestampedBackupFile =
        new File(format("%s/%s-%s.dat", baseDir, FILE_NAME, clock.millis()));

    try {
      FileUtils.moveFile(backupFile, timestampedBackupFile);
    } catch (IOException e) {
      logger.error(
          "Error while moving backup file from path {} to path {}.",
          backupFile.getAbsolutePath(),
          timestampedBackupFile.getAbsolutePath(),
          e);
      return Optional.empty();
    }

    return Optional.of(timestampedBackupFile);
  }

  public List<File> getTemporaryBackupV2Files(String temporaryDir) {
    try {
      Path dir = Paths.get(temporaryDir);
      return Files.list(dir)
          .filter(file -> file.getFileName().toString().startsWith(OLD_V2_BACKUP_PREFIX))
          .filter(file -> file.getFileName().toString().endsWith(OLD_V2_BACKUP_SUFFIX))
          .map(Path::toFile)
          .collect(toList());
    } catch (IOException e) {
      logger.error(
          "Error while scanning temporary backup v2 files from absolute path: {}",
          backupFile.getAbsolutePath(),
          e);
      return Collections.emptyList();
    }
  }

  public void delete(File file) {
    boolean status = FileUtils.deleteQuietly(file);
    logger.info("Deleted file from path {} with status {}", file.getAbsolutePath(), status);
  }

  private File getBackupFile() {
    return new File(format("%s/%s.dat", baseDir, FILE_NAME));
  }

  public List<File> getRolledBackupFiles() {
    return newArrayList(
        new File(baseDir).listFiles(new PatternFilenameFilter(TIMESTAMPED_BACKUP_FILE_PATTERN)));
  }
}
