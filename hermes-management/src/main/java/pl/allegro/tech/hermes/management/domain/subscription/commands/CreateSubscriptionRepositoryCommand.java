package pl.allegro.tech.hermes.management.domain.subscription.commands;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class CreateSubscriptionRepositoryCommand extends RepositoryCommand<SubscriptionRepository> {

    private final Subscription subscription;

    public CreateSubscriptionRepositoryCommand(Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void backup(SubscriptionRepository repository) {}

    @Override
    public void execute(SubscriptionRepository repository) {
        repository.createSubscription(subscription);
    }

    @Override
    public void rollback(SubscriptionRepository repository) {
        repository.removeSubscription(subscription.getTopicName(), subscription.getName());
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
