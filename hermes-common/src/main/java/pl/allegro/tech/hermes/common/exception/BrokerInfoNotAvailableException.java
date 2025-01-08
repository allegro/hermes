package pl.allegro.tech.hermes.common.exception;

public class BrokerInfoNotAvailableException extends InternalProcessingException {

  public BrokerInfoNotAvailableException(Integer brokerId, Throwable cause) {
    super("Could not find or read info about broker with id " + brokerId, cause);
  }
}
