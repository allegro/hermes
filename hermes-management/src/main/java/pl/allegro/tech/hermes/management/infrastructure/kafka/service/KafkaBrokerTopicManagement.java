package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import kafka.admin.AdminUtils;
import kafka.log.LogConfig;
import org.I0Itec.zkclient.ZkClient;
import pl.allegro.tech.hermes.api.RetentionTime;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.management.config.TopicProperties;
import pl.allegro.tech.hermes.management.domain.topic.BrokerTopicManagement;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class KafkaBrokerTopicManagement implements BrokerTopicManagement {

    private final TopicProperties topicProperties;

    private final ZkClient client;

    private final KafkaNamesMapper kafkaNamesMapper;

    public KafkaBrokerTopicManagement(TopicProperties topicProperties, ZkClient zkClient, KafkaNamesMapper kafkaNamesMapper) {
        this.topicProperties = topicProperties;
        this.client = zkClient;
        this.kafkaNamesMapper = kafkaNamesMapper;
    }

    @Override
    public void createTopic(TopicName topicName, RetentionTime retentionTime) {
        Properties config = createTopicConfig(retentionTime.getDuration(), topicProperties);

        AdminUtils.createTopic(
            client,
            kafkaNamesMapper.toKafkaTopicName(topicName).asString(),
            topicProperties.getPartitions(),
            topicProperties.getReplicationFactor(),
            config
        );
    }

    @Override
    public void removeTopic(TopicName name) {
        AdminUtils.deleteTopic(client, kafkaNamesMapper.toKafkaTopicName(name).asString());
    }

    @Override
    public void updateTopic(TopicName topicName, RetentionTime retentionTime) {
        Properties config = createTopicConfig(retentionTime.getDuration(), topicProperties);
        AdminUtils.changeTopicConfig(client, kafkaNamesMapper.toKafkaTopicName(topicName).asString(), config);
    }

    private Properties createTopicConfig(int retentionPolicy, TopicProperties topicProperties) {
        Properties props = new Properties();
        props.put(LogConfig.RententionMsProp(), String.valueOf(TimeUnit.DAYS.toMillis(retentionPolicy)));
        props.put(LogConfig.UncleanLeaderElectionEnableProp(), Boolean.toString(topicProperties.isUncleanLeaderElectionEnabled()));

        return props;
    }

}
