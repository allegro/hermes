package pl.allegro.tech.hermes.management.config.kafka;

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
import pl.allegro.tech.hermes.management.infrastructure.kafka.ClustersProvider;
import pl.allegro.tech.hermes.management.infrastructure.kafka.ClustersProvider2;
import pl.allegro.tech.hermes.management.infrastructure.kafka.ClustersProvider3;
import pl.allegro.tech.hermes.management.infrastructure.kafka.ClustersProvider4;
import pl.allegro.tech.hermes.management.infrastructure.kafka.ClustersProvider5;
import pl.allegro.tech.hermes.management.infrastructure.kafka.ClustersProvider6;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService2;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService3;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService4;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService5;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService6;
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

import java.time.Clock;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL;
import static org.apache.kafka.clients.CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;

@Configuration
@EnableConfigurationProperties(KafkaClustersProperties.class)
public class KafkaConfiguration implements MultipleDcKafkaNamesMappersFactory {

    @Autowired
    KafkaClustersProperties kafkaClustersProperties;

    @Autowired
    TopicProperties topicProperties;

    @Autowired
    SubscriptionProperties subscriptionProperties;

    @Autowired
    CompositeMessageContentWrapper compositeMessageContentWrapper;

    @Autowired
    ZookeeperRepositoryManager zookeeperRepositoryManager;

    @Autowired
    MultiDatacenterRepositoryCommandExecutor multiDcExecutor;

    @Bean
    ClustersProvider clustersProvider(KafkaNamesMappers kafkaNamesMappers, SchemaRepository schemaRepository,
                                      JsonAvroConverter jsonAvroConverter) {
        List<DatacenterBoundRepositoryHolder<SubscriptionOffsetChangeIndicator>> repositories =
                zookeeperRepositoryManager.getRepositories(SubscriptionOffsetChangeIndicator.class);

        List<BrokersClusterService> clusters = kafkaClustersProperties.getClusters().stream().map(kafkaProperties -> {
            KafkaNamesMapper kafkaNamesMapper = kafkaNamesMappers.getMapper(kafkaProperties.getQualifiedClusterName());
            AdminClient brokerAdminClient = brokerAdminClient(kafkaProperties);
            BrokerStorage storage = brokersStorage(brokerAdminClient);
            BrokerTopicManagement brokerTopicManagement =
                    new KafkaBrokerTopicManagement(topicProperties, brokerAdminClient, kafkaNamesMapper);
            KafkaConsumerPool consumerPool = kafkaConsumersPool(kafkaProperties, storage, kafkaProperties.getBootstrapKafkaServer());
            KafkaRawMessageReader kafkaRawMessageReader =
                    new KafkaRawMessageReader(consumerPool, kafkaProperties.getKafkaConsumer().getPollTimeoutMillis());
            SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator = getRepository(repositories, kafkaProperties);
            KafkaRetransmissionService retransmissionService = new KafkaRetransmissionService(
                    storage,
                    subscriptionOffsetChangeIndicator,
                    consumerPool,
                    kafkaNamesMapper
            );
            KafkaSingleMessageReader messageReader =
                    new KafkaSingleMessageReader(kafkaRawMessageReader, schemaRepository, jsonAvroConverter);
            return new BrokersClusterService(kafkaProperties.getQualifiedClusterName(), messageReader,
                    retransmissionService, brokerTopicManagement, kafkaNamesMapper,
                    new OffsetsAvailableChecker(consumerPool, storage), new LogEndOffsetChecker(consumerPool),
                    brokerAdminClient, createConsumerGroupManager(kafkaProperties, kafkaNamesMapper),
                    createKafkaConsumerManager(kafkaProperties, kafkaNamesMapper));
        }).collect(toList());

        return new ClustersProvider(clusters);
    }

