package pl.allegro.tech.hermes.common.ssl;

public class KeystoreConfigurationException extends RuntimeException {
  public KeystoreConfigurationException(String keystoreSource) {
    super(String.format("Unknown key store source: %s", keystoreSource));
  }
}
