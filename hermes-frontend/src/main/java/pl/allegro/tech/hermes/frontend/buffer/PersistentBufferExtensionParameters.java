package pl.allegro.tech.hermes.frontend.buffer;

/**
 * @deprecated This feature is deprecated and will be removed in a future version.
 */
@Deprecated
public interface PersistentBufferExtensionParameters {

  long getBufferedSizeBytes();

  boolean isV2MigrationEnabled();

  boolean isEnabled();

  String getDirectory();

  String getTemporaryDirectory();

  int getAverageMessageSize();

  boolean isSizeReportingEnabled();
}
