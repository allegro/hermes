package pl.allegro.tech.hermes.consumers;

public class ConsumerConfigurationProperties {

    public static String CONSUMER_SSL_KEYSTORE_SOURCE = "consumer.ssl.keystoreSource";
    public static String CONSUMER_SSL_TRUSTSTORE_SOURCE = "consumer.ssl.truststoreSource";
    public static String CONSUMER_COMMIT_OFFSET_QUEUES_INFLIGHT_DRAIN_FULL = "consumer.commit.offset.queuesInflightDrainFullEnabled";
    public static String CONSUMER_COMMIT_OFFSET_PERIOD = "consumer.commit.offset.period";
    public static String GOOGLE_PUBSUB_TRANSPORT_CHANNEL_PROVIDER_ADDRESS = "consumer.google.pubsub.sender.transportChannelProviderAddress";
    public static String CONSUMER_HEALTH_CHECK_PORT = "consumer.healthCheckPort";
    public static String CONSUMER_USE_TOPIC_MESSAGE_SIZE = "consumer.useTopicMessageSize";
}
