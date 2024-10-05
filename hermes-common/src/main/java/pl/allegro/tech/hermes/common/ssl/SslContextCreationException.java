package pl.allegro.tech.hermes.common.ssl;

class SslContextCreationException extends RuntimeException {
  SslContextCreationException(Exception e) {
    super(e);
  }
}
