package pl.allegro.tech.hermes.management.domain.subscription.commands;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class CreateSubscriptionRepositoryCommand extends RepositoryCommand<SubscriptionRepository> {

    private final Subscription subscription;

    public CreateSubscriptionRepositoryCommand(Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void backup(DatacenterBoundRepositoryHolder<SubscriptionRepository> holder) {}

    @Override
    public void execute(DatacenterBoundRepositoryHolder<SubscriptionRepository> holder) {
        holder.getRepository().createSubscription(subscription);
    }

    @Override
    public void rollback(DatacenterBoundRepositoryHolder<SubscriptionRepository> holder) {
        holder.getRepository().removeSubscription(subscription.getTopicName(), subscription.getName());
    }

    @Override
    public Class<SubscriptionRepository> getRepositoryType() {
        return SubscriptionRepository.class;
    }

    @Override
    public String toString() {
        return "CreateSubscription(" + subscription.getQualifiedName() + ")";
    }
}
