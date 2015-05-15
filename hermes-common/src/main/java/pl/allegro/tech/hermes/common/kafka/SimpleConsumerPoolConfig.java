package pl.allegro.tech.hermes.common.kafka;

public class SimpleConsumerPoolConfig {

    private final int cacheExpiration;

    private final int timeout;

    private final int bufferSize;

    private final String idPrefix;

    public SimpleConsumerPoolConfig(int cacheExpiration, int timeout, int bufferSize, String idPrefix) {
        this.cacheExpiration = cacheExpiration;
        this.timeout = timeout;
        this.bufferSize = bufferSize;
        this.idPrefix = idPrefix;
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
}
