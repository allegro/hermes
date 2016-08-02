package pl.allegro.tech.hermes.consumers.consumer;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;

public interface Consumer {

    void consume(Runnable signalsInterrupt);

    void initialize();

    void tearDown();

    void updateSubscription(Subscription subscription);

    void updateTopic(Topic topic);
}
