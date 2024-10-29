package pl.allegro.tech.hermes.frontend.config;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL;
import static org.apache.kafka.clients.CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;

import jakarta.inject.Named;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.kafka.KafkaParameters;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.config.FailFastKafkaProducerProperties.FallbackSchedulerProperties;
import pl.allegro.tech.hermes.frontend.producer.BrokerLatencyReporter;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.FallbackToRemoteDatacenterAwareMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaChaosProperties;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaHeaderFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaMessageSenders;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaMessageSendersFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.LocalDatacenterMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.MessageToKafkaProducerRecordConverter;
import pl.allegro.tech.hermes.frontend.producer.kafka.MultiDatacenterMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.ProducerMetadataLoadingJob;
import pl.allegro.tech.hermes.frontend.readiness.AdminReadinessService;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;

@Configuration
@EnableConfigurationProperties({
  LocalMessageStorageProperties.class,
  SchemaProperties.class,
  KafkaHeaderNameProperties.class,
  KafkaProducerProperties.class,
  FailFastKafkaProducerProperties.class,
  KafkaChaosProperties.class,
  KafkaClustersProperties.class,
  HTTPHeadersProperties.class
})
public class FrontendProducerConfiguration {

  @Bean
  public BrokerMessageProducer kafkaBrokerMessageProducer(
      @Named("localDatacenterBrokerProducer") BrokerMessageProducer localDatacenterBrokerProducer,
      @Named("multiDatacenterBrokerProducer") BrokerMessageProducer multiDatacenterBrokerProducer) {
    return new FallbackToRemoteDatacenterAwareMessageProducer(
        localDatacenterBrokerProducer, multiDatacenterBrokerProducer);
  }

  @Bean
  public BrokerMessageProducer localDatacenterBrokerProducer(
      @Named("kafkaMessageSenders") KafkaMessageSenders kafkaMessageSenders,
      MessageToKafkaProducerRecordConverter messageConverter) {
    return new LocalDatacenterMessageProducer(kafkaMessageSenders, messageConverter);
  }

  @Bean
  public BrokerMessageProducer multiDatacenterBrokerProducer(
      @Named("failFastKafkaMessageSenders") KafkaMessageSenders kafkaMessageSenders,
      MessageToKafkaProducerRecordConverter messageConverter,
      FailFastKafkaProducerProperties kafkaProducerProperties,
      AdminReadinessService adminReadinessService,
      InstrumentedExecutorServiceFactory executorServiceFactory) {
    FallbackSchedulerProperties fallbackSchedulerProperties =
        kafkaProducerProperties.getFallbackScheduler();
    ScheduledExecutorService fallbackScheduler =
        executorServiceFactory
            .scheduledExecutorBuilder(
                "fallback-to-remote", fallbackSchedulerProperties.getThreadPoolSize())
            .withMonitoringEnabled(fallbackSchedulerProperties.isThreadPoolMonitoringEnabled())
            .withRemoveOnCancel(true)
            .create();
    return new MultiDatacenterMessageProducer(
        kafkaMessageSenders,
        adminReadinessService,
        messageConverter,
        kafkaProducerProperties.getSpeculativeSendDelay(),
        fallbackScheduler);
  }

  @Bean
  public KafkaHeaderFactory kafkaHeaderFactory(
      KafkaHeaderNameProperties kafkaHeaderNameProperties,
      HTTPHeadersProperties httpHeadersProperties) {
    return new KafkaHeaderFactory(
        kafkaHeaderNameProperties, httpHeadersProperties.getPropagationAsKafkaHeaders());
  }

  @Bean(destroyMethod = "close")
  public KafkaMessageSenders kafkaMessageSenders(
      KafkaProducerProperties kafkaProducerProperties,
      KafkaMessageSendersFactory kafkaMessageSendersFactory) {
    return kafkaMessageSendersFactory.provide(kafkaProducerProperties, "default");
  }

