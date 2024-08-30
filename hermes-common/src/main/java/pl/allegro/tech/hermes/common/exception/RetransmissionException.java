package pl.allegro.tech.hermes.common.exception;

import pl.allegro.tech.hermes.api.ErrorCode;

public class RetransmissionException extends HermesException {

  public RetransmissionException(String message) {
    super(message);
  }

  public RetransmissionException(Throwable cause) {
    super("Error during retransmitting messages.", cause);
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.RETRANSMISSION_EXCEPTION;
  }
}
