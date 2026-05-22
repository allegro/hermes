package pl.allegro.tech.hermes.management.domain.subscription.commands;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class RemoveSubscriptionRepositoryCommand extends RepositoryCommand<SubscriptionRepository> {

  private final SubscriptionName subscriptionName;

  private Subscription backup;

  public RemoveSubscriptionRepositoryCommand(SubscriptionName subscriptionName) {
    this.subscriptionName = subscriptionName;
  }

  @Override
  public void backup(DatacenterBoundRepositoryHolder<SubscriptionRepository> holder) {
    backup =
        holder
            .getRepository()
            .getSubscriptionDetails(subscriptionName.getTopicName(), subscriptionName.getName());
  }

  @Override
  public void execute(DatacenterBoundRepositoryHolder<SubscriptionRepository> holder) {
    holder
        .getRepository()
        .removeSubscription(subscriptionName.getTopicName(), subscriptionName.getName());
  }

  @Override
  public void rollback(
      DatacenterBoundRepositoryHolder<SubscriptionRepository> holder, Exception exception) {
    holder.getRepository().createSubscription(backup);
  }

  @Override
  public Class<SubscriptionRepository> getRepositoryType() {
    return SubscriptionRepository.class;
  }

  @Override
  public String toString() {
    return "RemoveSubscription(" + subscriptionName.getQualifiedName() + ")";
  }
}
