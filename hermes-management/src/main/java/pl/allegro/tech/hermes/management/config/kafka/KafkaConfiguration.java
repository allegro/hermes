package pl.allegro.tech.hermes.management.config.kafka;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL;
import static org.apache.kafka.clients.CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;

import java.time.Clock;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.broker.KafkaBrokerStorage;
import pl.allegro.tech.hermes.common.kafka.KafkaConsumerPool;
import pl.allegro.tech.hermes.common.kafka.KafkaConsumerPoolConfig;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.management.config.SubscriptionProperties;
import pl.allegro.tech.hermes.management.config.TopicProperties;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.subscription.ConsumerGroupManager;
import pl.allegro.tech.hermes.management.domain.topic.BrokerTopicManagement;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.BrokersClusterService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.KafkaBrokerTopicManagement;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.KafkaConsumerGroupManager;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.KafkaConsumerManager;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.KafkaRawMessageReader;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.KafkaSingleMessageReader;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.LogEndOffsetChecker;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.NoOpConsumerGroupManager;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.OffsetsAvailableChecker;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.retransmit.KafkaRetransmissionService;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperRepositoryManager;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

@Configuration
@EnableConfigurationProperties(KafkaClustersProperties.class)
public class KafkaConfiguration implements MultipleDcKafkaNamesMappersFactory {

  @Autowired KafkaClustersProperties kafkaClustersProperties;

  @Autowired TopicProperties topicProperties;

  @Autowired SubscriptionProperties subscriptionProperties;

  @Autowired CompositeMessageContentWrapper compositeMessageContentWrapper;

  @Autowired ZookeeperRepositoryManager zookeeperRepositoryManager;

  @Autowired MultiDatacenterRepositoryCommandExecutor multiDcExecutor;

  @Bean
  MultiDCAwareService multiDCAwareService(
      KafkaNamesMappers kafkaNamesMappers,
      SchemaRepository schemaRepository,
      Clock clock,
      JsonAvroConverter jsonAvroConverter) {
    List<DatacenterBoundRepositoryHolder<SubscriptionOffsetChangeIndicator>> repositories =
        zookeeperRepositoryManager.getRepositories(SubscriptionOffsetChangeIndicator.class);

    List<BrokersClusterService> clusters =
        kafkaClustersProperties.getClusters().stream()
            .map(
                kafkaProperties -> {
                  KafkaNamesMapper kafkaNamesMapper =
                      kafkaNamesMappers.getMapper(kafkaProperties.getQualifiedClusterName());
                  AdminClient brokerAdminClient = brokerAdminClient(kafkaProperties);
                  BrokerStorage storage = brokersStorage(brokerAdminClient);
                  BrokerTopicManagement brokerTopicManagement =
                      new KafkaBrokerTopicManagement(
                          topicProperties,
                          brokerAdminClient,
                          kafkaNamesMapper,
                          kafkaProperties.getDatacenter());
                  KafkaConsumerPool consumerPool =
                      kafkaConsumersPool(kafkaProperties, storage, kafkaProperties.getBrokerList());
                  KafkaRawMessageReader kafkaRawMessageReader =
                      new KafkaRawMessageReader(
                          consumerPool, kafkaProperties.getKafkaConsumer().getPollTimeoutMillis());
                  SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator =
                      getRepository(repositories, kafkaProperties);
                  KafkaRetransmissionService retransmissionService =
                      new KafkaRetransmissionService(
                          storage,
                          subscriptionOffsetChangeIndicator,
                          consumerPool,
                          kafkaNamesMapper);
                  KafkaSingleMessageReader messageReader =
                      new KafkaSingleMessageReader(
                          kafkaRawMessageReader, schemaRepository, jsonAvroConverter);
                  return new BrokersClusterService(
                      kafkaProperties.getQualifiedClusterName(),
                      messageReader,
                      retransmissionService,
                      brokerTopicManagement,
                      kafkaNamesMapper,
                      new OffsetsAvailableChecker(consumerPool, storage),
                      new LogEndOffsetChecker(consumerPool),
                      brokerAdminClient,
                      createConsumerGroupManager(kafkaProperties, kafkaNamesMapper),
                      createKafkaConsumerManager(kafkaProperties, kafkaNamesMapper));
                })
            .collect(toList());

    return new MultiDCAwareService(
        clusters,
        clock,
        ofMillis(subscriptionProperties.getIntervalBetweenCheckinIfOffsetsMovedInMillis()),
        ofSeconds(subscriptionProperties.getOffsetsMovedTimeoutInSeconds()),
        multiDcExecutor);
  }

