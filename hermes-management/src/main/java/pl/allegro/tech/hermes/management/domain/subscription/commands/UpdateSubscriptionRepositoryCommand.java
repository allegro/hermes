package pl.allegro.tech.hermes.management.domain.subscription.commands;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class UpdateSubscriptionRepositoryCommand extends RepositoryCommand<SubscriptionRepository> {
    private final Subscription subscription;

    private Subscription backup;

    public UpdateSubscriptionRepositoryCommand(Subscription subscription) {this.subscription = subscription;}

    @Override
    public void backup(SubscriptionRepository repository) {
        backup = repository.getSubscriptionDetails(subscription.getTopicName(), subscription.getName());
    }

    @Override
    public void execute(SubscriptionRepository repository) {
        repository.updateSubscription(subscription);
    }

    @Override
    public void rollback(SubscriptionRepository repository) {
        repository.updateSubscription(backup);
    }

    @Override
    public Class<SubscriptionRepository> getRepositoryType() {
        return SubscriptionRepository.class;
    }

    @Override
    public String toString() {
        return "UpdateSubscription(" + subscription.getQualifiedName() + ")";
    }
}
