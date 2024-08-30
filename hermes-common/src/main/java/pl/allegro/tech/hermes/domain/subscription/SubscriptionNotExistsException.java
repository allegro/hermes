package pl.allegro.tech.hermes.domain.subscription;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class SubscriptionNotExistsException extends HermesException {

  public SubscriptionNotExistsException(TopicName topic, String subscription) {
    super(
        String.format(
            "Subscription %s for topic %s does not exist.", subscription, topic.qualifiedName()));
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.SUBSCRIPTION_NOT_EXISTS;
  }
}
