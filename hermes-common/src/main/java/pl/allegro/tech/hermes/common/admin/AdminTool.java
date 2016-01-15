package pl.allegro.tech.hermes.common.admin;

import pl.allegro.tech.hermes.api.SubscriptionName;

public interface AdminTool {

    void start() throws AdminToolStartupException;

    void retransmit(SubscriptionName subscriptionName);

    void restartConsumer(SubscriptionName subscriptionName);

    enum Operations {

        RETRANSMIT,
        RESTART_CONSUMER
    }
}
