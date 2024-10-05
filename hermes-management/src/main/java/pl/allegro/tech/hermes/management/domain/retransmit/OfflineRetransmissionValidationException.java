package pl.allegro.tech.hermes.management.domain.retransmit;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class OfflineRetransmissionValidationException extends HermesException {

  public OfflineRetransmissionValidationException(String msg) {
    super(msg);
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.VALIDATION_ERROR;
  }
}
