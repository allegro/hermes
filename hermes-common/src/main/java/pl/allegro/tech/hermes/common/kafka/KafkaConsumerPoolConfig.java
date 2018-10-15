package pl.allegro.tech.hermes.common.kafka;

public class KafkaConsumerPoolConfig {

    private final int cacheExpirationSeconds;
    private final int bufferSizeBytes;
    private final int fetchMaxWaitMillis;
    private final int fetchMinBytes;
    private final String idPrefix;
    private final String consumerGroupName;


    public KafkaConsumerPoolConfig(int cacheExpirationSeconds, int bufferSize, int fetchMaxWaitMillis,
                                   int fetchMinBytes, String idPrefix, String consumerGroupName) {
        this.cacheExpirationSeconds = cacheExpirationSeconds;
        this.bufferSizeBytes = bufferSize;
        this.fetchMaxWaitMillis = fetchMaxWaitMillis;
        this.fetchMinBytes = fetchMinBytes;
        this.idPrefix = idPrefix;
        this.consumerGroupName = consumerGroupName;
    }

    public int getCacheExpirationSeconds() {
        return cacheExpirationSeconds;
    }

    public int getBufferSizeBytes() {
        return bufferSizeBytes;
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
