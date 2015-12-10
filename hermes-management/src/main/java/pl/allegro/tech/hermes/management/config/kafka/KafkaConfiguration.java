package pl.allegro.tech.hermes.management.config.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import kafka.utils.ZKStringSerializer$;
import org.I0Itec.zkclient.ZkClient;
import org.apache.avro.Schema;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.admin.AdminTool;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.broker.ZookeeperBrokerStorage;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.NamespaceKafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.SimpleConsumerPool;
import pl.allegro.tech.hermes.common.kafka.SimpleConsumerPoolConfig;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
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
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Configuration
@EnableConfigurationProperties(KafkaClustersProperties.class)
public class KafkaConfiguration {

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

    @Autowired
    SchemaRepository<Schema> avroSchemaRepository;

    private final List<ZkClient> zkClients = new ArrayList<>();
    private final List<CuratorFramework> curators = new ArrayList<>();

    @Bean
    MultiDCAwareService multiDCAwareService(KafkaNameMappers kafkaNameMappers) {
        List<BrokersClusterService> clusters = kafkaClustersProperties.getClusters().stream().map(kafkaProperties -> {
            KafkaNamesMapper kafkaNameMapper = kafkaNameMappers.getMapper(kafkaProperties.getClusterName());

            BrokerStorage storage = brokersStorage(curatorFramework(kafkaProperties));
            BrokerTopicManagement brokerTopicManagement = new KafkaBrokerTopicManagement(topicProperties, zkClient(kafkaProperties), kafkaNameMapper);

            SimpleConsumerPool simpleConsumerPool = simpleConsumersPool(kafkaProperties, storage);
            KafkaRawMessageReader kafkaRawMessageReader = new KafkaRawMessageReader(simpleConsumerPool);
            KafkaRetransmissionService retransmissionService = new KafkaRetransmissionService(
                    storage,
                    kafkaRawMessageReader,
                    messageContentWrapper,
                    subscriptionOffsetChangeIndicator,
                    simpleConsumerPool,
                    kafkaNameMapper
            );

            return new BrokersClusterService(kafkaProperties.getClusterName(), new KafkaSingleMessageReader(kafkaRawMessageReader, avroSchemaRepository),
                    retransmissionService, brokerTopicManagement, kafkaNameMapper, new OffsetsAvailableChecker(simpleConsumerPool, storage));
        }).collect(toList());

        return new MultiDCAwareService(clusters, adminTool);
    }


    @Bean
    @ConditionalOnMissingBean
    KafkaNameMappers kafkaNameMappers() {
        Map<String, KafkaNamesMapper> mappers = kafkaClustersProperties.getClusters().stream()
                .filter(c -> c.getNamespace().isEmpty())
                .collect(toMap(KafkaProperties::getClusterName,
                        p -> new NamespaceKafkaNamesMapper(kafkaClustersProperties.getDefaultNamespace())));

        mappers.putAll(kafkaClustersProperties.getClusters().stream()
                .filter(c -> !c.getNamespace().isEmpty())
                .collect(toMap(KafkaProperties::getClusterName,
                        p -> new NamespaceKafkaNamesMapper(p.getNamespace()))));

        return new KafkaNameMappers(mappers);
    }

    @PreDestroy
    public void shutdown() {
        curators.forEach(CuratorFramework::close);
        zkClients.forEach(ZkClient::close);
    }

    private ZkClient zkClient(KafkaProperties kafkaProperties) {
        ZkClient zkClient = new ZkClient(
                kafkaProperties.getConnectionString(),
                kafkaProperties.getSessionTimeout(),
                kafkaProperties.getConnectionTimeout(),
                ZKStringSerializer$.MODULE$
        );

        zkClient.waitUntilConnected();

        zkClients.add(zkClient);

        return zkClient;
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
