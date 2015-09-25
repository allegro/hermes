package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import kafka.admin.AdminUtils;
import kafka.log.LogConfig;
import org.I0Itec.zkclient.ZkClient;
import pl.allegro.tech.hermes.api.Topic;
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
    public void createTopic(Topic topic) {
        Properties config = createTopicConfig(topic.getRetentionTime().getDuration(), topicProperties);

        AdminUtils.createTopic(
            client,
            kafkaNamesMapper.toKafkaTopicName(topic).name(),
            topicProperties.getPartitions(),
            topicProperties.getReplicationFactor(),
            config
        );
    }

    @Override
    public void removeTopic(Topic topic) {
        AdminUtils.deleteTopic(client, kafkaNamesMapper.toKafkaTopicName(topic).name());
    }

    @Override
    public void updateTopic(Topic topic) {
        Properties config = createTopicConfig(topic.getRetentionTime().getDuration(), topicProperties);
        AdminUtils.changeTopicConfig(client, kafkaNamesMapper.toKafkaTopicName(topic).name(), config);
    }

    private Properties createTopicConfig(int retentionPolicy, TopicProperties topicProperties) {
        Properties props = new Properties();
        props.put(LogConfig.RententionMsProp(), String.valueOf(TimeUnit.DAYS.toMillis(retentionPolicy)));
        props.put(LogConfig.UncleanLeaderElectionEnableProp(), Boolean.toString(topicProperties.isUncleanLeaderElectionEnabled()));

        return props;
    }

}
