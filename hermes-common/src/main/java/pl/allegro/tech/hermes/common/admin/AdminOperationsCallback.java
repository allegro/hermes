package pl.allegro.tech.hermes.common.admin;

import pl.allegro.tech.hermes.api.SubscriptionName;

public interface AdminOperationsCallback {

    void onRetransmissionStarts(SubscriptionName subscription) throws Exception;

    void onSubscriptionEndpointAddressChanged(SubscriptionName subscription) throws Exception;
}