    @Bean
    ClustersProvider2 clustersProvider2(KafkaNamesMappers kafkaNamesMappers, SchemaRepository schemaRepository,
                                      JsonAvroConverter jsonAvroConverter) {
        List<DatacenterBoundRepositoryHolder<SubscriptionOffsetChangeIndicator>> repositories =
                zookeeperRepositoryManager.getRepositories(SubscriptionOffsetChangeIndicator.class);

        List<BrokersClusterService> clusters = kafkaClustersProperties.getClusters().stream().map(kafkaProperties -> {
            KafkaNamesMapper kafkaNamesMapper = kafkaNamesMappers.getMapper(kafkaProperties.getQualifiedClusterName());
            AdminClient brokerAdminClient = brokerAdminClient(kafkaProperties);
            BrokerStorage storage = brokersStorage(brokerAdminClient);
            BrokerTopicManagement brokerTopicManagement =
                    new KafkaBrokerTopicManagement(topicProperties, brokerAdminClient, kafkaNamesMapper);
            KafkaConsumerPool consumerPool = kafkaConsumersPool(kafkaProperties, storage, kafkaProperties.getBootstrapKafkaServer());
            KafkaRawMessageReader kafkaRawMessageReader =
                    new KafkaRawMessageReader(consumerPool, kafkaProperties.getKafkaConsumer().getPollTimeoutMillis());
            SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator = getRepository(repositories, kafkaProperties);
            KafkaRetransmissionService retransmissionService = new KafkaRetransmissionService(
                    storage,
                    subscriptionOffsetChangeIndicator,
                    consumerPool,
                    kafkaNamesMapper
            );
            KafkaSingleMessageReader messageReader =
                    new KafkaSingleMessageReader(kafkaRawMessageReader, schemaRepository, jsonAvroConverter);
            return new BrokersClusterService(kafkaProperties.getQualifiedClusterName(), messageReader,
                    retransmissionService, brokerTopicManagement, kafkaNamesMapper,
                    new OffsetsAvailableChecker(consumerPool, storage),
                    new LogEndOffsetChecker(consumerPool),
                    brokerAdminClient, createConsumerGroupManager(kafkaProperties, kafkaNamesMapper));
        }).collect(toList());

        return new ClustersProvider2(clusters);
    }

