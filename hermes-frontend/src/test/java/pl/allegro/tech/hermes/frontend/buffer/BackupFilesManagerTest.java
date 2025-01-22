package pl.allegro.tech.hermes.frontend.buffer;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BackupFilesManagerTest {

  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(12345), ZoneId.systemDefault());

  private File tempDir;

  @Before
  public void setup() {
    tempDir = Files.createTempDir();
  }

  @After
  public void cleanup() throws IOException {
    FileUtils.deleteDirectory(tempDir);
  }

  @Test
  public void shouldRolloverExistingBackupFile() throws IOException {
    // given
    new File(tempDir, "hermes-buffer-v3.dat").createNewFile();
    BackupFilesManager backupFilesManager =
        new BackupFilesManager(tempDir.getAbsolutePath(), clock);

    // when
    Optional<File> backupFile = backupFilesManager.rolloverBackupFileIfExists();

    // then
    assertThat(backupFile.get().getName()).isEqualTo("hermes-buffer-v3-12345.dat");
  }

  @Test
  public void shouldReadBackupFilesList() throws IOException {
    // given
    final BackupFilesManager backupFilesManager =
        new BackupFilesManager(tempDir.getAbsolutePath(), clock);
    File timestampedBackup1 = new File(tempDir, "hermes-buffer-v3-001.dat");
    File timestampedBackup2 = new File(tempDir, "hermes-buffer-v3-002.dat");
    File customBackup = new File(tempDir, "hermes-buffer-v3-old.dat");

    // and
    timestampedBackup1.createNewFile();
    timestampedBackup2.createNewFile();
    customBackup.createNewFile();

    // when
    List<File> backups = backupFilesManager.getRolledBackupFiles();

    // then
    assertThat(backups).containsOnly(timestampedBackup1, timestampedBackup2);
  }

  @Test
  public void shouldReadEmptyBackupFileList() {
    // given
    BackupFilesManager backupFilesManager =
        new BackupFilesManager(tempDir.getAbsolutePath(), clock);

    // when
    List<File> backups = backupFilesManager.getRolledBackupFiles();

    // then
    assertThat(backups).isEmpty();
  }

  @Test
  public void shouldNotRolloverNotExistsBackupFile() {
    // given
    BackupFilesManager backupFilesManager =
        new BackupFilesManager(tempDir.getAbsolutePath(), clock);
    // when
    Optional<File> backupFile = backupFilesManager.rolloverBackupFileIfExists();

    // when & then
    assertThat(backupFile.isPresent()).isFalse();
  }

  @Test
  public void shouldUseNewNamingSchemeWhenNoFilesAvailable() {
    // given
    BackupFilesManager backupFilesManager =
        new BackupFilesManager(tempDir.getAbsolutePath(), clock);

    // when
    File file = backupFilesManager.getCurrentBackupFile();

    // then
    assertThat(file.getName()).isEqualTo("hermes-buffer-v3.dat");
  }

  @Test
  public void shouldLoadAllTemporaryBackupV2Files() throws IOException {
    // given
    final BackupFilesManager backupFilesManager =
        new BackupFilesManager(tempDir.getAbsolutePath(), clock);
    File temporaryBackup1 = new File(tempDir, "hermes-buffer-v2-old.tmp");
    File temporaryBackup2 = new File(tempDir, "hermes-buffer-002-v2-old.tmp");
    File customBackup = new File(tempDir, "hermes-buffer.dat");

    // and
    temporaryBackup1.createNewFile();
    temporaryBackup2.createNewFile();
    customBackup.createNewFile();

    // when
    List<File> files = backupFilesManager.getTemporaryBackupV2Files(tempDir.getAbsolutePath());

    // then
    assertThat(files.size()).isEqualTo(2);
  }
}
