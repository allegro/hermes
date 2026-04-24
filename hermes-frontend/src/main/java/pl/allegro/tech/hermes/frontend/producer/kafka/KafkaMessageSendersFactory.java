package pl.allegro.tech.hermes.frontend.producer.kafka;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BATCH_SIZE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BUFFER_MEMORY_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.COMPRESSION_TYPE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.LINGER_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.MAX_BLOCK_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION;
import static org.apache.kafka.clients.producer.ProducerConfig.MAX_REQUEST_SIZE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.METADATA_MAX_AGE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.METRICS_SAMPLE_WINDOW_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.PARTITIONER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.RETRY_BACKOFF_MAX_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.RETRY_BACKOFF_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.SEND_BUFFER_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;
import static org.slf4j.LoggerFactory.getLogger;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.IntStream;
import org.apache.kafka.clients.admin.AdminClient;
import org.slf4j.Logger;
import pl.allegro.tech.hermes.common.kafka.KafkaParameters;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.producer.BrokerLatencyReporter;

public class KafkaMessageSendersFactory {

  private static final Logger logger = getLogger(KafkaMessageSendersFactory.class);

  private static final String ACK_ALL = "-1";
  private static final String ACK_LEADER = "1";

  private final TopicMetadataLoadingExecutor topicMetadataLoadingExecutor;
  private final MinInSyncReplicasLoader localMinInSyncReplicasLoader;
  private final KafkaParameters kafkaParameters;
  private final List<KafkaParameters> remoteKafkaParameters;
  private final BrokerLatencyReporter brokerLatencyReporter;
  private final MetricsFacade metricsFacade;
  private final long bufferedSizeBytes;
  private final ScheduledExecutorService chaosScheduler;

  public KafkaMessageSendersFactory(
      KafkaParameters kafkaParameters,
      List<KafkaParameters> remoteKafkaParameters,
      BrokerLatencyReporter brokerLatencyReporter,
      MetricsFacade metricsFacade,
      AdminClient localAdminClient,
      TopicsCache topicsCache,
      int retryCount,
      Duration retryInterval,
      int threadPoolSize,
      long bufferedSizeBytes,
      Duration metadataMaxAge,
      ScheduledExecutorService chaosScheduler) {
    this.topicMetadataLoadingExecutor =
        new TopicMetadataLoadingExecutor(topicsCache, retryCount, retryInterval, threadPoolSize);
    this.localMinInSyncReplicasLoader =
        new MinInSyncReplicasLoader(localAdminClient, metadataMaxAge);
    this.bufferedSizeBytes = bufferedSizeBytes;
    this.kafkaParameters = kafkaParameters;
    this.remoteKafkaParameters = remoteKafkaParameters;
    this.metricsFacade = metricsFacade;
    this.brokerLatencyReporter = brokerLatencyReporter;
    this.chaosScheduler = chaosScheduler;
  }

  public KafkaMessageSenders provide(
      KafkaProducerParameters kafkaProducerParameters, String senderName) {
    return provide(kafkaProducerParameters, kafkaProducerParameters, senderName);
  }

  public KafkaMessageSenders provide(
      KafkaProducerParameters localKafkaProducerParameters,
      KafkaProducerParameters remoteKafkaProducerParameters,
      String senderName) {
    KafkaMessageSenders.SenderPair localProducers =
        new KafkaMessageSenders.SenderPair(
            createProducerPool(
                kafkaParameters, localKafkaProducerParameters, ACK_LEADER, senderName),
            createProducerPool(kafkaParameters, localKafkaProducerParameters, ACK_ALL, senderName));

    List<KafkaMessageSenders.SenderPair> remoteProducers =
        remoteKafkaParameters.stream()
            .map(
                kafkaProperties ->
                    new KafkaMessageSenders.SenderPair(
                        createProducerPool(
                            kafkaProperties, remoteKafkaProducerParameters, ACK_LEADER, senderName),
                        createProducerPool(
                            kafkaProperties, remoteKafkaProducerParameters, ACK_ALL, senderName)))
            .toList();
    KafkaMessageSenders senders =
        new KafkaMessageSenders(
            topicMetadataLoadingExecutor,
            localMinInSyncReplicasLoader,
            metricsFacade,
            localProducers,
            remoteProducers);
    senders.registerSenderMetrics(senderName);
    return senders;
  }

