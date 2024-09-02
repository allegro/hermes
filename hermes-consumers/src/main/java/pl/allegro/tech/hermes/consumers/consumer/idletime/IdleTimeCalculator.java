package pl.allegro.tech.hermes.consumers.consumer.idletime;

public interface IdleTimeCalculator {
  long increaseIdleTime();

  long getIdleTime();

  void reset();
}
