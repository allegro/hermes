package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import java.time.Duration;

public class KafkaReceiverParameters {
    private final Duration poolTimeout;
    private final int readQueueCapacity;
    private final boolean waitBetweenUnsuccessfulPolls;
    private final Duration initialIdleTime;
    private final Duration maxIdleTime;

    public KafkaReceiverParameters(Duration poolTimeout, int readQueueCapacity, boolean waitBetweenUnsuccessfulPolls, Duration initialIdleTime, Duration maxIdleTime) {
        this.poolTimeout = poolTimeout;
        this.readQueueCapacity = readQueueCapacity;
        this.waitBetweenUnsuccessfulPolls = waitBetweenUnsuccessfulPolls;
        this.initialIdleTime = initialIdleTime;
        this.maxIdleTime = maxIdleTime;
    }

    public Duration getPoolTimeout() {
        return poolTimeout;
    }

    public int getReadQueueCapacity() {
        return readQueueCapacity;
    }

    public boolean isWaitBetweenUnsuccessfulPollsEnabled() {
        return waitBetweenUnsuccessfulPolls;
    }

    public Duration getInitialIdleTime() {
        return initialIdleTime;
    }

    public Duration getMaxIdleTime() {
        return maxIdleTime;
    }
}
