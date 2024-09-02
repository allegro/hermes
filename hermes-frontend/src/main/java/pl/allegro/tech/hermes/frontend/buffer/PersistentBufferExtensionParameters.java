package pl.allegro.tech.hermes.frontend.buffer;

public interface PersistentBufferExtensionParameters {

  long getBufferedSizeBytes();

  boolean isV2MigrationEnabled();

  boolean isEnabled();

  String getDirectory();

  String getTemporaryDirectory();

  int getAverageMessageSize();

  boolean isSizeReportingEnabled();
}
