package pl.allegro.tech.hermes.management.config.kafka;

import java.util.Properties;
import kafka.zk.AdminZkClient;
import kafka.zk.KafkaZkClient;
import kafka.zookeeper.ZooKeeperClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.utils.Time;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.admin.AdminTool;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.broker.ZookeeperBrokerStorage;
import pl.allegro.tech.hermes.common.kafka.KafkaConsumerPool;
import pl.allegro.tech.hermes.common.kafka.KafkaConsumerPoolConfig;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.management.config.SubscriptionProperties;
import pl.allegro.tech.hermes.management.config.TopicProperties;
import pl.allegro.tech.hermes.management.domain.topic.BrokerTopicManagement;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.BrokersClusterService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.KafkaBrokerTopicManagement;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.KafkaRawMessageReader;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.KafkaSingleMessageReader;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.OffsetsAvailableChecker;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.retransmit.KafkaRetransmissionService;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

import javax.annotation.PreDestroy;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;

@Configuration
@EnableConfigurationProperties(KafkaClustersProperties.class)
public class KafkaConfiguration implements MultipleDcKafkaNamesMappersFactory {

    private final static String ZOOKEEPER_METRIC_GROUP = "zookeeper-metrics-group";
    private final static String ZOOKEEPER_METRIC_TYPE = "zookeeper";

    @Autowired
    KafkaClustersProperties kafkaClustersProperties;

    @Autowired
    TopicProperties topicProperties;

    @Autowired
    SubscriptionProperties subscriptionProperties;

    @Autowired
    MessageContentWrapper messageContentWrapper;

    @Autowired
    SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator;

    @Autowired
    AdminTool adminTool;

    private final List<ZooKeeperClient> zkClients = new ArrayList<>();
    private final List<CuratorFramework> curators = new ArrayList<>();

    @Bean
    MultiDCAwareService multiDCAwareService(KafkaNamesMappers kafkaNamesMappers, SchemaRepository schemaRepository,
                                            Clock clock) {
        List<BrokersClusterService> clusters = kafkaClustersProperties.getClusters().stream().map(kafkaProperties -> {
            KafkaNamesMapper kafkaNamesMapper = kafkaNamesMappers.getMapper(kafkaProperties.getClusterName());

            ZooKeeperClient zooKeeperClient = zooKeeperClient(kafkaProperties);
            KafkaZkClient kafkaZkClient = kafkaZkClient(zooKeeperClient);
            AdminZkClient adminZkClient = adminZkClient(kafkaZkClient);
            AdminClient brokerAdminClient = brokerAdminClient(kafkaProperties.getBootstrapKafkaServer());

            BrokerStorage storage = brokersStorage(curatorFramework(kafkaProperties), kafkaZkClient);

            BrokerTopicManagement brokerTopicManagement = new KafkaBrokerTopicManagement(topicProperties, adminZkClient, kafkaZkClient, kafkaNamesMapper);

            KafkaConsumerPool consumerPool = kafkaConsumersPool(kafkaProperties, storage);
            KafkaRawMessageReader kafkaRawMessageReader =
                    new KafkaRawMessageReader(consumerPool, kafkaProperties.getKafkaConsumer().getPollTimeoutMillis());
            KafkaRetransmissionService retransmissionService = new KafkaRetransmissionService(
                    storage,
                    subscriptionOffsetChangeIndicator,
                    consumerPool,
                    kafkaNamesMapper
            );
            KafkaSingleMessageReader messageReader = new KafkaSingleMessageReader(kafkaRawMessageReader, schemaRepository, new JsonAvroConverter());
            return new BrokersClusterService(kafkaProperties.getClusterName(), messageReader,
                    retransmissionService, brokerTopicManagement, kafkaNamesMapper, new OffsetsAvailableChecker(consumerPool, storage), brokerAdminClient);
        }).collect(toList());

        return new MultiDCAwareService(
                clusters,
                adminTool,
                clock,
                ofMillis(subscriptionProperties.getIntervalBetweenCheckinIfOffsetsMovedInMillis()),
                ofSeconds(subscriptionProperties.getOffsetsMovedTimeoutInSeconds()));
    }

    @Bean
    @ConditionalOnMissingBean
    KafkaNamesMappers kafkaNameMappers() {
        return createDefaultKafkaNamesMapper(kafkaClustersProperties);
    }

    @PreDestroy
    public void shutdown() {
        curators.forEach(CuratorFramework::close);
        zkClients.forEach(ZooKeeperClient::close);
    }

    private AdminZkClient adminZkClient(KafkaZkClient kafkaZkClient) {
        return new AdminZkClient(kafkaZkClient);
    }

    private KafkaZkClient kafkaZkClient(ZooKeeperClient zooKeeperClient) {
        return new KafkaZkClient(zooKeeperClient, false, Time.SYSTEM);
    }

    private ZooKeeperClient zooKeeperClient(KafkaProperties kafkaProperties) {
        ZooKeeperClient zooKeeperClient =  new ZooKeeperClient(
                kafkaProperties.getConnectionString(),
                kafkaProperties.getSessionTimeoutMillis(),
                kafkaProperties.getConnectionTimeoutMillis(),
                kafkaProperties.getMaxInflight(),
                Time.SYSTEM, ZOOKEEPER_METRIC_GROUP, ZOOKEEPER_METRIC_TYPE);

        zkClients.add(zooKeeperClient);
        zooKeeperClient.waitUntilConnected();
        return zooKeeperClient;
    }

    private CuratorFramework curatorFramework(KafkaProperties kafkaProperties) {
        CuratorFramework curator = CuratorFrameworkFactory.newClient(
                kafkaProperties.getConnectionString(),
                new RetryNTimes(kafkaProperties.getRetryTimes(), kafkaProperties.getRetrySleepMillis()));

        curator.start();

        curators.add(curator);

        return curator;
    }

    private BrokerStorage brokersStorage(CuratorFramework curatorFramework, KafkaZkClient kafkaZkClient) {
        return new ZookeeperBrokerStorage(curatorFramework, kafkaZkClient);
    }

    private KafkaConsumerPool kafkaConsumersPool(KafkaProperties kafkaProperties, BrokerStorage brokerStorage) {
        KafkaConsumerPoolConfig config = new KafkaConsumerPoolConfig(
                kafkaProperties.getKafkaConsumer().getCacheExpirationSeconds(),
                kafkaProperties.getKafkaConsumer().getBufferSizeBytes(),
                kafkaProperties.getKafkaConsumer().getFetchMaxWaitMillis(),
                kafkaProperties.getKafkaConsumer().getFetchMinBytes(),
                kafkaProperties.getKafkaConsumer().getNamePrefix(),
                kafkaProperties.getKafkaConsumer().getConsumerGroupName());

        return new KafkaConsumerPool(config, brokerStorage);
    }

    private AdminClient brokerAdminClient(String bootstrapServers) {
        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return AdminClient.create(props);
    }

}
