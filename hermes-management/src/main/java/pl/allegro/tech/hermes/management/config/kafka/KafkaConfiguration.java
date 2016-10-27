package pl.allegro.tech.hermes.management.config.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;
import pl.allegro.tech.hermes.common.admin.AdminTool;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.broker.ZookeeperBrokerStorage;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.SimpleConsumerPool;
import pl.allegro.tech.hermes.common.kafka.SimpleConsumerPoolConfig;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.management.config.TopicProperties;
import pl.allegro.tech.hermes.management.domain.topic.BrokerTopicManagement;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.BrokersClusterService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.KafkaBrokerTopicManagement;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.KafkaRawMessageReader;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.KafkaSingleMessageReader;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.OffsetsAvailableChecker;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.retransmit.KafkaRetransmissionService;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Configuration
@EnableConfigurationProperties(KafkaClustersProperties.class)
public class KafkaConfiguration implements MultipleDcKafkaNamesMappersFactory {

    @Autowired
    KafkaClustersProperties kafkaClustersProperties;

    @Autowired
    TopicProperties topicProperties;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    MessageContentWrapper messageContentWrapper;

    @Autowired
    SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator;

    @Autowired
    AdminTool adminTool;

    private final List<ZkClient> zkClients = new ArrayList<>();
    private final List<CuratorFramework> curators = new ArrayList<>();

    @Bean
    MultiDCAwareService multiDCAwareService(KafkaNamesMappers kafkaNamesMappers, SchemaRepository schemaRepository) {
        List<BrokersClusterService> clusters = kafkaClustersProperties.getClusters().stream().map(kafkaProperties -> {
            KafkaNamesMapper kafkaNamesMapper = kafkaNamesMappers.getMapper(kafkaProperties.getClusterName());

            BrokerStorage storage = brokersStorage(curatorFramework(kafkaProperties));
            BrokerTopicManagement brokerTopicManagement = new KafkaBrokerTopicManagement(topicProperties, zkClient(kafkaProperties), kafkaNamesMapper);

            SimpleConsumerPool simpleConsumerPool = simpleConsumersPool(kafkaProperties, storage);
            KafkaRawMessageReader kafkaRawMessageReader = new KafkaRawMessageReader(simpleConsumerPool);
            KafkaRetransmissionService retransmissionService = new KafkaRetransmissionService(
                    storage,
                    kafkaRawMessageReader,
                    messageContentWrapper,
                    subscriptionOffsetChangeIndicator,
                    simpleConsumerPool,
                    kafkaNamesMapper,
                    schemaRepository
            );
            KafkaSingleMessageReader messageReader = new KafkaSingleMessageReader(kafkaRawMessageReader, schemaRepository, new JsonAvroConverter());
            return new BrokersClusterService(kafkaProperties.getClusterName(), messageReader,
                    retransmissionService, brokerTopicManagement, kafkaNamesMapper, new OffsetsAvailableChecker(simpleConsumerPool, storage));
        }).collect(toList());

        return new MultiDCAwareService(clusters, adminTool);
    }


    @Bean
    @ConditionalOnMissingBean
    KafkaNamesMappers kafkaNameMappers() {
        return createDefaultKafkaNamesMapper(kafkaClustersProperties);
    }

    @PreDestroy
    public void shutdown() {
        curators.forEach(CuratorFramework::close);
        zkClients.forEach(ZkClient::close);
    }

    private ZkUtils zkClient(KafkaProperties kafkaProperties) {
        ZkConnection connection = new ZkConnection(kafkaProperties.getConnectionString(), kafkaProperties.getSessionTimeout());

        ZkClient zkClient = new ZkClient(
                connection,
                kafkaProperties.getConnectionTimeout(),
                ZKStringSerializer$.MODULE$
        );

        zkClient.waitUntilConnected();

        zkClients.add(zkClient);

        return new ZkUtils(zkClient, connection, false);
    }

    private CuratorFramework curatorFramework(KafkaProperties kafkaProperties) {
        CuratorFramework curator = CuratorFrameworkFactory.newClient(
                kafkaProperties.getConnectionString(),
                new RetryNTimes(kafkaProperties.getRetryTimes(), kafkaProperties.getRetrySleep()));

        curator.start();

        curators.add(curator);

        return curator;
    }

    private BrokerStorage brokersStorage(CuratorFramework curatorFramework) {
        return new ZookeeperBrokerStorage(curatorFramework, mapper);
    }

    private SimpleConsumerPool simpleConsumersPool(KafkaProperties kafkaProperties, BrokerStorage brokerStorage) {
        SimpleConsumerPoolConfig config = new SimpleConsumerPoolConfig(
                kafkaProperties.getSimpleConsumer().getCacheExpiration(),
                kafkaProperties.getSimpleConsumer().getTimeout(),
                kafkaProperties.getSimpleConsumer().getBufferSize(),
                kafkaProperties.getSimpleConsumer().getNamePrefix());

        return new SimpleConsumerPool(config, brokerStorage);
    }
}
