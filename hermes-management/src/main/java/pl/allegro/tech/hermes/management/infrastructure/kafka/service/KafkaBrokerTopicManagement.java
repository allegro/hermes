package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import kafka.admin.AdminUtils;
import kafka.log.LogConfig;
import org.I0Itec.zkclient.ZkClient;
import pl.allegro.tech.hermes.api.RetentionTime;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.config.TopicProperties;
import pl.allegro.tech.hermes.management.domain.topic.BrokerTopicManagement;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class KafkaBrokerTopicManagement implements BrokerTopicManagement {

    private final TopicProperties topicProperties;

    private final ZkClient client;

    public KafkaBrokerTopicManagement(TopicProperties topicProperties, ZkClient zkClient) {
        this.topicProperties = topicProperties;
        this.client = zkClient;
    }

    @Override
    public void createTopic(TopicName topicName, RetentionTime retentionTime) {
        Properties config = createTopicConfig(retentionTime.getDuration(), topicProperties);

        AdminUtils.createTopic(
            client,
            topicName.qualifiedName(),
            topicProperties.getPartitions(),
            topicProperties.getReplicationFactor(),
            config
        );
    }

    @Override
    public void removeTopic(TopicName name) {
        AdminUtils.deleteTopic(client, name.qualifiedName());
    }

    @Override
    public void updateTopic(TopicName topicName, RetentionTime retentionTime) {
        Properties config = createTopicConfig(retentionTime.getDuration(), topicProperties);
        AdminUtils.changeTopicConfig(client, topicName.qualifiedName(), config);
    }

    private Properties createTopicConfig(int retentionPolicy, TopicProperties topicProperties) {
        Properties props = new Properties();
        props.put(LogConfig.RententionMsProp(), "" + TimeUnit.DAYS.toMillis(retentionPolicy));
        props.put(LogConfig.UncleanLeaderElectionEnableProp(), Boolean.toString(topicProperties.isUncleanLeaderElectionEnabled()));

        return props;
    }

}
