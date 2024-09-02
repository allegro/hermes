package pl.allegro.tech.hermes.mock;

public class HermesMockException extends RuntimeException {
  public HermesMockException(String message) {
    super(message);
  }

  public HermesMockException(String message, Throwable cause) {
    super(message, cause);
  }
}
