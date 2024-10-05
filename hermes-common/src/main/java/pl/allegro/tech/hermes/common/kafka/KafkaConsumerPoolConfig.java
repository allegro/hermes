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

  public KafkaConsumerPoolConfig(
      int cacheExpirationSeconds,
      int bufferSize,
      int fetchMaxWaitMillis,
      int fetchMinBytes,
      String idPrefix,
      String consumerGroupName,
      boolean isSaslEnabled,
      String securityMechanism,
      String securityProtocol,
      String saslJaasConfig) {
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
}
