package pl.allegro.tech.hermes.consumers.consumer.receiver;

import com.codahale.metrics.Timer;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.SubscriptionMetrics;
import pl.allegro.tech.hermes.consumers.consumer.idletime.IdleTimeCalculator;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;

import java.util.Optional;
import java.util.Set;

public class ThrottlingMessageReceiver implements MessageReceiver {

    private final MessageReceiver receiver;
    private final IdleTimeCalculator idleTimeCalculator;
    private final SubscriptionMetrics metrics;

    public ThrottlingMessageReceiver(MessageReceiver receiver,
                                     IdleTimeCalculator idleTimeCalculator,
                                     SubscriptionMetrics metrics) {
        this.receiver = receiver;
        this.idleTimeCalculator = idleTimeCalculator;
        this.metrics = metrics;
    }

    @Override
    public Optional<Message> next() {
        Optional<Message> next = receiver.next();
        if (next.isPresent()) {
            idleTimeCalculator.reset();
        } else {
            awaitUntilNextPoll();
        }
        return next;
    }

    private void awaitUntilNextPoll() {
        try (Timer.Context ctx = metrics.consumerIdleTimer().time()) {
            Thread.sleep(idleTimeCalculator.increaseIdleTime());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void commit(Set<SubscriptionPartitionOffset> offsets) {
        receiver.commit(offsets);
    }

    @Override
    public boolean moveOffset(PartitionOffset offset) {
        return receiver.moveOffset(offset);
    }

    @Override
    public void stop() {
        receiver.stop();
    }

    @Override
    public void update(Subscription newSubscription) {
        this.receiver.update(newSubscription);
    }
}
