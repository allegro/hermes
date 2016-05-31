package pl.allegro.tech.hermes.consumers.consumer;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

import java.util.List;

public interface Consumer {

    void consume(Runnable processSignals);

    void initialize();

    void tearDown();

    Subscription getSubscription();

    void updateSubscription(Subscription subscription);

    List<PartitionOffset> getOffsetsToCommit();
}
