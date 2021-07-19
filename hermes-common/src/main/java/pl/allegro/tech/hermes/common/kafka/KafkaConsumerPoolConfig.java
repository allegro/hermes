package pl.allegro.tech.hermes.common.kafka;

public class KafkaConsumerPoolConfig {

    private final int cacheExpirationSeconds;
    private final int bufferSizeBytes;
    private final int fetchMaxWaitMillis;
    private final int fetchMinBytes;
    private final String idPrefix;
    private final String consumerGroupName;
    private final boolean isSaslEnabled;
    private final String securityMechanism;
    private final String securityProtocol;
    private final String saslJaasConfig;
    private final boolean isSslEnabled;
    private final String sslTrustStoreLocation;
    private final String sslTrustStorePassword;
    private final String sslKeyStoreLocation;
    private final String sslKeyStorePassword;
    private final String sslKeyPassword;
    private final String sslProtocolVersion;
    private final String sslSecurityProtocol;
    private final String sslEndpointIdentificationAlgorithm;

    public KafkaConsumerPoolConfig(int cacheExpirationSeconds, int bufferSize, int fetchMaxWaitMillis,
                                   int fetchMinBytes, String idPrefix, String consumerGroupName,
                                   boolean isSaslEnabled, String securityMechanism, String securityProtocol, String saslJaasConfig,
                                   boolean isSslEnabled, String sslTrustStoreLocation, String sslTrustStorePassword, String sslKeyStoreLocation,
                                   String sslKeyStorePassword, String sslKeyPassword, String sslProtocolVersion,
                                   String sslSecurityProtocol, String sslEndpointIdentificationAlgorithm) {
        this.cacheExpirationSeconds = cacheExpirationSeconds;
        this.bufferSizeBytes = bufferSize;
        this.fetchMaxWaitMillis = fetchMaxWaitMillis;
        this.fetchMinBytes = fetchMinBytes;
        this.idPrefix = idPrefix;
        this.consumerGroupName = consumerGroupName;
        this.isSaslEnabled = isSaslEnabled;
        this.securityMechanism = securityMechanism;
        this.securityProtocol = securityProtocol;
        this.saslJaasConfig = saslJaasConfig;
        this.isSslEnabled = isSslEnabled;
        this.sslTrustStoreLocation = sslTrustStoreLocation;
        this.sslTrustStorePassword = sslTrustStorePassword;
        this.sslKeyStoreLocation = sslKeyStoreLocation;
        this.sslKeyStorePassword = sslKeyStorePassword;
        this.sslKeyPassword = sslKeyPassword;
        this.sslProtocolVersion = sslProtocolVersion;
        this.sslSecurityProtocol = sslSecurityProtocol;
        this.sslEndpointIdentificationAlgorithm = sslEndpointIdentificationAlgorithm;
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

    public boolean isSaslEnabled() {
        return isSaslEnabled;
    }

    public String getSecurityMechanism() {
        return securityMechanism;
    }

    public String getSecurityProtocol() {
        return securityProtocol;
    }

    public String getSaslJaasConfig() {
        return saslJaasConfig;
    }

    public boolean isSslEnabled() {
        return isSaslEnabled;
    }

    public String getSslTrustStoreLocation() {
        return sslTrustStoreLocation;
    }

    public String getSslTrustStorePassword() {
        return sslTrustStorePassword;
    }

    public String getSslKeyStoreLocation() {
        return sslKeyStoreLocation;
    }

    public String getSslKeyStorePassword() {
        return sslKeyStorePassword;
    }

    public String getSslKeyPassword() {
        return sslKeyPassword;
    }

    public String getSslProtocolVersion() {
        return sslProtocolVersion;
    }

    public String getSslSecurityProtocol() {
        return sslSecurityProtocol;
    }

    public String getSslEndpointIdentificationAlgorithm() {
        return sslEndpointIdentificationAlgorithm;
    }
}
