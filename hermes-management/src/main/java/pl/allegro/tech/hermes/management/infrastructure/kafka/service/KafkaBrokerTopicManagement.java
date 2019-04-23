package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import kafka.log.LogConfig;
import kafka.zk.AdminZkClient;
import kafka.zk.KafkaZkClient;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopics;
import pl.allegro.tech.hermes.management.config.TopicProperties;
import pl.allegro.tech.hermes.management.domain.topic.BrokerTopicManagement;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class KafkaBrokerTopicManagement implements BrokerTopicManagement {

    private final TopicProperties topicProperties;

    private final AdminZkClient adminZkClient;

    private final KafkaZkClient kafkaZkClient;

    private final KafkaNamesMapper kafkaNamesMapper;

    public KafkaBrokerTopicManagement(TopicProperties topicProperties, AdminZkClient adminZkClient,
                                      KafkaZkClient kafkaZkClient, KafkaNamesMapper kafkaNamesMapper) {
        this.topicProperties = topicProperties;
        this.adminZkClient = adminZkClient;
        this.kafkaZkClient = kafkaZkClient;
        this.kafkaNamesMapper = kafkaNamesMapper;
    }

    @Override
    public void createTopic(Topic topic) {
        Properties config = createTopicConfig(topic.getRetentionTime().getDuration(), topicProperties);

        kafkaNamesMapper.toKafkaTopics(topic).forEach(k ->
                adminZkClient.createTopic(
                        k.name().asString(),
                        topicProperties.getPartitions(),
                        topicProperties.getReplicationFactor(),
                        config,
                        kafka.admin.RackAwareMode.Enforced$.MODULE$
                )
        );
    }

    @Override
    public void removeTopic(Topic topic) {
        kafkaNamesMapper.toKafkaTopics(topic).forEach(k -> adminZkClient.deleteTopic(k.name().asString()));
    }

    @Override
    public void updateTopic(Topic topic) {
        Properties config = createTopicConfig(topic.getRetentionTime().getDuration(), topicProperties);
        KafkaTopics kafkaTopics = kafkaNamesMapper.toKafkaTopics(topic);

        if (isMigrationToNewKafkaTopic(kafkaTopics)) {
            adminZkClient.createTopic(
                    kafkaTopics.getPrimary().name().asString(),
                    topicProperties.getPartitions(),
                    topicProperties.getReplicationFactor(),
                    config,
                    kafka.admin.RackAwareMode.Enforced$.MODULE$
            );
        } else {
            adminZkClient.changeTopicConfig(kafkaTopics.getPrimary().name().asString(), config);
        }

        kafkaTopics.getSecondary().ifPresent(secondary ->
                adminZkClient.changeTopicConfig(secondary.name().asString(), config)
        );
    }

    @Override
    public boolean topicExists(Topic topic) {
        return kafkaNamesMapper.toKafkaTopics(topic)
                .allMatch(kafkaTopic -> kafkaZkClient.topicExists(kafkaTopic.name().asString()));
    }

    private boolean isMigrationToNewKafkaTopic(KafkaTopics kafkaTopics) {
        return kafkaTopics.getSecondary().isPresent() &&
                !kafkaZkClient.topicExists(kafkaTopics.getPrimary().name().asString());
    }

    private Properties createTopicConfig(int retentionPolicy, TopicProperties topicProperties) {
        Properties props = new Properties();
        props.put(LogConfig.RetentionMsProp(), String.valueOf(TimeUnit.DAYS.toMillis(retentionPolicy)));
        props.put(LogConfig.UncleanLeaderElectionEnableProp(), Boolean.toString(topicProperties.isUncleanLeaderElectionEnabled()));
        props.put(LogConfig.MaxMessageBytesProp(), String.valueOf(topicProperties.getMaxMessageSize()));

        return props;
    }

}