    @Bean
    ClustersProvider3 clustersProvider3(KafkaNamesMappers kafkaNamesMappers, SchemaRepository schemaRepository,
                                        JsonAvroConverter jsonAvroConverter) {
        List<DatacenterBoundRepositoryHolder<SubscriptionOffsetChangeIndicator>> repositories =
                zookeeperRepositoryManager.getRepositories(SubscriptionOffsetChangeIndicator.class);

        List<BrokersClusterService> clusters = kafkaClustersProperties.getClusters().stream().map(kafkaProperties -> {
            KafkaNamesMapper kafkaNamesMapper = kafkaNamesMappers.getMapper(kafkaProperties.getQualifiedClusterName());
            AdminClient brokerAdminClient = brokerAdminClient(kafkaProperties);
            BrokerStorage storage = brokersStorage(brokerAdminClient);
            BrokerTopicManagement brokerTopicManagement =
                    new KafkaBrokerTopicManagement(topicProperties, brokerAdminClient, kafkaNamesMapper);
            KafkaConsumerPool consumerPool = kafkaConsumersPool(kafkaProperties, storage, kafkaProperties.getBootstrapKafkaServer());
            KafkaRawMessageReader kafkaRawMessageReader =
                    new KafkaRawMessageReader(consumerPool, kafkaProperties.getKafkaConsumer().getPollTimeoutMillis());
            SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator = getRepository(repositories, kafkaProperties);
            KafkaRetransmissionService retransmissionService = new KafkaRetransmissionService(
                    storage,
                    subscriptionOffsetChangeIndicator,
                    consumerPool,
                    kafkaNamesMapper
            );
            KafkaSingleMessageReader messageReader =
                    new KafkaSingleMessageReader(kafkaRawMessageReader, schemaRepository, jsonAvroConverter);
            return new BrokersClusterService(kafkaProperties.getQualifiedClusterName(), messageReader,
                    retransmissionService, brokerTopicManagement, kafkaNamesMapper,
                    new OffsetsAvailableChecker(consumerPool, storage),
                    new LogEndOffsetChecker(consumerPool),
                    brokerAdminClient, createConsumerGroupManager(kafkaProperties, kafkaNamesMapper));
        }).collect(toList());

        return new ClustersProvider3(clusters);
    }
    @Bean
    ClustersProvider4 clustersProvider4(KafkaNamesMappers kafkaNamesMappers, SchemaRepository schemaRepository,
                                        JsonAvroConverter jsonAvroConverter) {
        List<DatacenterBoundRepositoryHolder<SubscriptionOffsetChangeIndicator>> repositories =
                zookeeperRepositoryManager.getRepositories(SubscriptionOffsetChangeIndicator.class);

        List<BrokersClusterService> clusters = kafkaClustersProperties.getClusters().stream().map(kafkaProperties -> {
            KafkaNamesMapper kafkaNamesMapper = kafkaNamesMappers.getMapper(kafkaProperties.getQualifiedClusterName());
            AdminClient brokerAdminClient = brokerAdminClient(kafkaProperties);
            BrokerStorage storage = brokersStorage(brokerAdminClient);
            BrokerTopicManagement brokerTopicManagement =
                    new KafkaBrokerTopicManagement(topicProperties, brokerAdminClient, kafkaNamesMapper);
            KafkaConsumerPool consumerPool = kafkaConsumersPool(kafkaProperties, storage, kafkaProperties.getBootstrapKafkaServer());
            KafkaRawMessageReader kafkaRawMessageReader =
                    new KafkaRawMessageReader(consumerPool, kafkaProperties.getKafkaConsumer().getPollTimeoutMillis());
            SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator = getRepository(repositories, kafkaProperties);
            KafkaRetransmissionService retransmissionService = new KafkaRetransmissionService(
                    storage,
                    subscriptionOffsetChangeIndicator,
                    consumerPool,
                    kafkaNamesMapper
            );
            KafkaSingleMessageReader messageReader =
                    new KafkaSingleMessageReader(kafkaRawMessageReader, schemaRepository, jsonAvroConverter);
            return new BrokersClusterService(kafkaProperties.getQualifiedClusterName(), messageReader,
                    retransmissionService, brokerTopicManagement, kafkaNamesMapper,
                    new OffsetsAvailableChecker(consumerPool, storage),
                    new LogEndOffsetChecker(consumerPool),
                    brokerAdminClient, createConsumerGroupManager(kafkaProperties, kafkaNamesMapper));
        }).collect(toList());

        return new ClustersProvider4(clusters);
    }
    @Bean
    ClustersProvider5 clustersProvider5(KafkaNamesMappers kafkaNamesMappers, SchemaRepository schemaRepository,
                                        JsonAvroConverter jsonAvroConverter) {
        List<DatacenterBoundRepositoryHolder<SubscriptionOffsetChangeIndicator>> repositories =
                zookeeperRepositoryManager.getRepositories(SubscriptionOffsetChangeIndicator.class);

        List<BrokersClusterService> clusters = kafkaClustersProperties.getClusters().stream().map(kafkaProperties -> {
            KafkaNamesMapper kafkaNamesMapper = kafkaNamesMappers.getMapper(kafkaProperties.getQualifiedClusterName());
            AdminClient brokerAdminClient = brokerAdminClient(kafkaProperties);
            BrokerStorage storage = brokersStorage(brokerAdminClient);
            BrokerTopicManagement brokerTopicManagement =
                    new KafkaBrokerTopicManagement(topicProperties, brokerAdminClient, kafkaNamesMapper);
            KafkaConsumerPool consumerPool = kafkaConsumersPool(kafkaProperties, storage, kafkaProperties.getBootstrapKafkaServer());
            KafkaRawMessageReader kafkaRawMessageReader =
                    new KafkaRawMessageReader(consumerPool, kafkaProperties.getKafkaConsumer().getPollTimeoutMillis());
            SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator = getRepository(repositories, kafkaProperties);
            KafkaRetransmissionService retransmissionService = new KafkaRetransmissionService(
                    storage,
                    subscriptionOffsetChangeIndicator,
                    consumerPool,
                    kafkaNamesMapper
            );
            KafkaSingleMessageReader messageReader =
                    new KafkaSingleMessageReader(kafkaRawMessageReader, schemaRepository, jsonAvroConverter);
            return new BrokersClusterService(kafkaProperties.getQualifiedClusterName(), messageReader,
                    retransmissionService, brokerTopicManagement, kafkaNamesMapper,
                    new OffsetsAvailableChecker(consumerPool, storage),
                    new LogEndOffsetChecker(consumerPool),
                    brokerAdminClient, createConsumerGroupManager(kafkaProperties, kafkaNamesMapper));
        }).collect(toList());

        return new ClustersProvider5(clusters);
    }
    @Bean
    ClustersProvider6 clustersProvider6(KafkaNamesMappers kafkaNamesMappers, SchemaRepository schemaRepository,
                                        JsonAvroConverter jsonAvroConverter) {
        List<DatacenterBoundRepositoryHolder<SubscriptionOffsetChangeIndicator>> repositories =
                zookeeperRepositoryManager.getRepositories(SubscriptionOffsetChangeIndicator.class);

        List<BrokersClusterService> clusters = kafkaClustersProperties.getClusters().stream().map(kafkaProperties -> {
            KafkaNamesMapper kafkaNamesMapper = kafkaNamesMappers.getMapper(kafkaProperties.getQualifiedClusterName());
            AdminClient brokerAdminClient = brokerAdminClient(kafkaProperties);
            BrokerStorage storage = brokersStorage(brokerAdminClient);
            BrokerTopicManagement brokerTopicManagement =
                    new KafkaBrokerTopicManagement(topicProperties, brokerAdminClient, kafkaNamesMapper);
            KafkaConsumerPool consumerPool = kafkaConsumersPool(kafkaProperties, storage, kafkaProperties.getBootstrapKafkaServer());
            KafkaRawMessageReader kafkaRawMessageReader =
                    new KafkaRawMessageReader(consumerPool, kafkaProperties.getKafkaConsumer().getPollTimeoutMillis());
            SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator = getRepository(repositories, kafkaProperties);
            KafkaRetransmissionService retransmissionService = new KafkaRetransmissionService(
                    storage,
                    subscriptionOffsetChangeIndicator,
                    consumerPool,
                    kafkaNamesMapper
            );
            KafkaSingleMessageReader messageReader =
                    new KafkaSingleMessageReader(kafkaRawMessageReader, schemaRepository, jsonAvroConverter);
            return new BrokersClusterService(kafkaProperties.getQualifiedClusterName(), messageReader,
                    retransmissionService, brokerTopicManagement, kafkaNamesMapper,
                    new OffsetsAvailableChecker(consumerPool, storage),
                    new LogEndOffsetChecker(consumerPool),
                    brokerAdminClient, createConsumerGroupManager(kafkaProperties, kafkaNamesMapper));
        }).collect(toList());

        return new ClustersProvider6(clusters);
    }

