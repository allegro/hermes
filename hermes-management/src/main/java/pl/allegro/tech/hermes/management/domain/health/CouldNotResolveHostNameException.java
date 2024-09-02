package pl.allegro.tech.hermes.management.domain.health;

class CouldNotResolveHostNameException extends RuntimeException {
  CouldNotResolveHostNameException(Throwable cause) {
    super(cause);
  }
}
