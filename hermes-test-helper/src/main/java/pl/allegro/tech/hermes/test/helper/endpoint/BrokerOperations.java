package pl.allegro.tech.hermes.test.helper.endpoint;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.JsonToAvroMigrationKafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toMap;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL;
import static org.apache.kafka.clients.CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

/**
 * Created to perform operations directly on broker excluding Hermes internal structures
 */
public class BrokerOperations {

    private static final int DEFAULT_PARTITIONS = 2;
    private static final int DEFAULT_REPLICATION_FACTOR = 1;

    private final Map<String, AdminClient> adminClients;
    private final KafkaNamesMapper kafkaNamesMapper;

    public BrokerOperations(Map<String, String> kafkaConnection, ConfigFactory configFactory) {
        adminClients = kafkaConnection.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> brokerAdminClient(e.getValue())));
        String namespace = configFactory.getStringProperty(Configs.KAFKA_NAMESPACE);
        String namespaceSeparator = configFactory.getStringProperty(Configs.KAFKA_NAMESPACE_SEPARATOR);
        kafkaNamesMapper = new JsonToAvroMigrationKafkaNamesMapper(namespace, namespaceSeparator);
    }

    public void createTopic(String topicName) {
        adminClients.values().forEach(c -> createTopic(topicName, c));
    }

    public void createTopic(String topicName, String brokerName) {
        createTopic(topicName, adminClients.get(brokerName));
    }

    private void createTopic(String topicName, AdminClient adminClient) {
        Topic topic = topic(topicName).build();
        kafkaNamesMapper.toKafkaTopics(topic)
                .forEach(kafkaTopic -> createTopic(adminClient, kafkaTopic.name().asString()));
    }

    private void createTopic(AdminClient adminClient, String topicName) {
        try {
            NewTopic topic = new NewTopic(topicName, DEFAULT_PARTITIONS, (short) DEFAULT_REPLICATION_FACTOR);
            adminClient.createTopics(singletonList(topic))
                    .all()
                    .get(1, MINUTES);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean topicExists(String topicName, String kafkaClusterName) {
        Topic topic = topic(topicName).build();
        return kafkaNamesMapper.toKafkaTopics(topic)
                .allMatch(kafkaTopic -> topicExists(kafkaClusterName, kafkaTopic));
    }

    private boolean topicExists(String kafkaClusterName, KafkaTopic kafkaTopic) {
        try {
            adminClients.get(kafkaClusterName)
                    .describeTopics(singletonList(kafkaTopic.name().asString()))
                    .all()
                    .get(1, MINUTES);
            return true;
        } catch (UnknownTopicOrPartitionException e) {
            return false;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof UnknownTopicOrPartitionException) {
                return false;
            }
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AdminClient brokerAdminClient(String bootstrapKafkaServer) {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapKafkaServer);
        props.put(SECURITY_PROTOCOL_CONFIG, DEFAULT_SECURITY_PROTOCOL);
        props.put(REQUEST_TIMEOUT_MS_CONFIG, 10000);
        return AdminClient.create(props);
    }
}