    @Bean
    MultiDCAwareService multiDCAwareService(ClustersProvider clustersProvider, Clock clock) {
        return new MultiDCAwareService(
                clustersProvider,
                clock,
                ofMillis(subscriptionProperties.getIntervalBetweenCheckinIfOffsetsMovedInMillis()),
                ofSeconds(subscriptionProperties.getOffsetsMovedTimeoutInSeconds()),
                multiDcExecutor);
    }

    @Bean
    MultiDCAwareService2 multiDCAwareService2(ClustersProvider2 clustersProvider, Clock clock) {
        return new MultiDCAwareService2(
                clustersProvider,
                clock,
                ofMillis(subscriptionProperties.getIntervalBetweenCheckinIfOffsetsMovedInMillis()),
                ofSeconds(subscriptionProperties.getOffsetsMovedTimeoutInSeconds()),
                multiDcExecutor);
    }

    @Bean
    MultiDCAwareService3 multiDCAwareService3(ClustersProvider3 clustersProvider, Clock clock) {
        return new MultiDCAwareService3(
                clustersProvider,
                clock,
                ofMillis(subscriptionProperties.getIntervalBetweenCheckinIfOffsetsMovedInMillis()),
                ofSeconds(subscriptionProperties.getOffsetsMovedTimeoutInSeconds()),
                multiDcExecutor);
    }
    @Bean
    MultiDCAwareService4 multiDCAwareService4(ClustersProvider4 clustersProvider, Clock clock) {
        return new MultiDCAwareService4(
                clustersProvider,
                clock,
                ofMillis(subscriptionProperties.getIntervalBetweenCheckinIfOffsetsMovedInMillis()),
                ofSeconds(subscriptionProperties.getOffsetsMovedTimeoutInSeconds()),
                multiDcExecutor);
    }
    @Bean
    MultiDCAwareService5 multiDCAwareService5(ClustersProvider5 clustersProvider, Clock clock) {
        return new MultiDCAwareService5(
                clustersProvider,
                clock,
                ofMillis(subscriptionProperties.getIntervalBetweenCheckinIfOffsetsMovedInMillis()),
                ofSeconds(subscriptionProperties.getOffsetsMovedTimeoutInSeconds()),
                multiDcExecutor);
    }
    @Bean
    MultiDCAwareService6 multiDCAwareService6(ClustersProvider6 clustersProvider, Clock clock) {
        return new MultiDCAwareService6(
                clustersProvider,
                clock,
                ofMillis(subscriptionProperties.getIntervalBetweenCheckinIfOffsetsMovedInMillis()),
                ofSeconds(subscriptionProperties.getOffsetsMovedTimeoutInSeconds()),
                multiDcExecutor);
    }

