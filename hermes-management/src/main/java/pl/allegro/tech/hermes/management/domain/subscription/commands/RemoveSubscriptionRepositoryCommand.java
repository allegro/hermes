package pl.allegro.tech.hermes.management.domain.subscription.commands;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class RemoveSubscriptionRepositoryCommand extends RepositoryCommand<SubscriptionRepository> {

    private final TopicName topicName;
    private final String subscriptionName;

    private Subscription backup;

    public RemoveSubscriptionRepositoryCommand(TopicName topicName, String subscriptionName) {
        this.topicName = topicName;
        this.subscriptionName = subscriptionName;
    }

    @Override
    public void backup(DatacenterBoundRepositoryHolder<SubscriptionRepository> holder) {
        backup = holder.getRepository().getSubscriptionDetails(topicName, subscriptionName);
    }

    @Override
    public void execute(DatacenterBoundRepositoryHolder<SubscriptionRepository> holder) {
        holder.getRepository().removeSubscription(topicName, subscriptionName);
    }

    @Override
    public void rollback(DatacenterBoundRepositoryHolder<SubscriptionRepository> holder) {
        holder.getRepository().createSubscription(backup);
    }

    @Override
    public Class<SubscriptionRepository> getRepositoryType() {
        return SubscriptionRepository.class;
    }

    @Override
    public String toString() {
        return "RemoveSubscription(" + new SubscriptionName(subscriptionName, topicName).getQualifiedName() + ")";
    }
}
