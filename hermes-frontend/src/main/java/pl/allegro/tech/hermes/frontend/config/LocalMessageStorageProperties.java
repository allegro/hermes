package pl.allegro.tech.hermes.frontend.config;

import com.google.common.io.Files;
import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.frontend.buffer.BackupMessagesLoaderParameters;
import pl.allegro.tech.hermes.frontend.buffer.PersistentBufferExtensionParameters;

@ConfigurationProperties(prefix = "frontend.messages.local.storage")
public class LocalMessageStorageProperties {

    private long bufferedSizeBytes = 256 * 1024 * 1024L;

    private boolean v2MigrationEnabled = true;

    private boolean enabled = false;

    private String directory = Files.createTempDir().getAbsolutePath();

    private String temporaryDirectory = Files.createTempDir().getAbsolutePath();

    private int averageMessageSize = 600;

    private int maxAgeHours = 72;

    private int maxResendRetries = 5;

    private int loadingPauseBetweenResend = 30;

    private int loadingWaitForBrokerTopicInfo = 5;

    private boolean sizeReportingEnabled = true;

    public long getBufferedSizeBytes() {
        return bufferedSizeBytes;
    }

    public void setBufferedSizeBytes(long bufferedSizeBytes) {
        this.bufferedSizeBytes = bufferedSizeBytes;
    }

    public boolean isV2MigrationEnabled() {
        return v2MigrationEnabled;
    }

    public void setV2MigrationEnabled(boolean v2MigrationEnabled) {
        this.v2MigrationEnabled = v2MigrationEnabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getTemporaryDirectory() {
        return temporaryDirectory;
    }

    public void setTemporaryDirectory(String temporaryDirectory) {
        this.temporaryDirectory = temporaryDirectory;
    }

    public int getAverageMessageSize() {
        return averageMessageSize;
    }

    public void setAverageMessageSize(int averageMessageSize) {
        this.averageMessageSize = averageMessageSize;
    }

    public int getMaxAgeHours() {
        return maxAgeHours;
    }

    public void setMaxAgeHours(int maxAgeHours) {
        this.maxAgeHours = maxAgeHours;
    }

    public int getMaxResendRetries() {
        return maxResendRetries;
    }

    public void setMaxResendRetries(int maxResendRetries) {
        this.maxResendRetries = maxResendRetries;
    }

    public int getLoadingPauseBetweenResend() {
        return loadingPauseBetweenResend;
    }

    public void setLoadingPauseBetweenResend(int loadingPauseBetweenResend) {
        this.loadingPauseBetweenResend = loadingPauseBetweenResend;
    }

    public int getLoadingWaitForBrokerTopicInfo() {
        return loadingWaitForBrokerTopicInfo;
    }

    public void setLoadingWaitForBrokerTopicInfo(int loadingWaitForBrokerTopicInfo) {
        this.loadingWaitForBrokerTopicInfo = loadingWaitForBrokerTopicInfo;
    }

    public boolean isSizeReportingEnabled() {
        return sizeReportingEnabled;
    }

    public void setSizeReportingEnabled(boolean sizeReportingEnabled) {
        this.sizeReportingEnabled = sizeReportingEnabled;
    }

    protected PersistentBufferExtensionParameters toPersistentBufferExtensionParameters() {
        return new PersistentBufferExtensionParameters(
                this.bufferedSizeBytes,
                this.v2MigrationEnabled,
                this.enabled,
                this.directory,
                this.temporaryDirectory,
                this.averageMessageSize,
                this.sizeReportingEnabled
        );
    }

    public BackupMessagesLoaderParameters toBackupMessagesLoaderParameters() {
        return new BackupMessagesLoaderParameters(
                this.maxAgeHours,
                this.maxResendRetries,
                this.loadingPauseBetweenResend,
                this.loadingWaitForBrokerTopicInfo
        );
    }
}
