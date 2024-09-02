package pl.allegro.tech.hermes.common.exception;

import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.ErrorCode;

public class EndpointProtocolNotSupportedException extends HermesException {

  public EndpointProtocolNotSupportedException(EndpointAddress endpoint) {
    super(
        String.format(
            "Protocol %s not supported in endpoint %s",
            endpoint.getProtocol(), endpoint.toString()));
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.VALIDATION_ERROR;
  }
}
