package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaConsumerParameters;

import java.time.Duration;

@ConfigurationProperties(prefix = "kafka.consumer")
public class KafkaConsumerProperties {

    private int sendBufferBytes = 256 * 1024;

    private int receiveBufferBytes = 256 * 1024;

    private int fetchMinBytes = 1;

    private Duration fetchMaxWait = Duration.ofMillis(500);

    private Duration reconnectBackoff = Duration.ofMillis(500);

    private Duration retryBackoff = Duration.ofMillis(500);

    private boolean checkCrcs = true;

    private Duration metricsSampleWindow = Duration.ofSeconds(30);

    private int metricsNumSamples = 2;

    private Duration requestTimeout = Duration.ofSeconds(250);

    private Duration connectionsMaxIdle = Duration.ofMinutes(9);

    private int maxPollRecords = 1;

    private Duration maxPollInterval = Duration.ofMillis(Integer.MAX_VALUE);

    private String autoOffsetReset = "earliest";

    private Duration sessionTimeout = Duration.ofSeconds(200);

    private Duration heartbeatInterval = Duration.ofSeconds(3);

    private Duration metadataMaxAge = Duration.ofMinutes(5);

    private int maxPartitionFetchMin = Topic.MIN_MESSAGE_SIZE;

    private int maxPartitionFetchMax = Topic.MAX_MESSAGE_SIZE;

    public int getSendBufferBytes() {
        return sendBufferBytes;
    }

    public void setSendBufferBytes(int sendBufferBytes) {
        this.sendBufferBytes = sendBufferBytes;
    }

    public int getReceiveBufferBytes() {
        return receiveBufferBytes;
    }

    public void setReceiveBufferBytes(int receiveBufferBytes) {
        this.receiveBufferBytes = receiveBufferBytes;
    }

    public int getFetchMinBytes() {
        return fetchMinBytes;
    }

    public void setFetchMinBytes(int fetchMinBytes) {
        this.fetchMinBytes = fetchMinBytes;
    }

    public Duration getFetchMaxWait() {
        return fetchMaxWait;
    }

    public void setFetchMaxWait(Duration fetchMaxWait) {
        this.fetchMaxWait = fetchMaxWait;
    }

    public Duration getReconnectBackoff() {
        return reconnectBackoff;
    }

    public void setReconnectBackoff(Duration reconnectBackoff) {
        this.reconnectBackoff = reconnectBackoff;
    }

    public Duration getRetryBackoff() {
        return retryBackoff;
    }

    public void setRetryBackoff(Duration retryBackoff) {
        this.retryBackoff = retryBackoff;
    }

    public boolean isCheckCrcs() {
        return checkCrcs;
    }

    public void setCheckCrcs(boolean checkCrcs) {
        this.checkCrcs = checkCrcs;
    }

    public Duration getMetricsSampleWindow() {
        return metricsSampleWindow;
    }

    public void setMetricsSampleWindow(Duration metricsSampleWindow) {
        this.metricsSampleWindow = metricsSampleWindow;
    }

    public int getMetricsNumSamples() {
        return metricsNumSamples;
    }

    public void setMetricsNumSamples(int metricsNumSamples) {
        this.metricsNumSamples = metricsNumSamples;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Duration getConnectionsMaxIdle() {
        return connectionsMaxIdle;
    }

    public void setConnectionsMaxIdle(Duration connectionsMaxIdle) {
        this.connectionsMaxIdle = connectionsMaxIdle;
    }

    public int getMaxPollRecords() {
        return maxPollRecords;
    }

    public void setMaxPollRecords(int maxPollRecords) {
        this.maxPollRecords = maxPollRecords;
    }

    public Duration getMaxPollInterval() {
        return maxPollInterval;
    }

    public void setMaxPollInterval(Duration maxPollInterval) {
        this.maxPollInterval = maxPollInterval;
    }

    public String getAutoOffsetReset() {
        return autoOffsetReset;
    }

    public void setAutoOffsetReset(String autoOffsetReset) {
        this.autoOffsetReset = autoOffsetReset;
    }

    public Duration getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(Duration sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public Duration getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(Duration heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public Duration getMetadataMaxAge() {
        return metadataMaxAge;
    }

    public void setMetadataMaxAge(Duration metadataMaxAge) {
        this.metadataMaxAge = metadataMaxAge;
    }

    public int getMaxPartitionFetchMin() {
        return maxPartitionFetchMin;
    }

    public void setMaxPartitionFetchMin(int maxPartitionFetchMin) {
        this.maxPartitionFetchMin = maxPartitionFetchMin;
    }

    public int getMaxPartitionFetchMax() {
        return maxPartitionFetchMax;
    }

    public void setMaxPartitionFetchMax(int maxPartitionFetchMax) {
        this.maxPartitionFetchMax = maxPartitionFetchMax;
    }

    protected KafkaConsumerParameters toKafkaConsumerParameters() {
        return new KafkaConsumerParameters(
                this.sendBufferBytes,
                this.receiveBufferBytes,
                this.fetchMinBytes,
                this.fetchMaxWait,
                this.reconnectBackoff,
                this.retryBackoff,
                this.checkCrcs,
                this.metricsSampleWindow,
                this.metricsNumSamples,
                this.requestTimeout,
                this.connectionsMaxIdle,
                this.maxPollRecords,
                this.maxPollInterval,
                this.autoOffsetReset,
                this.sessionTimeout,
                this.heartbeatInterval,
                this.metadataMaxAge,
                this.maxPartitionFetchMin,
                this.maxPartitionFetchMax
        );
    }
}
