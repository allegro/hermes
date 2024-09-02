package pl.allegro.tech.hermes.common.exception;

public class InternalProcessingException extends RuntimeException {
  public InternalProcessingException(Throwable throwable) {
    super(throwable);
  }

  public InternalProcessingException(String message) {
    super(message);
  }

  public InternalProcessingException(String message, Throwable throwable) {
    super(message, throwable);
  }
}
