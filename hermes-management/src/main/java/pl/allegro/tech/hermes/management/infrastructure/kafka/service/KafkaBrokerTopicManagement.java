package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import kafka.admin.AdminUtils;
import kafka.log.LogConfig;
import org.I0Itec.zkclient.ZkClient;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopics;
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

        kafkaNamesMapper.toKafkaTopics(topic).forEach(k ->
                        AdminUtils.createTopic(
                                client,
                                k.name().asString(),
                                topicProperties.getPartitions(),
                                topicProperties.getReplicationFactor(),
                                config
                        )
        );
    }

    @Override
    public void removeTopic(Topic topic) {
        kafkaNamesMapper.toKafkaTopics(topic).forEach(k -> AdminUtils.deleteTopic(client, k.name().asString()));
    }

    @Override
    public void updateTopic(Topic topic) {
        Properties config = createTopicConfig(topic.getRetentionTime().getDuration(), topicProperties);
        KafkaTopics kafkaTopics = kafkaNamesMapper.toKafkaTopics(topic);

        if (isMigrationToNewKafkaTopic(kafkaTopics)) {
            AdminUtils.createTopic(
                    client,
                    kafkaTopics.getPrimary().name().asString(),
                    topicProperties.getPartitions(),
                    topicProperties.getReplicationFactor(),
                    config
            );
        } else {
            AdminUtils.changeTopicConfig(client, kafkaTopics.getPrimary().name().asString(), config);
        }

        kafkaTopics.getSecondary().ifPresent(secondary ->
                        AdminUtils.changeTopicConfig(client, secondary.name().asString(), config)
        );
    }

    protected boolean isMigrationToNewKafkaTopic(KafkaTopics kafkaTopics) {
        return kafkaTopics.getSecondary().isPresent() &&
                !AdminUtils.topicExists(client, kafkaTopics.getPrimary().name().asString());
    }

    private Properties createTopicConfig(int retentionPolicy, TopicProperties topicProperties) {
        Properties props = new Properties();
        props.put(LogConfig.RententionMsProp(), String.valueOf(TimeUnit.DAYS.toMillis(retentionPolicy)));
        props.put(LogConfig.UncleanLeaderElectionEnableProp(), Boolean.toString(topicProperties.isUncleanLeaderElectionEnabled()));

        return props;
    }

}
