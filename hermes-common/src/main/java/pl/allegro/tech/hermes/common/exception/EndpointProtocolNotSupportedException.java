package pl.allegro.tech.hermes.common.exception;

import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.ErrorCode;

public class EndpointProtocolNotSupportedException extends HermesException {

    public EndpointProtocolNotSupportedException(EndpointAddress endpointUri) {
        super(String.format("Protocol %s not supported in endpoint %s", endpointUri.getProtocol(), endpointUri.toString()));
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.VALIDATION_ERROR;
    }
}
