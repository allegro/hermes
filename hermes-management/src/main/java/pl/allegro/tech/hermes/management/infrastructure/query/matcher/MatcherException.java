package pl.allegro.tech.hermes.management.infrastructure.query.matcher;

public class MatcherException extends RuntimeException {

  public MatcherException(String message) {
    super(message);
  }

  public MatcherException(String message, Throwable cause) {
    super(message, cause);
  }
}
