package pl.allegro.tech.hermes.management.infrastructure.kafka.service.retransmit;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

import static pl.allegro.tech.hermes.api.ErrorCode.OFFSET_NOT_FOUND_EXCEPTION;

class OffsetNotFoundException extends ManagementException {

    OffsetNotFoundException(int errorCode) {
        super(String.format("Offset not found. Error code: %s", errorCode));
    }

    @Override
    public ErrorCode getCode() {
        return OFFSET_NOT_FOUND_EXCEPTION;
    }
}