  private ConsumerGroupManager createConsumerGroupManager(
      KafkaProperties kafkaProperties, KafkaNamesMapper kafkaNamesMapper) {
    return subscriptionProperties.isCreateConsumerGroupManuallyEnabled()
        ? new KafkaConsumerGroupManager(
            kafkaNamesMapper,
            kafkaProperties.getQualifiedClusterName(),
            kafkaProperties.getBrokerList(),
            kafkaProperties)
        : new NoOpConsumerGroupManager();
  }

  private KafkaConsumerManager createKafkaConsumerManager(
      KafkaProperties kafkaProperties, KafkaNamesMapper kafkaNamesMapper) {
    return new KafkaConsumerManager(
        kafkaProperties, kafkaNamesMapper, kafkaProperties.getBrokerList());
  }

  private SubscriptionOffsetChangeIndicator getRepository(
      List<DatacenterBoundRepositoryHolder<SubscriptionOffsetChangeIndicator>> repositories,
      KafkaProperties kafkaProperties) {
    if (repositories.size() == 1) {
      return repositories.get(0).getRepository();
    }
    return repositories.stream()
        .filter(
            repository -> kafkaProperties.getDatacenter().equals(repository.getDatacenterName()))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format(
                        "Kafka cluster dc name '%s' not matched with Zookeeper dc names: %s",
                        kafkaProperties.getDatacenter(),
                        repositories.stream()
                            .map(DatacenterBoundRepositoryHolder::getDatacenterName)
                            .collect(Collectors.joining(",")))))
        .getRepository();
  }

  @Bean
  @ConditionalOnMissingBean
  KafkaNamesMappers kafkaNameMappers() {
    return createDefaultKafkaNamesMapper(kafkaClustersProperties);
  }

  private BrokerStorage brokersStorage(AdminClient kafkaAdminClient) {
    return new KafkaBrokerStorage(kafkaAdminClient);
  }

  private KafkaConsumerPool kafkaConsumersPool(
      KafkaProperties kafkaProperties,
      BrokerStorage brokerStorage,
      String configuredBootstrapServers) {
    KafkaConsumerPoolConfig config =
        new KafkaConsumerPoolConfig(
            kafkaProperties.getKafkaConsumer().getCacheExpirationSeconds(),
            kafkaProperties.getKafkaConsumer().getBufferSizeBytes(),
            kafkaProperties.getKafkaConsumer().getFetchMaxWaitMillis(),
            kafkaProperties.getKafkaConsumer().getFetchMinBytes(),
            kafkaProperties.getKafkaConsumer().getNamePrefix(),
            kafkaProperties.getKafkaConsumer().getConsumerGroupName(),
            kafkaProperties.getAuthentication().isEnabled(),
            kafkaProperties.getAuthentication().getMechanism(),
            kafkaProperties.getAuthentication().getProtocol(),
            kafkaProperties.getAuthentication().getJaasConfig());

    return new KafkaConsumerPool(config, brokerStorage, configuredBootstrapServers);
  }

  private AdminClient brokerAdminClient(KafkaProperties kafkaProperties) {
    Properties props = new Properties();
    props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBrokerList());
    props.put(SECURITY_PROTOCOL_CONFIG, DEFAULT_SECURITY_PROTOCOL);
    props.put(REQUEST_TIMEOUT_MS_CONFIG, kafkaProperties.getKafkaServerRequestTimeoutMillis());
    if (kafkaProperties.getAuthentication().isEnabled()) {
      props.put(SASL_MECHANISM, kafkaProperties.getAuthentication().getMechanism());
      props.put(SECURITY_PROTOCOL_CONFIG, kafkaProperties.getAuthentication().getProtocol());
      props.put(SASL_JAAS_CONFIG, kafkaProperties.getAuthentication().getJaasConfig());
    }
    return AdminClient.create(props);
  }
}
