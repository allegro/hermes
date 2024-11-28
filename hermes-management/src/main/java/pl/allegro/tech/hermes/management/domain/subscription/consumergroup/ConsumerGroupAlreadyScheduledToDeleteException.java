package pl.allegro.tech.hermes.management.domain.subscription.consumergroup;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

import static java.lang.String.format;
import static pl.allegro.tech.hermes.api.ErrorCode.OTHER;

public class ConsumerGroupAlreadyScheduledToDeleteException extends ManagementException {

  public ConsumerGroupAlreadyScheduledToDeleteException(
      ConsumerGroupToDelete consumerGroupToDelete, Throwable e) {
    super(
        format(
            "Consumer group already scheduled to delete, for subscription %s ",
            consumerGroupToDelete.subscriptionName().getQualifiedName()),
        e);
  }

  @Override
  public ErrorCode getCode() {
    return OTHER;
  }
}
