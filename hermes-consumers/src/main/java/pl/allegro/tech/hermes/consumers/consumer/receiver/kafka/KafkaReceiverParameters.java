package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

public class KafkaReceiverParameters {
    private final int poolTimeout;
    private final int readQueueCapacity;
    private final boolean waitBetweenUnsuccessfulPolls;
    private final int initialIdleTime;
    private final int maxIdleTime;

    public KafkaReceiverParameters(int poolTimeout, int readQueueCapacity, boolean waitBetweenUnsuccessfulPolls, int initialIdleTime, int maxIdleTime) {
        this.poolTimeout = poolTimeout;
        this.readQueueCapacity = readQueueCapacity;
        this.waitBetweenUnsuccessfulPolls = waitBetweenUnsuccessfulPolls;
        this.initialIdleTime = initialIdleTime;
        this.maxIdleTime = maxIdleTime;
    }

    public int getPoolTimeout() {
        return poolTimeout;
    }

    public int getReadQueueCapacity() {
        return readQueueCapacity;
    }

    public boolean isWaitBetweenUnsuccessfulPollsEnabled() {
        return waitBetweenUnsuccessfulPolls;
    }

    public int getInitialIdleTime() {
        return initialIdleTime;
    }

    public int getMaxIdleTime() {
        return maxIdleTime;
    }
}
