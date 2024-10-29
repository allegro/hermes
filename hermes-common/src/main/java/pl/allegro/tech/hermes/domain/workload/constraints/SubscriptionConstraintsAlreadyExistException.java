package pl.allegro.tech.hermes.domain.workload.constraints;

import static pl.allegro.tech.hermes.api.ErrorCode.SUBSCRIPTION_CONSTRAINTS_ALREADY_EXIST;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class SubscriptionConstraintsAlreadyExistException extends HermesException {

  public SubscriptionConstraintsAlreadyExistException(
      SubscriptionName subscriptionName, Throwable cause) {
    super(
        String.format(
            "Constraints for subscription %s already exist.", subscriptionName.getQualifiedName()),
        cause);
  }

  @Override
  public ErrorCode getCode() {
    return SUBSCRIPTION_CONSTRAINTS_ALREADY_EXIST;
  }
}
