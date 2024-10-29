package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder;

public class NoOpConsumerNodeLoadRegistry implements ConsumerNodeLoadRegistry {

  private static final NoOpSubscriptionLoadRecorder SUBSCRIPTION_LOAD_RECORDER =
      new NoOpSubscriptionLoadRecorder();

  @Override
  public void start() {}

  @Override
  public void stop() {}

  @Override
  public ConsumerNodeLoad get(String consumerId) {
    return ConsumerNodeLoad.UNDEFINED;
  }

  @Override
  public SubscriptionLoadRecorder register(SubscriptionName subscriptionName) {
    return SUBSCRIPTION_LOAD_RECORDER;
  }

  private static class NoOpSubscriptionLoadRecorder implements SubscriptionLoadRecorder {

    @Override
    public void initialize() {}

    @Override
    public void recordSingleOperation() {}

    @Override
    public void shutdown() {}
  }
}
