package pl.allegro.tech.hermes.consumers.config;

public class RegistryBinaryEncoderProperties {

  private int maxRateBufferSizeBytes = 100_000;

  private int historyBufferSizeBytes = 100_000;

  public int getMaxRateBufferSizeBytes() {
    return maxRateBufferSizeBytes;
  }

  public void setMaxRateBufferSizeBytes(int maxRateBufferSizeBytes) {
    this.maxRateBufferSizeBytes = maxRateBufferSizeBytes;
  }

  public int getHistoryBufferSizeBytes() {
    return historyBufferSizeBytes;
  }

  public void setHistoryBufferSizeBytes(int historyBufferSizeBytes) {
    this.historyBufferSizeBytes = historyBufferSizeBytes;
  }
}
