package pl.allegro.tech.hermes.consumers.supervisor.process;

import java.time.Clock;
import java.time.Duration;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumerFactory;

public class ConsumerProcessFactory implements ConsumerProcessSupplier {

  private final Retransmitter retransmitter;
  private final Duration unhealthyAfter;
  private final Clock clock;
  private final ConsumerFactory consumerFactory;

  public ConsumerProcessFactory(
      Retransmitter retransmitter,
      ConsumerFactory consumerFactory,
      Duration unhealthyAfter,
      Clock clock) {
    this.retransmitter = retransmitter;
    this.consumerFactory = consumerFactory;
    this.unhealthyAfter = unhealthyAfter;
    this.clock = clock;
  }

  @Override
  public ConsumerProcess createProcess(
      Subscription subscription,
      Signal startSignal,
      java.util.function.Consumer<SubscriptionName> onConsumerStopped) {

    return new ConsumerProcess(
        startSignal,
        consumerFactory.createConsumer(subscription),
        retransmitter,
        clock,
        unhealthyAfter,
        onConsumerStopped);
  }
}
