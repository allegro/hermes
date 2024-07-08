package pl.allegro.tech.hermes.consumers.consumer.load;

public interface SubscriptionLoadRecorder {

    void initialize();

    default void recordSingleOperation() {
        recordSingleOperation(1);
    }

    void recordSingleOperation(long weight);

    void shutdown();
}
