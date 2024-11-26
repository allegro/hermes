package pl.allegro.tech.hermes.management.infrastructure.kafka;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.management.domain.ManagementException;

import static java.lang.String.format;
import static pl.allegro.tech.hermes.api.ErrorCode.CONSUMER_GROUP_DELETION_ERROR;

public class ConsumerGroupDeletionException extends ManagementException {

  public ConsumerGroupDeletionException(SubscriptionName subscriptionName, Throwable e) {
    super(format("Failed to delete consumer group, for subscription %s ", subscriptionName), e);
  }

  @Override
  public ErrorCode getCode() {
    return CONSUMER_GROUP_DELETION_ERROR;
  }
}
