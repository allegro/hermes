package pl.allegro.tech.hermes.consumers;

public class ConsumersConfigurationProperties {
  public static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";
  public static final String CONSUMER_HEALTH_CHECK_PORT = "consumer.healthCheckPort";
  public static final String CONSUMER_KAFKA_NAMESPACE = "consumer.kafka.namespace";
  public static final String CONSUMER_KAFKA_CLUSTER_BROKER_LIST =
      "consumer.kafka.clusters.[0].brokerList";
  public static final String CONSUMER_KAFKA_CLUSTER_NAME =
      "consumer.kafka.clusters.[0].clusterName";
  public static final String CONSUMER_ZOOKEEPER_CONNECTION_STRING =
      "consumer.zookeeper.clusters.[0].connectionString";
  public static final String CONSUMER_SCHEMA_REPOSITORY_SERVER_URL =
      "consumer.schema.repository.serverUrl";
  public static final String CONSUMER_BACKGROUND_SUPERVISOR_INTERVAL =
      "consumer.backgroundSupervisor.interval";
  public static final String CONSUMER_WORKLOAD_REBALANCE_INTERVAL =
      "consumer.workload.rebalanceInterval";
  public static final String CONSUMER_COMMIT_OFFSET_PERIOD = "consumer.commit.offset.period";
  public static final String CONSUMER_METRICS_MICROMETER_REPORT_PERIOD =
      "consumer.metrics.micrometer.reportPeriod";
  public static final String CONSUMER_SCHEMA_CACHE_ENABLED = "consumer.schema.cache.enabled";
  public static final String CONSUMER_GOOGLE_PUBSUB_TRANSPORT_CHANNEL_PROVIDER_ADDRESS =
      "consumer.google.pubsub.sender.transportChannelProviderAddress";
  public static final String CONSUMER_MAX_RATE_UPDATE_INTERVAL = "consumer.maxrate.updateInterval";
  public static final String CONSUMER_MAX_RATE_BALANCE_INTERVAL =
      "consumer.maxrate.balanceInterval";
  public static final String CONSUMER_RATE_LIMITER_SUPERVISOR_PERIOD =
      "consumer.rate.limiterSupervisorPeriod";
  public static final String CONSUMER_RATE_LIMITER_HEARTBEAT_MODE_DELAY =
      "consumer.rate.limiterHeartbeatModeDelay";
  public static final String CONSUMER_RATE_CONVERGENCE_FACTOR = "consumer.rate.convergenceFactor";
}
