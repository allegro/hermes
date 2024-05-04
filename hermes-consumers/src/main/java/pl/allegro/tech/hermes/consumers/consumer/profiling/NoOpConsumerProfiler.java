package pl.allegro.tech.hermes.consumers.consumer.profiling;

public class NoOpConsumerProfiler implements ConsumerProfiler {

    @Override
    public void startMeasurements(Measurement measurement) {

    }

    @Override
    public void measure(Measurement measurement) {

    }

    @Override
    public void startPartialMeasurement(Measurement measurement) {

    }

    @Override
    public void stopPartialMeasurement(Measurement measurement) {

    }

    @Override
    public void saveRetryDelay(long retryDelay) {

    }

    @Override
    public void flushMeasurements(ConsumerRun consumerRun) {

    }
}
