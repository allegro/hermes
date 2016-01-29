package pl.allegro.tech.hermes.frontend.buffer;

import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class BackupFilesManagerTest {

    private Clock clock = Clock.fixed(Instant.ofEpochMilli(12345), ZoneId.systemDefault());

    private File tempDir;

    @Before
    public void setup() throws IOException {
        tempDir = Files.createTempDir();
    }

    @Test
    public void shouldRolloverExistingBackupFile() throws IOException {
        // given
        new File(tempDir, "hermes-buffer.dat").createNewFile();
        BackupFilesManager backupFilesManager = new BackupFilesManager(tempDir.getAbsolutePath(), clock);

        // when
        Optional<File> backupFile = backupFilesManager.rolloverBackupFileIfExists();

        // then
        assertThat(backupFile.get().getName()).isEqualTo("hermes-buffer-12345.dat");
    }

    @Test
    public void shouldNotRolloverNotExistsBackupFile() {
        // given
        BackupFilesManager backupFilesManager = new BackupFilesManager(tempDir.getAbsolutePath(), clock);
        // when
        Optional<File> backupFile = backupFilesManager.rolloverBackupFileIfExists();
        
        // when & then
        assertThat(backupFile.isPresent()).isFalse();
    }

    @Test
    public void shouldUseNewNamingSchemeWhenNoFilesAvailable() throws IOException {
        // given
        BackupFilesManager backupFilesManager = new BackupFilesManager(tempDir.getAbsolutePath(), clock);

        // when
        File file = backupFilesManager.getCurrentBackupFile();

        // then
        assertThat(file.getName()).isEqualTo("hermes-buffer.dat");
    }
}