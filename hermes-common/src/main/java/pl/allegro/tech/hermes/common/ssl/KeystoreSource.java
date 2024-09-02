package pl.allegro.tech.hermes.common.ssl;

public enum KeystoreSource {
  JRE("jre"),
  PROVIDED("provided");

  KeystoreSource(String value) {
    this.value = value;
  }

  private final String value;

  public final String getValue() {
    return value;
  }
}
