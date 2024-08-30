package pl.allegro.tech.hermes.common.ssl;

public class TruststoreConfigurationException extends RuntimeException {
  public TruststoreConfigurationException(String truststoreSource) {
    super(String.format("Unknown trust store source: %s", truststoreSource));
  }
}
