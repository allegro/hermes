package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consumer.commit.offset")
public class CommitOffsetProperties {

    private int period = 60;

    private int queuesSize = 200_000;

    private boolean queuesInflightDrainFullEnabled = false;

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public int getQueuesSize() {
        return queuesSize;
    }

    public void setQueuesSize(int queuesSize) {
        this.queuesSize = queuesSize;
    }

    public boolean isQueuesInflightDrainFullEnabled() {
        return queuesInflightDrainFullEnabled;
    }

    public void setQueuesInflightDrainFullEnabled(boolean queuesInflightDrainFullEnabled) {
        this.queuesInflightDrainFullEnabled = queuesInflightDrainFullEnabled;
    }
}
