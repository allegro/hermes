package pl.allegro.tech.hermes.common.exception;

import pl.allegro.tech.hermes.api.ErrorCode;

public class SubscriptionEndpointAddressChangeException extends HermesException {
  public SubscriptionEndpointAddressChangeException(Throwable cause) {
    super("Error during change topic endpoint address.", cause);
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.SUBSCRIPTION_ENDPOINT_ADDRESS_CHANGE_EXCEPTION;
  }
}
