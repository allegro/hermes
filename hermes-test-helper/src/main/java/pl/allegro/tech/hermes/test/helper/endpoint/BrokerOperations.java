package pl.allegro.tech.hermes.test.helper.endpoint;

import com.jayway.awaitility.Duration;
import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.admin.RackAwareMode$;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.JsonToAvroMigrationKafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;

import java.util.Map;
import java.util.Properties;

import static com.jayway.awaitility.Awaitility.waitAtMost;
import static java.util.stream.Collectors.toMap;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

/**
 * Created to perform operations directly on broker excluding Hermes internal structures
 */
public class BrokerOperations {

    private static final int DEFAULT_PARTITIONS = 2;
    private static final int DEFAULT_REPLICATION_FACTOR = 1;
    private Map<String, ZkUtils> zkClients;
    private KafkaNamesMapper kafkaNamesMapper;

    public BrokerOperations(Map<String, String> kafkaZkConnection, ConfigFactory configFactory) {
        this(kafkaZkConnection, configFactory.getIntProperty(Configs.ZOOKEEPER_SESSION_TIMEOUT),
                configFactory.getIntProperty(Configs.ZOOKEEPER_CONNECTION_TIMEOUT),
                configFactory.getStringProperty(Configs.KAFKA_NAMESPACE));
    }

    public BrokerOperations(Map<String, String> kafkaZkConnection, int sessionTimeout, int connectionTimeout, String namespace) {
        zkClients = kafkaZkConnection.entrySet().stream()
                .collect(toMap(Map.Entry::getKey,
                               e -> {
                                   ZkConnection connection = new ZkConnection(e.getValue(), sessionTimeout);
                                   ZkClient zkClient = new ZkClient(connection, connectionTimeout, ZKStringSerializer$.MODULE$);
                                   return new ZkUtils(zkClient, connection, false);
                               }));
        kafkaNamesMapper = new JsonToAvroMigrationKafkaNamesMapper(namespace);
    }

    public void createTopic(String topicName) {
        zkClients.values().forEach(c -> createTopic(topicName, c));
    }

    public void createTopic(String topicName, String brokerName) {
        createTopic(topicName, zkClients.get(brokerName));
    }

    private void createTopic(String topicName, ZkUtils zkUtils) {
        Topic topic = topic(topicName).build();
        kafkaNamesMapper.toKafkaTopics(topic).forEach(kafkaTopic -> {
            AdminUtils.createTopic(zkUtils, kafkaTopic.name().asString(), DEFAULT_PARTITIONS, DEFAULT_REPLICATION_FACTOR, new Properties(), RackAwareMode.Enforced$.MODULE$);

            waitAtMost(adjust(Duration.ONE_MINUTE)).until(() -> {
                        AdminUtils.topicExists(zkUtils, kafkaTopic.name().asString());
                    }
            );
        });
    }

    public boolean topicExists(String topicName, String kafkaClusterName) {
        Topic topic = topic(topicName).build();
        return kafkaNamesMapper.toKafkaTopics(topic)
                .allMatch(kafkaTopic -> AdminUtils.topicExists(zkClients.get(kafkaClusterName), kafkaTopic.name().asString()) &&
                        !isMarkedForDeletion(kafkaClusterName, kafkaTopic));
    }

    private boolean isMarkedForDeletion(String kafkaClusterName, KafkaTopic kafkaTopic) {
        return zkClients.get(kafkaClusterName).pathExists(ZkUtils.getDeleteTopicPath(kafkaTopic.name().asString()));
    }
}
