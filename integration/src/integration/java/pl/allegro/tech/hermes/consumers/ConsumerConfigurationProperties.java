package pl.allegro.tech.hermes.consumers;

public class ConsumerConfigurationProperties {

    public static String KAFKA_CONSUMER_RECONNECT_BACKOFF_MS_CONFIG = "kafka.consumer.reconnect.backoff.ms";
    public static String KAFKA_CONSUMER_RETRY_BACKOFF_MS_CONFIG = "kafka.consumer.retry.backoff.ms";
    public static String KAFKA_CONSUMER_REQUEST_TIMEOUT_MS_CONFIG = "kafka.consumer.request.timeout.ms";
    public static String KAFKA_CONSUMER_MAX_POLL_RECORDS_CONFIG = "kafka.consumer.max.poll.records";
    public static String KAFKA_CONSUMER_AUTO_OFFSET_RESET_CONFIG = "kafka.consumer.auto.offset.reset";
    public static String KAFKA_CONSUMER_SESSION_TIMEOUT_MS_CONFIG = "kafka.consumer.session.timeout.ms";
    public static String KAFKA_CONSUMER_HEARTBEAT_INTERVAL_MS_CONFIG = "kafka.consumer.heartbeat.interval.ms";
    public static String CONSUMER_SSL_KEYSTORE_SOURCE = "consumer.ssl.keystoreSource";
    public static String CONSUMER_SSL_TRUSTSTORE_SOURCE = "consumer.ssl.truststoreSource";
    public static String CONSUMER_COMMIT_OFFSET_QUEUES_INFLIGHT_DRAIN_FULL = "consumer.commit.offset.queuesInflightDrainFullEnabled";
    public static String CONSUMER_COMMIT_OFFSET_PERIOD = "consumer.commit.offset.period";
    public static String GOOGLE_PUBSUB_TRANSPORT_CHANNEL_PROVIDER_ADDRESS = "google.pubsub.sender.transportChannelProviderAddress";
    public static String CONSUMER_HEALTH_CHECK_PORT = "consumer.healthCheckPort";
    public static String CONSUMER_USE_TOPIC_MESSAGE_SIZE = "consumer.useTopicMessageSize";
    public static String METRICS_ZOOKEEPER_REPORTER_ENABLED = "consumer.metrics.zookeeperReporterEnabled";
    public static String METRICS_GRAPHITE_REPORTER_ENABLED = "consumer.metrics.graphiteReporterEnabled";
    public static String SCHEMA_CACHE_ENABLED = "consumer.schema.cache.enabled";
    public static String SCHEMA_REPOSITORY_SERVER_URL = "consumer.schema.repository.serverUrl";
}
