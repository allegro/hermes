package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
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
        Map<String, String> config = createTopicConfig(topic.getRetentionTime().getDurationInMillis(), topicProperties);

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
        Map<String, String> config = createTopicConfig(topic.getRetentionTime().getDurationInMillis(), topicProperties);
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

    private Map<String, String> createTopicConfig(long retentionPolicy, TopicProperties topicProperties) {
        Map<String, String> props = new HashMap<>();
        props.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retentionPolicy));
        props.put(TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_CONFIG, Boolean.toString(topicProperties.isUncleanLeaderElectionEnabled()));
        props.put(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, String.valueOf(topicProperties.getMaxMessageSize()));

        return props;
    }
}
