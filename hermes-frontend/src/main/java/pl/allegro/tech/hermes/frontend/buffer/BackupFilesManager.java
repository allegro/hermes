package pl.allegro.tech.hermes.frontend.buffer;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.util.Optional;

import static java.lang.String.format;

public class BackupFilesManager {

    private static final Logger logger = LoggerFactory.getLogger(BackupFilesManager.class);

    private static final String FILE_NAME = "hermes-buffer";

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

        File timestampedBackupFile = new File(format("%s/%s-%s.dat", baseDir, FILE_NAME, clock.millis()));

        try {
            FileUtils.moveFile(backupFile, timestampedBackupFile);
        } catch (IOException e) {
            logger.error("Error while moving backup file from path {} to path {}.",
                backupFile.getAbsolutePath(),
                timestampedBackupFile.getAbsolutePath(),
                e);
            return Optional.empty();
        }

        return Optional.of(timestampedBackupFile);
    }

    public void delete(File file) {
        boolean status = FileUtils.deleteQuietly(file);
        logger.info("Deleted file from path {} with status {}", file.getAbsolutePath(), status);
    }
    
    private File getBackupFile() {
        return new File(format("%s/%s.dat", baseDir, FILE_NAME));
    }
}
