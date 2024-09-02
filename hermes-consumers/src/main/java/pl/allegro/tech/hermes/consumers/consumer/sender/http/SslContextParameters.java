package pl.allegro.tech.hermes.consumers.consumer.sender.http;

public interface SslContextParameters {

  boolean isEnabled();

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
