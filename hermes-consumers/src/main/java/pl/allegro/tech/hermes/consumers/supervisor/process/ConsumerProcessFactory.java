package pl.allegro.tech.hermes.consumers.supervisor.process;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetCommitter;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetCommitterFactory;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumerFactory;

import java.time.Clock;
import java.time.Duration;

public class ConsumerProcessFactory implements ConsumerProcessSupplier {

    private final Retransmitter retransmitter;
    private final Duration unhealthyAfter;
    private final Clock clock;
    private final ConsumerFactory consumerFactory;
    private final OffsetCommitterFactory offsetCommitterFactory;
    private final MetricsFacade metrics;
    private final int offsetQueuesSize;

    public ConsumerProcessFactory(Retransmitter retransmitter,
                                  ConsumerFactory consumerFactory,
                                  OffsetCommitterFactory offsetCommitterFactory,
                                  int offsetQueuesSize,
                                  MetricsFacade metrics,
                                  Duration unhealthyAfter,
                                  Clock clock) {
        this.retransmitter = retransmitter;
        this.consumerFactory = consumerFactory;
        this.offsetCommitterFactory = offsetCommitterFactory;
        this.metrics = metrics;
        this.offsetQueuesSize = offsetQueuesSize;
        this.unhealthyAfter = unhealthyAfter;
        this.clock = clock;
    }

    @Override
    public ConsumerProcess createProcess(Subscription subscription,
                                         Signal startSignal,
                                         java.util.function.Consumer<SubscriptionName> onConsumerStopped,
                                         MessageCommitter messageCommitter) {
        OffsetQueue offsetQueue = new OffsetQueue(metrics, offsetQueuesSize);
        Consumer consumer = consumerFactory.createConsumer(subscription, offsetQueue);
        OffsetCommitter offsetCommitter = offsetCommitterFactory.createOffsetCommitter(
                subscription.getQualifiedName(),
                messageCommitter,
                offsetQueue
        );
        return new ConsumerProcess(startSignal,
                consumer,
                offsetCommitter,
                retransmitter,
                clock,
                unhealthyAfter,
                onConsumerStopped);
    }
}