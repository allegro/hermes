package pl.allegro.tech.hermes.consumers;

public class ConsumerConfigurationProperties {

    public static String KAFKA_CONSUMER_RECONNECT_BACKOFF_MS_CONFIG = "kafka.consumer.reconnect.backoff.ms";
    public static String KAFKA_CONSUMER_RETRY_BACKOFF_MS_CONFIG = "kafka.consumer.retry.backoff.ms";
    public static String KAFKA_CONSUMER_REQUEST_TIMEOUT_MS_CONFIG = "kafka.consumer.request.timeout.ms";
    public static String KAFKA_CONSUMER_MAX_POLL_RECORDS_CONFIG = "kafka.consumer.max.poll.records";
    public static String KAFKA_CONSUMER_AUTO_OFFSET_RESET_CONFIG = "kafka.consumer.auto.offset.reset";
    public static String KAFKA_CONSUMER_SESSION_TIMEOUT_MS_CONFIG = "kafka.consumer.session.timeout.ms";
    public static String KAFKA_CONSUMER_HEARTBEAT_INTERVAL_MS_CONFIG = "kafka.consumer.heartbeat.interval.ms";

}
