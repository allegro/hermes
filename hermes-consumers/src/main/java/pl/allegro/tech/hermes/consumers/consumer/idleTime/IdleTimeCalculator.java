package pl.allegro.tech.hermes.consumers.consumer.idleTime;

public interface IdleTimeCalculator {
    long increaseIdleTime();
    long getIdleTime();
    void reset();
}