package pl.allegro.tech.hermes.management.infrastructure.kafka;

import static java.lang.String.format;
import static pl.allegro.tech.hermes.api.ErrorCode.CONSUMER_GROUP_DELETION_ERROR;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class ConsumerGroupDeletionException extends ManagementException {

  public ConsumerGroupDeletionException(SubscriptionName subscriptionName, Throwable e) {
    super(format("Failed to delete consumer group, for subscription %s ", subscriptionName), e);
  }

  @Override
  public ErrorCode getCode() {
    return CONSUMER_GROUP_DELETION_ERROR;
  }
}
