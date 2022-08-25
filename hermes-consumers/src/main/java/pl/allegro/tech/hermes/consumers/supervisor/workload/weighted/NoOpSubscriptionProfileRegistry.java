package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import static pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.SubscriptionProfiles.EMPTY;

public class NoOpSubscriptionProfileRegistry implements SubscriptionProfileRegistry {

    @Override
    public SubscriptionProfiles fetch() {
        return EMPTY;
    }

    @Override
    public void persist(SubscriptionProfiles profiles) {

    }
}
