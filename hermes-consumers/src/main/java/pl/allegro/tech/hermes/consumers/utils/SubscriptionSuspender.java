package pl.allegro.tech.hermes.consumers.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;

import javax.inject.Inject;

public class SubscriptionSuspender {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionSuspender.class);
    private SubscriptionRepository subscriptionRepository;

    @Inject
    public SubscriptionSuspender(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public void suspend(Subscription subscription) {
        Subscription actualSubscription = subscriptionRepository.getSubscriptionDetails(
                subscription.getTopicName(), subscription.getName()
        );

        if (Subscription.State.SUSPENDED == actualSubscription.getState()) {
            LOGGER.warn("Can't suspend subscription {}. Subscription already suspended", subscription.getId());
            return;
        }

        actualSubscription.setState(Subscription.State.SUSPENDED);

        LOGGER.warn("Suspending subscription {}", subscription.getId());
        subscriptionRepository.updateSubscription(actualSubscription);
    }
}
