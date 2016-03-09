package pl.allegro.tech.hermes.consumers.consumer.batch;

import pl.allegro.tech.hermes.api.Subscription;

public interface MessageBatchFactory {

    MessageBatch createBatch(Subscription subscription);

    void destroyBatch(MessageBatch batch);
}
