package pl.allegro.tech.hermes.consumers;

public class ConsumerConfigurationProperties {

    public static String KAFKA_CLUSTER_NAME = "consumer.kafka.clusters.[0].clusterName";
    public static String CONSUMER_SSL_KEYSTORE_SOURCE = "consumer.ssl.keystoreSource";
    public static String CONSUMER_SSL_TRUSTSTORE_SOURCE = "consumer.ssl.truststoreSource";
    public static String CONSUMER_COMMIT_OFFSET_QUEUES_INFLIGHT_DRAIN_FULL = "consumer.commit.offset.queuesInflightDrainFullEnabled";
    public static String CONSUMER_COMMIT_OFFSET_PERIOD = "consumer.commit.offset.period";
    public static String GOOGLE_PUBSUB_TRANSPORT_CHANNEL_PROVIDER_ADDRESS = "consumer.google.pubsub.sender.transportChannelProviderAddress";
    public static String CONSUMER_HEALTH_CHECK_PORT = "consumer.healthCheckPort";
    public static String CONSUMER_USE_TOPIC_MESSAGE_SIZE = "consumer.useTopicMessageSize";
    public static String METRICS_ZOOKEEPER_REPORTER_ENABLED = "consumer.metrics.zookeeperReporterEnabled";
    public static String METRICS_GRAPHITE_REPORTER_ENABLED = "consumer.metrics.graphiteReporterEnabled";
    public static String SCHEMA_CACHE_ENABLED = "consumer.schema.cache.enabled";
    public static String SCHEMA_REPOSITORY_SERVER_URL = "consumer.schema.repository.serverUrl";
    public static String ZOOKEEPER_CONNECTION_STRING = "consumer.zookeeper.clusters.[0].connectionString";
    public static String KAFKA_AUTHORIZATION_ENABLED = "consumer.kafka.clusters.[0].authorization.enabled";
    public static String KAFKA_BROKER_LIST = "consumer.kafka.clusters.[0].brokerList";
}
