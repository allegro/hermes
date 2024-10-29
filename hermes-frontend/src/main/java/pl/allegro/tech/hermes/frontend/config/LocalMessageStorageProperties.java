package pl.allegro.tech.hermes.frontend.config;

import com.google.common.io.Files;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.frontend.buffer.BackupMessagesLoaderParameters;
import pl.allegro.tech.hermes.frontend.buffer.PersistentBufferExtensionParameters;

@ConfigurationProperties(prefix = "frontend.messages.local.storage")
public class LocalMessageStorageProperties
    implements BackupMessagesLoaderParameters, PersistentBufferExtensionParameters {

  private long bufferedSizeBytes = 256 * 1024 * 1024L;

  private boolean v2MigrationEnabled = true;

  private boolean enabled = false;

  private String directory = Files.createTempDir().getAbsolutePath();

  private String temporaryDirectory = Files.createTempDir().getAbsolutePath();

  private int averageMessageSize = 600;

  private Duration maxAge = Duration.ofHours(72);

  private int maxResendRetries = 5;

  private Duration loadingPauseBetweenResend = Duration.ofMillis(30);

  private Duration loadingWaitForBrokerTopicInfo = Duration.ofSeconds(5);

  private boolean sizeReportingEnabled = true;

  @Override
  public long getBufferedSizeBytes() {
    return bufferedSizeBytes;
  }

  public void setBufferedSizeBytes(long bufferedSizeBytes) {
    this.bufferedSizeBytes = bufferedSizeBytes;
  }

  @Override
  public boolean isV2MigrationEnabled() {
    return v2MigrationEnabled;
  }

  public void setV2MigrationEnabled(boolean v2MigrationEnabled) {
    this.v2MigrationEnabled = v2MigrationEnabled;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public String getDirectory() {
    return directory;
  }

  public void setDirectory(String directory) {
    this.directory = directory;
  }

  @Override
  public String getTemporaryDirectory() {
    return temporaryDirectory;
  }

  public void setTemporaryDirectory(String temporaryDirectory) {
    this.temporaryDirectory = temporaryDirectory;
  }

  @Override
  public int getAverageMessageSize() {
    return averageMessageSize;
  }

  public void setAverageMessageSize(int averageMessageSize) {
    this.averageMessageSize = averageMessageSize;
  }

  @Override
  public Duration getMaxAge() {
    return maxAge;
  }

  public void setMaxAge(Duration maxAge) {
    this.maxAge = maxAge;
  }

  @Override
  public int getMaxResendRetries() {
    return maxResendRetries;
  }

  public void setMaxResendRetries(int maxResendRetries) {
    this.maxResendRetries = maxResendRetries;
  }

  @Override
  public Duration getLoadingPauseBetweenResend() {
    return loadingPauseBetweenResend;
  }

  public void setLoadingPauseBetweenResend(Duration loadingPauseBetweenResend) {
    this.loadingPauseBetweenResend = loadingPauseBetweenResend;
  }

  @Override
  public Duration getLoadingWaitForBrokerTopicInfo() {
    return loadingWaitForBrokerTopicInfo;
  }

  public void setLoadingWaitForBrokerTopicInfo(Duration loadingWaitForBrokerTopicInfo) {
    this.loadingWaitForBrokerTopicInfo = loadingWaitForBrokerTopicInfo;
  }

  @Override
  public boolean isSizeReportingEnabled() {
    return sizeReportingEnabled;
  }

  public void setSizeReportingEnabled(boolean sizeReportingEnabled) {
    this.sizeReportingEnabled = sizeReportingEnabled;
  }
}
