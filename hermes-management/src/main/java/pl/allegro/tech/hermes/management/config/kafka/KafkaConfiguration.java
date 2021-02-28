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
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.BrokersClusterService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.KafkaBrokerTopicManagement;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.KafkaConsumerGroupManager;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;
import static org.apache.kafka.common.config.SslConfigs.SSL_CIPHER_SUITES_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_ENGINE_FACTORY_CLASS_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_KEYMANAGER_ALGORITHM_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_KEY_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_TYPE_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_KEY_PASSWORD_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_PROVIDER_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_SECURE_RANDOM_IMPLEMENTATION_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTMANAGER_ALGORITHM_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG;

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
    MultiDCAwareService multiDCAwareService(KafkaNamesMappers kafkaNamesMappers, SchemaRepository schemaRepository,
                                            Clock clock, JsonAvroConverter jsonAvroConverter) {
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

        return new MultiDCAwareService(
                clusters,
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
                kafkaProperties.getSecurityProtocol(),
                kafkaProperties.getSasl().isEnabled(),
                kafkaProperties.getSasl().getMechanism(),
                kafkaProperties.getSasl().getJaasConfig(),
                kafkaProperties.getSsl().isEnabled(),
                kafkaProperties.getSsl().getTrustStoreLocation(),
                kafkaProperties.getSsl().getTrustStorePassword(),
                kafkaProperties.getSsl().getKeyStoreLocation(),
                kafkaProperties.getSsl().getKeyStorePassword(),
                kafkaProperties.getSsl().getKeyPassword(),
                kafkaProperties.getSsl().getProtocolVersion(),
                kafkaProperties.getSsl().getEndpointIdentificationAlgorithm(),
                kafkaProperties.getSsl().getKeyStoreCertificateChain(),
                kafkaProperties.getSsl().getKeyStoreKey(),
                kafkaProperties.getSsl().getTrustStoreCertificates(),
                kafkaProperties.getSsl().getEnabledProtocols(),
                kafkaProperties.getSsl().getKeyStoreType(),
                kafkaProperties.getSsl().getProtocol(),
                kafkaProperties.getSsl().getProvider(),
                kafkaProperties.getSsl().getTrustStoreType(),
                kafkaProperties.getSsl().getCipherSuites(),
                kafkaProperties.getSsl().getEngineFactoryClass(),
                kafkaProperties.getSsl().getKeymanagerAlgorithm(),
                kafkaProperties.getSsl().getSecureRandomImplementation(),
                kafkaProperties.getSsl().getTrustmanagerAlgorithm());

        return new KafkaConsumerPool(config, brokerStorage, configuredBootstrapServers);
    }

    private AdminClient brokerAdminClient(KafkaProperties kafkaProperties) {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapKafkaServer());
        props.put(REQUEST_TIMEOUT_MS_CONFIG, kafkaProperties.getKafkaServerRequestTimeoutMillis());
        props.put(SECURITY_PROTOCOL_CONFIG, kafkaProperties.getSecurityProtocol());

        if (kafkaProperties.getSasl().isEnabled()) {
            props.put(SASL_MECHANISM, kafkaProperties.getSasl().getMechanism());
            props.put(SASL_JAAS_CONFIG, kafkaProperties.getSasl().getJaasConfig());
        }

        if (kafkaProperties.getSsl().isEnabled()) {
            Optional.ofNullable(kafkaProperties.getSsl().getKeyPassword()).ifPresent(v -> props.put(SSL_KEY_PASSWORD_CONFIG, v));
            Optional.ofNullable(kafkaProperties.getSsl().getKeyStoreCertificateChain())
                    .ifPresent(v -> props.put(SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG, v));
            Optional.ofNullable(kafkaProperties.getSsl().getKeyStoreKey()).ifPresent(v -> props.put(SSL_KEYSTORE_KEY_CONFIG, v));
            Optional.ofNullable(kafkaProperties.getSsl().getKeyStoreLocation()).ifPresent(v -> props.put(SSL_KEYSTORE_LOCATION_CONFIG, v));
            Optional.ofNullable(kafkaProperties.getSsl().getKeyStorePassword()).ifPresent(v -> props.put(SSL_KEYSTORE_PASSWORD_CONFIG, v));
            Optional.ofNullable(kafkaProperties.getSsl().getTrustStoreCertificates())
                    .ifPresent(v -> props.put(SSL_TRUSTSTORE_CERTIFICATES_CONFIG, v));
            Optional.ofNullable(kafkaProperties.getSsl().getTrustStoreLocation())
                    .ifPresent(v -> props.put(SSL_TRUSTSTORE_LOCATION_CONFIG, v));
            Optional.ofNullable(kafkaProperties.getSsl().getTrustStorePassword())
                    .ifPresent(v -> props.put(SSL_TRUSTSTORE_PASSWORD_CONFIG, v));
            Optional.ofNullable(kafkaProperties.getSsl().getEnabledProtocols()).map(s -> Arrays.asList(s.split(",")))
                    .ifPresent(v -> props.put(SSL_ENABLED_PROTOCOLS_CONFIG, v));
            Optional.ofNullable(kafkaProperties.getSsl().getKeyStoreType()).ifPresent(v -> props.put(SSL_KEYSTORE_TYPE_CONFIG, v));
            Optional.ofNullable(kafkaProperties.getSsl().getProtocolVersion()).ifPresent(v -> props.put(SSL_PROTOCOL_CONFIG, v));
            Optional.ofNullable(kafkaProperties.getSsl().getProvider()).ifPresent(v -> props.put(SSL_PROVIDER_CONFIG, v));
            Optional.ofNullable(kafkaProperties.getSsl().getTrustStoreType()).ifPresent(v -> props.put(SSL_TRUSTSTORE_TYPE_CONFIG, v));
            Optional.ofNullable(kafkaProperties.getSsl().getEndpointIdentificationAlgorithm())
                    .ifPresent(v -> props.put(SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, v));
            Optional.ofNullable(kafkaProperties.getSsl().getCipherSuites()).map(s -> Arrays.asList(s.split(",")))
                    .ifPresent(v -> props.put(SSL_CIPHER_SUITES_CONFIG, v));
            Optional.ofNullable(kafkaProperties.getSsl().getEngineFactoryClass())
                    .ifPresent(v -> props.put(SSL_ENGINE_FACTORY_CLASS_CONFIG, v));
            Optional.ofNullable(kafkaProperties.getSsl().getKeymanagerAlgorithm())
                    .ifPresent(v -> props.put(SSL_KEYMANAGER_ALGORITHM_CONFIG, v));
            Optional.ofNullable(kafkaProperties.getSsl().getSecureRandomImplementation())
                    .ifPresent(v -> props.put(SSL_SECURE_RANDOM_IMPLEMENTATION_CONFIG, v));
            Optional.ofNullable(kafkaProperties.getSsl().getTrustmanagerAlgorithm())
                    .ifPresent(v -> props.put(SSL_TRUSTMANAGER_ALGORITHM_CONFIG, v));
        }

        return AdminClient.create(props);
    }
}
