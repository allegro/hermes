package pl.allegro.tech.hermes.management.domain.consistency;

public class ConsistencyCheckingException extends RuntimeException {

  ConsistencyCheckingException(String message, Throwable th) {
    super(message, th);
  }
}