  private KafkaMessageSenderPool createProducerPool(
      KafkaParameters kafkaParameters,
      KafkaProducerParameters kafkaProducerParameters,
      String acks,
      String senderName) {
    String poolName = senderName + "-" + acksToLabel(acks);
    List<KafkaMessageSender<byte[], byte[]>> senders =
        IntStream.range(0, kafkaProducerParameters.getPoolSize())
            .mapToObj(i -> sender(kafkaParameters, kafkaProducerParameters, acks))
            .toList();
    return new KafkaMessageSenderPool(poolName, senders);
  }

  private static String acksToLabel(String acks) {
    return switch (acks) {
      case ACK_ALL -> "ackAll";
      case ACK_LEADER -> "ackLeader";
      default -> "ack" + acks;
    };
  }

  private KafkaMessageSender<byte[], byte[]> sender(
      KafkaParameters kafkaParameters,
      KafkaProducerParameters kafkaProducerParameters,
      String acks) {
    Map<String, Object> props = new HashMap<>();
    props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaParameters.getBrokerList());
    props.put(MAX_BLOCK_MS_CONFIG, (int) kafkaProducerParameters.getMaxBlock().toMillis());
    props.put(COMPRESSION_TYPE_CONFIG, kafkaProducerParameters.getCompressionCodec());
    props.put(
        BUFFER_MEMORY_CONFIG,
        bufferedSizeBytes); // TODO @deprecated to be moved to the KafkaProducerParameters
    props.put(
        REQUEST_TIMEOUT_MS_CONFIG, (int) kafkaProducerParameters.getRequestTimeout().toMillis());
    props.put(
        DELIVERY_TIMEOUT_MS_CONFIG, (int) kafkaProducerParameters.getDeliveryTimeout().toMillis());
    props.put(BATCH_SIZE_CONFIG, kafkaProducerParameters.getBatchSize());
    props.put(SEND_BUFFER_CONFIG, kafkaProducerParameters.getTcpSendBuffer());
    props.put(RETRIES_CONFIG, kafkaProducerParameters.getRetries());
    props.put(RETRY_BACKOFF_MS_CONFIG, (int) kafkaProducerParameters.getRetryBackoff().toMillis());
    props.put(
        RETRY_BACKOFF_MAX_MS_CONFIG, (int) kafkaProducerParameters.getRetryBackoffMax().toMillis());
    props.put(
        METADATA_MAX_AGE_CONFIG, (int) kafkaProducerParameters.getMetadataMaxAge().toMillis());
    props.put(
        KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
    props.put(
        VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
    props.put(MAX_REQUEST_SIZE_CONFIG, kafkaProducerParameters.getMaxRequestSize());
    props.put(LINGER_MS_CONFIG, (int) kafkaProducerParameters.getLinger().toMillis());
    props.put(
        METRICS_SAMPLE_WINDOW_MS_CONFIG,
        (int) kafkaProducerParameters.getMetricsSampleWindow().toMillis());
    props.put(
        MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,
        kafkaProducerParameters.getMaxInflightRequestsPerConnection());
    props.put(ENABLE_IDEMPOTENCE_CONFIG, kafkaProducerParameters.isIdempotenceEnabled());
    props.put(ACKS_CONFIG, acks);

    String partitionerClass = kafkaProducerParameters.getPartitionerClass();
    if (partitionerClass != null && !partitionerClass.isBlank()) {
      props.put(PARTITIONER_CLASS_CONFIG, partitionerClass);
    }

    logger.info("Creating KafkaProducer with properties (excluding auth related): {}", props);

    if (kafkaParameters.isAuthenticationEnabled()) {
      props.put(SASL_MECHANISM, kafkaParameters.getAuthenticationMechanism());
      props.put(SECURITY_PROTOCOL_CONFIG, kafkaParameters.getAuthenticationProtocol());
      props.put(SASL_JAAS_CONFIG, kafkaParameters.getJaasConfig());
    }

    return new KafkaMessageSender<>(
        new org.apache.kafka.clients.producer.KafkaProducer<>(props),
        brokerLatencyReporter,
        kafkaParameters.getDatacenter(),
        chaosScheduler);
  }

  public void close() throws Exception {
    topicMetadataLoadingExecutor.close();
    localMinInSyncReplicasLoader.close();
  }
}
