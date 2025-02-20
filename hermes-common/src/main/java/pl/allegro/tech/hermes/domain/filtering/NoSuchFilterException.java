package pl.allegro.tech.hermes.domain.filtering;

public class NoSuchFilterException extends FilteringException {
  public NoSuchFilterException(Throwable throwable) {
    super(throwable);
  }

  public NoSuchFilterException(String filterType) {
    super("Filter of type '" + filterType + "' not found.");
  }

  public NoSuchFilterException(String message, Throwable throwable) {
    super(message, throwable);
  }
}
