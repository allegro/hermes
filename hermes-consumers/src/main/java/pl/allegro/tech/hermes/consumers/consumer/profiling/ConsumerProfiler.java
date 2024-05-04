package pl.allegro.tech.hermes.consumers.consumer.profiling;

public interface ConsumerProfiler {

    void startMeasurements(Measurement measurement);

    void measure(Measurement measurement);

    void startPartialMeasurement(Measurement measurement);

    void stopPartialMeasurement(Measurement measurement);

    void saveRetryDelay(long retryDelay);

    void flushMeasurements(ConsumerRun consumerRun);
}
