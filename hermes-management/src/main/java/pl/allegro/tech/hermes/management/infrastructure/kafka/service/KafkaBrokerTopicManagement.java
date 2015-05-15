package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import kafka.admin.AdminUtils;
import org.I0Itec.zkclient.ZkClient;
import pl.allegro.tech.hermes.api.RetentionTime;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.config.TopicProperties;
import pl.allegro.tech.hermes.management.domain.topic.BrokerTopicManagement;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class KafkaBrokerTopicManagement implements BrokerTopicManagement {

    public static final String RETENTION_MS_PROPERTY = "retention.ms";

    private final TopicProperties topicProperties;

    private final ZkClient client;

    public KafkaBrokerTopicManagement(TopicProperties topicProperties, ZkClient zkClient) {
        this.topicProperties = topicProperties;
        this.client = zkClient;
    }

    @Override
    public void createTopic(TopicName topicName, RetentionTime retentionTime) {
        Properties props = new Properties();
        populateRetentionToProperties(retentionTime.getDuration(), props);

        AdminUtils.createTopic(
            client, topicName.qualifiedName(),
            topicProperties.getPartitions(),
            topicProperties.getReplicationFactor(),
            props
        );
    }

    @Override
    public void removeTopic(TopicName name) {
        AdminUtils.deleteTopic(client, name.qualifiedName());
    }

    @Override
    public void updateTopic(TopicName topicName, RetentionTime retentionTime) {
        Properties props = new Properties();
        populateRetentionToProperties(retentionTime.getDuration(), props);
        AdminUtils.changeTopicConfig(client, topicName.qualifiedName(), props);
    }

    private void populateRetentionToProperties(int retentionPolicy, Properties props) {
        props.put(RETENTION_MS_PROPERTY, "" + TimeUnit.DAYS.toMillis(retentionPolicy));
    }

}
