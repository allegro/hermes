package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consumer.receiver")
public class ConsumerReceiverProperties {

    private int poolTimeout = 30;

    private int readQueueCapacity = 1000;

    private boolean waitBetweenUnsuccessfulPolls = true;

    private int initialIdleTime = 10;

    private int maxIdleTime = 1000;

    public int getPoolTimeout() {
        return poolTimeout;
    }

    public void setPoolTimeout(int poolTimeout) {
        this.poolTimeout = poolTimeout;
    }

    public int getReadQueueCapacity() {
        return readQueueCapacity;
    }

    public void setReadQueueCapacity(int readQueueCapacity) {
        this.readQueueCapacity = readQueueCapacity;
    }

    public boolean isWaitBetweenUnsuccessfulPolls() {
        return waitBetweenUnsuccessfulPolls;
    }

    public void setWaitBetweenUnsuccessfulPolls(boolean waitBetweenUnsuccessfulPolls) {
        this.waitBetweenUnsuccessfulPolls = waitBetweenUnsuccessfulPolls;
    }

    public int getInitialIdleTime() {
        return initialIdleTime;
    }

    public void setInitialIdleTime(int initialIdleTime) {
        this.initialIdleTime = initialIdleTime;
    }

    public int getMaxIdleTime() {
        return maxIdleTime;
    }

    public void setMaxIdleTime(int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }
}
