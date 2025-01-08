package pl.allegro.tech.hermes.consumers.consumer.profiling;

public interface ConsumerProfiler {

  void startMeasurements(String measurement);

  /**
   * Measures the execution time of a specific piece of code. The measurement starts with a call to
   * this method, and is terminated by another call to the same method with a different parameter
   * (to keep the measurement continuity), or by calling the <code>flushMeasurements</code> method.
   */
  void measure(String measurement);

  /**
   * Measures the same piece of code several times, for example, a method call in the middle of a
   * loop. Default implementation stores individual measurements, as well as their sum. <code>
   * stopPartialMeasurements</code> should be called before measuring again.
   */
  void startPartialMeasurement(String measurement);

  void stopPartialMeasurement();

  void saveRetryDelay(long retryDelay);

  void flushMeasurements(ConsumerRun consumerRun);
}
