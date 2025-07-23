package pl.allegro.tech.hermes.management.domain.subscription.consumergroup;

import static java.lang.String.format;

public class ConsumerGroupAlreadyScheduledToDeleteException extends RuntimeException {

  public ConsumerGroupAlreadyScheduledToDeleteException(
      ConsumerGroupToDelete consumerGroupToDelete, Throwable e) {
    super(
        format(
            "Consumer group already scheduled to delete, for subscription %s ",
            consumerGroupToDelete.subscriptionName().getQualifiedName()),
        e);
  }
}
