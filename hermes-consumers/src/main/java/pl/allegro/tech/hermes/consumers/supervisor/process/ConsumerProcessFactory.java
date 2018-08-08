package pl.allegro.tech.hermes.consumers.supervisor.process;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumerFactory;

import java.time.Clock;

public class ConsumerProcessFactory implements ConsumerProcessSupplier {

    private final Retransmitter retransmitter;
    private final long unhealthyAfter;
    private final Clock clock;
    private final ConsumerFactory consumerFactory;

    public ConsumerProcessFactory(Retransmitter retransmitter,
                                  ConsumerFactory consumerFactory,
                                  ConfigFactory configs,
                                  Clock clock) {
        this.retransmitter = retransmitter;
        this.consumerFactory = consumerFactory;
        this.unhealthyAfter = configs.getIntProperty(Configs.CONSUMER_BACKGROUND_SUPERVISOR_UNHEALTHY_AFTER);
        this.clock = clock;
    }

    @Override
    public ConsumerProcess createProcess(Subscription subscription,
                                  Signal startSignal,
                                  java.util.function.Consumer<SubscriptionName> onConsumerStopped) {

        return new ConsumerProcess(startSignal,
                                   consumerFactory.createConsumer(subscription),
                                   retransmitter,
                                   clock,
                                   unhealthyAfter,
                                   onConsumerStopped);
    }
}