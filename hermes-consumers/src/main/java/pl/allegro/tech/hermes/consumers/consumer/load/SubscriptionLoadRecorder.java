package pl.allegro.tech.hermes.consumers.consumer.load;

public interface SubscriptionLoadRecorder {

  void initialize();

  void recordSingleOperation();

  void shutdown();
}
