package pl.allegro.tech.hermes.domain.filtering;

import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

public class FilteringException extends InternalProcessingException {
  public FilteringException(Throwable throwable) {
    super(throwable);
  }

  public FilteringException(String message) {
    super(message);
  }

  public FilteringException(String message, Throwable throwable) {
    super(message, throwable);
  }

  public static void check(boolean condition, String message) {
    if (!condition) {
      throw new FilteringException(message);
    }
  }
}
