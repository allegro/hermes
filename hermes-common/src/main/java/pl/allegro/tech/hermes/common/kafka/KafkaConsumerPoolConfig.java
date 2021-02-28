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
    private final String sslEndpointIdentificationAlgorithm;

    public String getSslKeyStoreCertificateChain() {
        return sslKeyStoreCertificateChain;
    }

    public String getSslKeyStoreKey() {
        return sslKeyStoreKey;
    }

    public String getSslTrustStoreCertificates() {
        return sslTrustStoreCertificates;
    }

    public String getSslEnabledProtocols() {
        return sslEnabledProtocols;
    }

    public String getSslKeyStoreType() {
        return sslKeyStoreType;
    }

    public String getSslProtocol() {
        return sslProtocol;
    }

    public String getSslProvider() {
        return sslProvider;
    }

    public String getSslTrustStoreType() {
        return sslTrustStoreType;
    }

    public String getSslCipherSuites() {
        return sslCipherSuites;
    }

    public String getSslEngineFactoryClass() {
        return sslEngineFactoryClass;
    }

    public String getSslKeymanagerAlgorithm() {
        return sslKeymanagerAlgorithm;
    }

    public String getSslSecureRandomImplementation() {
        return sslSecureRandomImplementation;
    }

    public String getSslTrustmanagerAlgorithm() {
        return sslTrustmanagerAlgorithm;
    }

    private final String sslKeyStoreCertificateChain;
    private final String sslKeyStoreKey;
    private final String sslTrustStoreCertificates;
    private final String sslEnabledProtocols;
    private final String sslKeyStoreType;
    private final String sslProtocol;
    private final String sslProvider;
    private final String sslTrustStoreType;
    private final String sslCipherSuites;
    private final String sslEngineFactoryClass;
    private final String sslKeymanagerAlgorithm;
    private final String sslSecureRandomImplementation;
    private final String sslTrustmanagerAlgorithm;

    public KafkaConsumerPoolConfig(int cacheExpirationSeconds, int bufferSize, int fetchMaxWaitMillis,
                                   int fetchMinBytes, String idPrefix, String consumerGroupName, String securityProtocol,
                                   boolean isSaslEnabled, String securityMechanism, String saslJaasConfig,
                                   boolean isSslEnabled, String sslTrustStoreLocation, String sslTrustStorePassword,
                                   String sslKeyStoreLocation,
                                   String sslKeyStorePassword, String sslKeyPassword, String sslProtocolVersion,
                                   String sslEndpointIdentificationAlgorithm, String sslKeyStoreCertificateChain, String sslKeyStoreKey,
                                   String sslTrustStoreCertificates, String sslEnabledProtocols, String sslKeyStoreType, String sslProtocol,
                                   String sslProvider, String sslTrustStoreType, String sslCipherSuites, String sslEngineFactoryClass,
                                   String sslKeymanagerAlgorithm, String sslSecureRandomImplementation, String sslTrustmanagerAlgorithm) {
        this.cacheExpirationSeconds = cacheExpirationSeconds;
        this.bufferSizeBytes = bufferSize;
        this.fetchMaxWaitMillis = fetchMaxWaitMillis;
        this.fetchMinBytes = fetchMinBytes;
        this.idPrefix = idPrefix;
        this.consumerGroupName = consumerGroupName;
        this.securityProtocol = securityProtocol;
        this.isSaslEnabled = isSaslEnabled;
        this.securityMechanism = securityMechanism;
        this.saslJaasConfig = saslJaasConfig;
        this.isSslEnabled = isSslEnabled;
        this.sslTrustStoreLocation = sslTrustStoreLocation;
        this.sslTrustStorePassword = sslTrustStorePassword;
        this.sslKeyStoreLocation = sslKeyStoreLocation;
        this.sslKeyStorePassword = sslKeyStorePassword;
        this.sslKeyPassword = sslKeyPassword;
        this.sslProtocolVersion = sslProtocolVersion;
        this.sslEndpointIdentificationAlgorithm = sslEndpointIdentificationAlgorithm;
        this.sslKeyStoreCertificateChain = sslKeyStoreCertificateChain;
        this.sslKeyStoreKey = sslKeyStoreKey;
        this.sslTrustStoreCertificates = sslTrustStoreCertificates;
        this.sslEnabledProtocols = sslEnabledProtocols;
        this.sslKeyStoreType = sslKeyStoreType;
        this.sslProtocol = sslProtocol;
        this.sslProvider = sslProvider;
        this.sslTrustStoreType = sslTrustStoreType;
        this.sslCipherSuites = sslCipherSuites;
        this.sslEngineFactoryClass = sslEngineFactoryClass;
        this.sslKeymanagerAlgorithm = sslKeymanagerAlgorithm;
        this.sslSecureRandomImplementation = sslSecureRandomImplementation;
        this.sslTrustmanagerAlgorithm = sslTrustmanagerAlgorithm;
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
        return isSslEnabled;
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

    public String getSslEndpointIdentificationAlgorithm() {
        return sslEndpointIdentificationAlgorithm;
    }
}
