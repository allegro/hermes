package pl.allegro.tech.hermes.frontend.server;

public interface SslParameters {

  boolean isEnabled();

  int getPort();

  String getClientAuthMode();

  String getProtocol();

  String getKeystoreSource();

  String getKeystoreLocation();

  String getKeystorePassword();

  String getKeystoreFormat();

  String getTruststoreSource();

  String getTruststoreLocation();

  String getTruststorePassword();

  String getTruststoreFormat();
}
