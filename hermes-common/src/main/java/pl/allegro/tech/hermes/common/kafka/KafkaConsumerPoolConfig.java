package pl.allegro.tech.hermes.common.kafka;

public class KafkaConsumerPoolConfig {

    private final int cacheExpiration;
    private final int timeout;
    private final int bufferSize;
    private final int fetchMaxWaitMillis;
    private final int fetchMinBytes;
    private final String idPrefix;
    private final String consumerGroupName;


    public KafkaConsumerPoolConfig(int cacheExpiration, int timeout, int bufferSize, int fetchMaxWaitMillis,
                                   int fetchMinBytes, String idPrefix, String consumerGroupName) {
        this.cacheExpiration = cacheExpiration;
        this.timeout = timeout;
        this.bufferSize = bufferSize;
        this.fetchMaxWaitMillis = fetchMaxWaitMillis;
        this.fetchMinBytes = fetchMinBytes;
        this.idPrefix = idPrefix;
        this.consumerGroupName = consumerGroupName;
    }

    public int getCacheExpiration() {
        return cacheExpiration;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public int getTimeout() {
        return timeout;
    }

    public String getIdPrefix() {
        return idPrefix;
    }

    public String getConsumerGroupName() {
        return consumerGroupName;
    }

    public int getFetchMaxWaitMillis() {
        return fetchMaxWaitMillis;
    }

    public int getFetchMinBytes() {
        return fetchMinBytes;
    }
}