    private ConsumerGroupManager createConsumerGroupManager(KafkaProperties kafkaProperties, KafkaNamesMapper kafkaNamesMapper) {
        return subscriptionProperties.isCreateConsumerGroupManuallyEnabled()
                ? new KafkaConsumerGroupManager(kafkaNamesMapper, kafkaProperties.getQualifiedClusterName(),
                        kafkaProperties.getBootstrapKafkaServer(), kafkaProperties)
                : new NoOpConsumerGroupManager();
    }

    private KafkaConsumerManager createKafkaConsumerManager(KafkaProperties kafkaProperties,
                                                            KafkaNamesMapper kafkaNamesMapper) {
        return new KafkaConsumerManager(kafkaProperties, kafkaNamesMapper, kafkaProperties.getBootstrapKafkaServer());
    }

    private SubscriptionOffsetChangeIndicator getRepository(
            List<DatacenterBoundRepositoryHolder<SubscriptionOffsetChangeIndicator>> repositories,
            KafkaProperties kafkaProperties) {
        if (repositories.size() == 1) {
            return repositories.get(0).getRepository();
        }
        return repositories.stream()
                .filter(repository -> kafkaProperties.getDatacenter().equals(repository.getDatacenterName()))
                .findFirst().orElseThrow(() ->
                        new IllegalArgumentException(
                                String.format("Kafka cluster dc name '%s' not matched with Zookeeper dc names: %s",
                                        kafkaProperties.getDatacenter(),
                                        repositories.stream()
                                                .map(DatacenterBoundRepositoryHolder::getDatacenterName)
                                                .collect(Collectors.joining(","))
                                )
                        )
                )
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

    private KafkaConsumerPool kafkaConsumersPool(KafkaProperties kafkaProperties, BrokerStorage brokerStorage,
                                                 String configuredBootstrapServers) {
        KafkaConsumerPoolConfig config = new KafkaConsumerPoolConfig(
                kafkaProperties.getKafkaConsumer().getCacheExpirationSeconds(),
                kafkaProperties.getKafkaConsumer().getBufferSizeBytes(),
                kafkaProperties.getKafkaConsumer().getFetchMaxWaitMillis(),
                kafkaProperties.getKafkaConsumer().getFetchMinBytes(),
                kafkaProperties.getKafkaConsumer().getNamePrefix(),
                kafkaProperties.getKafkaConsumer().getConsumerGroupName(),
                kafkaProperties.getSasl().isEnabled(),
                kafkaProperties.getSasl().getMechanism(),
                kafkaProperties.getSasl().getProtocol(),
                kafkaProperties.getSasl().getJaasConfig());

        return new KafkaConsumerPool(config, brokerStorage, configuredBootstrapServers);
    }

    private AdminClient brokerAdminClient(KafkaProperties kafkaProperties) {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapKafkaServer());
        props.put(SECURITY_PROTOCOL_CONFIG, DEFAULT_SECURITY_PROTOCOL);
        props.put(REQUEST_TIMEOUT_MS_CONFIG, kafkaProperties.getKafkaServerRequestTimeoutMillis());
        if (kafkaProperties.getSasl().isEnabled()) {
            props.put(SASL_MECHANISM, kafkaProperties.getSasl().getMechanism());
            props.put(SECURITY_PROTOCOL_CONFIG, kafkaProperties.getSasl().getProtocol());
            props.put(SASL_JAAS_CONFIG, kafkaProperties.getSasl().getJaasConfig());
        }
        return AdminClient.create(props);
    }
}
