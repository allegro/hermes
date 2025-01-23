package pl.allegro.tech.hermes.client;

public class HermesClientShutdownException extends RuntimeException {

  public HermesClientShutdownException() {
    this("Hermes client is already shutdown");
  }

  public HermesClientShutdownException(String message) {
    super(message);
  }
}
