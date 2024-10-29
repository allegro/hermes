package pl.allegro.tech.hermes.consumers.consumer.profiling;

public class NoOpConsumerProfiler implements ConsumerProfiler {

  @Override
  public void startMeasurements(String measurement) {}

  @Override
  public void measure(String measurement) {}

  @Override
  public void startPartialMeasurement(String measurement) {}

  @Override
  public void stopPartialMeasurement() {}

  @Override
  public void saveRetryDelay(long retryDelay) {}

  @Override
  public void flushMeasurements(ConsumerRun consumerRun) {}
}