  @Bean(destroyMethod = "close")
  public KafkaMessageSenders failFastKafkaMessageSenders(
      FailFastKafkaProducerProperties kafkaProducerProperties,
      KafkaMessageSendersFactory kafkaMessageSendersFactory) {
    return kafkaMessageSendersFactory.provide(
        kafkaProducerProperties.getLocal(), kafkaProducerProperties.getRemote(), "failFast");
  }

  @Bean
  public ScheduledExecutorService chaosScheduler(
      KafkaChaosProperties chaosProperties,
      InstrumentedExecutorServiceFactory executorServiceFactory) {
    KafkaChaosProperties.ChaosSchedulerProperties chaosSchedulerProperties =
        chaosProperties.getChaosScheduler();
    return executorServiceFactory
        .scheduledExecutorBuilder("chaos", chaosSchedulerProperties.getThreadPoolSize())
        .withMonitoringEnabled(chaosSchedulerProperties.isThreadPoolMonitoringEnabled())
        .create();
  }

  @Bean(destroyMethod = "close")
  public KafkaMessageSendersFactory kafkaMessageSendersFactory(
      KafkaClustersProperties kafkaClustersProperties,
      KafkaProducerProperties kafkaProducerProperties,
      TopicLoadingProperties topicLoadingProperties,
      TopicsCache topicsCache,
      LocalMessageStorageProperties localMessageStorageProperties,
      DatacenterNameProvider datacenterNameProvider,
      BrokerLatencyReporter brokerLatencyReporter,
      MetricsFacade metricsFacade,
      @Named("chaosScheduler") ScheduledExecutorService chaosScheduler) {
    KafkaProperties kafkaProperties =
        kafkaClustersProperties.toKafkaProperties(datacenterNameProvider);
    List<KafkaParameters> remoteKafkaProperties =
        kafkaClustersProperties.toRemoteKafkaProperties(datacenterNameProvider);

    return new KafkaMessageSendersFactory(
        kafkaProperties,
        remoteKafkaProperties,
        brokerLatencyReporter,
        metricsFacade,
        createAdminClient(kafkaProperties),
        topicsCache,
        topicLoadingProperties.getMetadata().getRetryCount(),
        topicLoadingProperties.getMetadata().getRetryInterval(),
        topicLoadingProperties.getMetadata().getThreadPoolSize(),
        localMessageStorageProperties.getBufferedSizeBytes(),
        kafkaProducerProperties.getMetadataMaxAge(),
        chaosScheduler);
  }

  private static AdminClient createAdminClient(KafkaProperties kafkaProperties) {
    Properties props = new Properties();
    props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBrokerList());
    props.put(SECURITY_PROTOCOL_CONFIG, DEFAULT_SECURITY_PROTOCOL);
    props.put(REQUEST_TIMEOUT_MS_CONFIG, (int) kafkaProperties.getAdminRequestTimeout().toMillis());
    if (kafkaProperties.isAuthenticationEnabled()) {
      props.put(SASL_MECHANISM, kafkaProperties.getAuthenticationMechanism());
      props.put(SECURITY_PROTOCOL_CONFIG, kafkaProperties.getAuthenticationProtocol());
      props.put(SASL_JAAS_CONFIG, kafkaProperties.getJaasConfig());
    }
    return AdminClient.create(props);
  }

  @Bean(initMethod = "start", destroyMethod = "stop")
  public ProducerMetadataLoadingJob producerMetadataLoadingJob(
      List<KafkaMessageSenders> kafkaMessageSendersList,
      TopicLoadingProperties topicLoadingProperties) {
    return new ProducerMetadataLoadingJob(
        kafkaMessageSendersList,
        topicLoadingProperties.getMetadataRefreshJob().isEnabled(),
        topicLoadingProperties.getMetadataRefreshJob().getInterval());
  }

  @Bean
  public MessageToKafkaProducerRecordConverter messageToKafkaProducerRecordConverter(
      KafkaHeaderFactory kafkaHeaderFactory, SchemaProperties schemaProperties) {
    return new MessageToKafkaProducerRecordConverter(
        kafkaHeaderFactory, schemaProperties.isIdHeaderEnabled());
  }
}
