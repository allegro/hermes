package pl.allegro.tech.hermes.common.kafka;

public record KafkaConsumerPoolConfig(
    int cacheExpirationSeconds,
    int bufferSizeBytes,
    int fetchMaxWaitMillis,
    int fetchMinBytes,
    String idPrefix,
    String consumerGroupName,
    boolean isSaslEnabled,
    String securityMechanism,
    String securityProtocol,
    String saslJaasConfig) {}
