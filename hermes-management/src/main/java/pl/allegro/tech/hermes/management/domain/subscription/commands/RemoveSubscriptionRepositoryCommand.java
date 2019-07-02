package pl.allegro.tech.hermes.management.domain.subscription.commands;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
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
    public void backup(SubscriptionRepository repository) {
        backup = repository.getSubscriptionDetails(topicName, subscriptionName);
    }

    @Override
    public void execute(SubscriptionRepository repository) {
        repository.removeSubscription(topicName, subscriptionName);
    }

    @Override
    public void rollback(SubscriptionRepository repository) {
        repository.createSubscription(backup);
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
