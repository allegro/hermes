package pl.allegro.tech.hermes.domain.workload.constraints;

import static pl.allegro.tech.hermes.api.ErrorCode.SUBSCRIPTION_CONSTRAINTS_DO_NOT_EXIST;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class SubscriptionConstraintsDoNotExistException extends HermesException {

  public SubscriptionConstraintsDoNotExistException(
      SubscriptionName subscriptionName, Throwable cause) {
    super(
        String.format(
            "Constraints for subscription %s do not exist.", subscriptionName.getQualifiedName()),
        cause);
  }

  @Override
  public ErrorCode getCode() {
    return SUBSCRIPTION_CONSTRAINTS_DO_NOT_EXIST;
  }
}
