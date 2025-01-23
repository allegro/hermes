package pl.allegro.tech.hermes.management.infrastructure.kafka.service.retransmit;

import static pl.allegro.tech.hermes.api.ErrorCode.OFFSET_NOT_FOUND_EXCEPTION;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

class OffsetNotFoundException extends ManagementException {

  public OffsetNotFoundException(String message) {
    super(message);
  }

  @Override
  public ErrorCode getCode() {
    return OFFSET_NOT_FOUND_EXCEPTION;
  }
}
