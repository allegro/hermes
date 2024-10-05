package pl.allegro.tech.hermes.domain.subscription;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class SubscriptionAlreadyExistsException extends HermesException {

  public SubscriptionAlreadyExistsException(Subscription subscription, Throwable cause) {
    super(message(subscription), cause);
  }

  public SubscriptionAlreadyExistsException(Subscription subscription) {
    super(message(subscription));
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.SUBSCRIPTION_ALREADY_EXISTS;
  }

  private static String message(Subscription subscription) {
    return String.format(
        "Subscription %s for topic %s already exists.",
        subscription.getName(), subscription.getQualifiedTopicName());
  }
}
