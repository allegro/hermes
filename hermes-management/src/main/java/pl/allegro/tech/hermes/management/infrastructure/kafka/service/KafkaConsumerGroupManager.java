package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.management.domain.subscription.ConsumerGroupManager;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG;

public class KafkaConsumerGroupManager implements ConsumerGroupManager {

    private final Logger logger = LoggerFactory.getLogger(KafkaConsumerGroupManager.class);

    private final KafkaNamesMapper kafkaNamesMapper;
    private final String clusterName;
    private final String bootstrapKafkaServer;

    public KafkaConsumerGroupManager(KafkaNamesMapper kafkaNamesMapper,
                                     String clusterName,
                                     String bootstrapKafkaServer) {
        this.kafkaNamesMapper = kafkaNamesMapper;
        this.clusterName = clusterName;
        this.bootstrapKafkaServer = bootstrapKafkaServer;
    }

    @Override
    public void createConsumerGroup(Subscription subscription) {
        logger.info("Creating consumer group for subscription {}, cluster: {}", subscription.getQualifiedName(), clusterName);

        ConsumerGroupId groupId = kafkaNamesMapper.toConsumerGroupId(subscription.getQualifiedName());
        KafkaConsumer<byte[], byte[]> kafkaConsumer = new KafkaConsumer<>(properties(groupId));

        try {
            Set<TopicPartition> topicPartitions = kafkaConsumer.partitionsFor(subscription.getQualifiedTopicName()).stream()
                    .map(info -> new TopicPartition(info.topic(), info.partition()))
                    .collect(toSet());

            logger.info("Received partitions: {}, cluster: {}", topicPartitions, clusterName);

            kafkaConsumer.assign(topicPartitions);

            Map<TopicPartition, OffsetAndMetadata> topicPartitionByOffset = topicPartitions.stream()
                    .map(topicPartition -> {
                        long offset = kafkaConsumer.position(topicPartition);
                        return ImmutablePair.of(topicPartition, new OffsetAndMetadata(offset));
                    })
                    .collect(toMap(Pair::getKey, Pair::getValue));

            kafkaConsumer.commitSync(topicPartitionByOffset);
            kafkaConsumer.close();

            logger.info("Successfully created consumer groups for subscription {}, cluster: {}", subscription.getQualifiedName(), clusterName);
        } catch (Exception e) {
            logger.error("Failed to create consumer groups for subscription {}, cluster: {}", subscription.getQualifiedName(), clusterName, e);
        }
    }

    private Properties properties(ConsumerGroupId groupId) {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapKafkaServer);
        props.put(GROUP_ID_CONFIG, groupId.asString());
        props.put(ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(REQUEST_TIMEOUT_MS_CONFIG, 1000);
        props.put("default.api.timeout.ms", 1000);
        props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        return props;
    }
}
