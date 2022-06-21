package pl.allegro.tech.hermes.frontend.buffer;

public class PersistentBufferExtensionParameters {

    private final long bufferedSizeBytes;

    private final boolean v2MigrationEnabled;

    private final boolean enabled;

    private final String directory;

    private final String temporaryDirectory;

    private final int averageMessageSize;

    private final boolean sizeReportingEnabled;

    public long getBufferedSizeBytes() {
        return bufferedSizeBytes;
    }

    public boolean isV2MigrationEnabled() {
        return v2MigrationEnabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getDirectory() {
        return directory;
    }

    public String getTemporaryDirectory() {
        return temporaryDirectory;
    }

    public int getAverageMessageSize() {
        return averageMessageSize;
    }

    public boolean isSizeReportingEnabled() {
        return sizeReportingEnabled;
    }

    public PersistentBufferExtensionParameters(long bufferedSizeBytes,
                                               boolean v2MigrationEnabled,
                                               boolean enabled,
                                               String directory,
                                               String temporaryDirectory,
                                               int averageMessageSize,
                                               boolean sizeReportingEnabled) {
        this.bufferedSizeBytes = bufferedSizeBytes;
        this.v2MigrationEnabled = v2MigrationEnabled;
        this.enabled = enabled;
        this.directory = directory;
        this.temporaryDirectory = temporaryDirectory;
        this.averageMessageSize = averageMessageSize;
        this.sizeReportingEnabled = sizeReportingEnabled;
    }
}
