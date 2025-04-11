package pl.allegro.tech.hermes.api;

public enum MetricUnavailable implements MetricValue {
  INSTANCE;

  @Override
  public boolean isAvailable() {
    return false;
  }

  @Override
  public String toString() {
    return "MetricUnavailable";
  }
}
