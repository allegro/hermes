package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import kafka.log.LogConfig;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.ConfigResource;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopics;
import pl.allegro.tech.hermes.management.config.TopicProperties;
import pl.allegro.tech.hermes.management.domain.topic.BrokerTopicManagement;
import pl.allegro.tech.hermes.management.infrastructure.kafka.BrokersClusterCommunicationException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class KafkaBrokerTopicManagement implements BrokerTopicManagement {

    private final TopicProperties topicProperties;

    private final AdminClient kafkaAdminClient;

    private final KafkaNamesMapper kafkaNamesMapper;

    public KafkaBrokerTopicManagement(TopicProperties topicProperties, AdminClient kafkaAdminClient, KafkaNamesMapper kafkaNamesMapper) {
        this.topicProperties = topicProperties;
        this.kafkaAdminClient = kafkaAdminClient;
        this.kafkaNamesMapper = kafkaNamesMapper;
    }

    @Override
    public void createTopic(Topic topic) {
        Map<String, String> config = createTopicConfig(topic.getRetentionTime().getDuration(), topicProperties);

        kafkaNamesMapper.toKafkaTopics(topic).forEach(k ->
                kafkaAdminClient.createTopics(Collections.singletonList(
                        new NewTopic(
                                k.name().asString(),
                                topicProperties.getPartitions(),
                                (short) topicProperties.getReplicationFactor()
                        ).configs(config)
                ))
        );
    }

    @Override
    public void removeTopic(Topic topic) {
        kafkaNamesMapper.toKafkaTopics(topic).forEach(k -> kafkaAdminClient.deleteTopics(Collections.singletonList(k.name().asString())));
    }

    @Override
    public void updateTopic(Topic topic) {
        Map<String, String> config = createTopicConfig(topic.getRetentionTime().getDuration(), topicProperties);
        KafkaTopics kafkaTopics = kafkaNamesMapper.toKafkaTopics(topic);

        if (isMigrationToNewKafkaTopic(kafkaTopics)) {
            kafkaAdminClient.createTopics(Collections.singletonList(
                    new NewTopic(
                            kafkaTopics.getPrimary().name().asString(),
                            topicProperties.getPartitions(),
                            (short) topicProperties.getReplicationFactor()
                    ).configs(config)
            ));
        } else {
            doUpdateTopic(kafkaTopics.getPrimary(), config);
        }

        kafkaTopics.getSecondary().ifPresent(secondary ->
                doUpdateTopic(secondary, config)
        );
    }

    @Override
    public boolean topicExists(Topic topic) {
        return kafkaNamesMapper.toKafkaTopics(topic)
                .allMatch(this::doesTopicExist);
    }

    private boolean isMigrationToNewKafkaTopic(KafkaTopics kafkaTopics) {
        return kafkaTopics.getSecondary().isPresent() &&
                !doesTopicExist(kafkaTopics.getPrimary());
    }

    private boolean doesTopicExist(KafkaTopic topic) {
        try {
            return kafkaAdminClient.listTopics().names()
                    .thenApply(names -> names.contains(topic.name().asString()))
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BrokersClusterCommunicationException(e);
        }
    }

    private void doUpdateTopic(KafkaTopic topic, Map<String, String> configMap) {
        ConfigResource topicConfigResource = new ConfigResource(
                ConfigResource.Type.TOPIC,
                topic.name().asString()
        );

        Collection<ConfigEntry> configEntries = configMap.entrySet().stream().map(entry ->
                new ConfigEntry(entry.getKey(), entry.getValue())
        ).collect(Collectors.toList());

        Map<ConfigResource, Config> configUpdates = new HashMap<>();
        configUpdates.put(topicConfigResource, new Config(configEntries));

        kafkaAdminClient.alterConfigs(configUpdates);
    }

    private Map<String, String> createTopicConfig(int retentionPolicy, TopicProperties topicProperties) {
        Map<String, String> props = new HashMap<>();
        props.put(LogConfig.RetentionMsProp(), String.valueOf(TimeUnit.DAYS.toMillis(retentionPolicy)));
        props.put(LogConfig.UncleanLeaderElectionEnableProp(), Boolean.toString(topicProperties.isUncleanLeaderElectionEnabled()));
        props.put(LogConfig.MaxMessageBytesProp(), String.valueOf(topicProperties.getMaxMessageSize()));

        return props;
    }
}
