package pl.allegro.tech.hermes.management.domain.subscription.consumergroup.command;

import java.time.Instant;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;
import pl.allegro.tech.hermes.management.domain.subscription.consumergroup.ConsumerGroupAlreadyScheduledToDeleteException;
import pl.allegro.tech.hermes.management.domain.subscription.consumergroup.ConsumerGroupToDelete;
import pl.allegro.tech.hermes.management.domain.subscription.consumergroup.ConsumerGroupToDeleteRepository;

public class ScheduleConsumerGroupToDeleteCommand
    extends RepositoryCommand<ConsumerGroupToDeleteRepository> {
  private final SubscriptionName subscriptionName;
  private final Instant requestedAt;

  public ScheduleConsumerGroupToDeleteCommand(
      SubscriptionName subscriptionName, Instant requestedAt) {
    this.subscriptionName = subscriptionName;
    this.requestedAt = requestedAt;
  }

  @Override
  public void backup(DatacenterBoundRepositoryHolder<ConsumerGroupToDeleteRepository> holder) {}

  @Override
  public void execute(DatacenterBoundRepositoryHolder<ConsumerGroupToDeleteRepository> holder) {
    ConsumerGroupToDelete consumerGroupToDelete =
        new ConsumerGroupToDelete(subscriptionName, holder.getDatacenterName(), requestedAt);
    try {
      holder.getRepository().scheduleConsumerGroupToDeleteTask(consumerGroupToDelete);
    } catch (ConsumerGroupAlreadyScheduledToDeleteException ignored) {
    }
  }

  @Override
  public void rollback(
      DatacenterBoundRepositoryHolder<ConsumerGroupToDeleteRepository> holder,
      Exception exception) {
    ConsumerGroupToDelete consumerGroupToDelete =
        new ConsumerGroupToDelete(subscriptionName, holder.getDatacenterName(), requestedAt);
    holder.getRepository().deleteConsumerGroupToDeleteTask(consumerGroupToDelete);
  }

  @Override
  public Class<ConsumerGroupToDeleteRepository> getRepositoryType() {
    return ConsumerGroupToDeleteRepository.class;
  }

  @Override
  public String toString() {
    return "ScheduleConsumerGroupToDelete(" + subscriptionName.getQualifiedName() + ")";
  }
}
