package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import pl.allegro.tech.hermes.api.SubscriptionName;

public interface SubscriptionProfileProvider {

    SubscriptionProfile get(SubscriptionName subscriptionName);
}
