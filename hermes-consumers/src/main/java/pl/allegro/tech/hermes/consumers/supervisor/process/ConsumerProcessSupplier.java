package pl.allegro.tech.hermes.consumers.supervisor.process;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;

public interface ConsumerProcessSupplier {
  ConsumerProcess createProcess(
      Subscription subscription,
      Signal startSignal,
      java.util.function.Consumer<SubscriptionName> onConsumerStopped);
}
