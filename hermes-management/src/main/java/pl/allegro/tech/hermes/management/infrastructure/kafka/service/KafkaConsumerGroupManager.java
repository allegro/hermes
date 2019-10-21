package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import kafka.common.TopicAndPartition;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.broker.BrokerDetails;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.management.domain.subscription.ConsumerGroupManager;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;

public class KafkaConsumerGroupManager implements ConsumerGroupManager {

    private final Logger logger = LoggerFactory.getLogger(KafkaConsumerGroupManager.class);

    private final BrokerStorage brokerStorage;
    private final KafkaNamesMapper kafkaNamesMapper;
    private final Integer partitions;
    private final String clusterName;
    private final ExecutorService executor;

    public KafkaConsumerGroupManager(BrokerStorage brokerStorage,
                                     KafkaNamesMapper kafkaNamesMapper,
                                     Integer partitions,
                                     String clusterName) {
        this.brokerStorage = brokerStorage;
        this.kafkaNamesMapper = kafkaNamesMapper;
        this.partitions = partitions;
        this.clusterName = clusterName;
        this.executor = Executors.newFixedThreadPool(
                partitions, new ThreadFactoryBuilder().setNameFormat("kafka-consumer-group-manager-%d").build()
        );
    }

    @Override
    public void createConsumerGroup(Subscription subscription) {
        IntStream.range(0, partitions).forEach(partition ->
            CompletableFuture
                    .runAsync(new CreateConsumerGroupTask(subscription, partition), executor)
                    .whenComplete((done, e) -> {
                        if (e == null) {
                            logger.info("Successfully created consumer group for subscription {} on partition {}, cluster: {}",
                                    subscription.getQualifiedName(), partition, clusterName);
                        } else {
                            logger.error("Failed to create consumer group for subscription {} on partition {}, cluster: {}",
                                    subscription.getQualifiedName(), partition, clusterName);
                        }
                    })
        );
    }

    class CreateConsumerGroupTask implements Runnable {

        private final Logger logger = LoggerFactory.getLogger(CreateConsumerGroupTask.class);

        private final Subscription subscription;
        private final int partition;
        private final TopicPartition topicPartition;

        CreateConsumerGroupTask(Subscription subscription, int partition) {
            this.subscription = subscription;
            this.partition = partition;
            this.topicPartition = new TopicPartition(subscription.getQualifiedTopicName(), partition);
        }

        @Override
        public void run() {
            logger.info("Creating consumer group for subscription {} on partition {}", subscription.getQualifiedName(), partition);

            int leaderId = brokerStorage.readLeaderForPartition(new TopicAndPartition(topicPartition));
            ConsumerGroupId groupId = kafkaNamesMapper.toConsumerGroupId(subscription.getQualifiedName());
            KafkaConsumer<byte[], byte[]> kafkaConsumer = createKafkaConsumer(groupId, leaderId);

            kafkaConsumer.assign(ImmutableSet.of(topicPartition));
            long offset = kafkaConsumer.position(topicPartition);
            kafkaConsumer.commitSync(ImmutableMap.of(topicPartition, new OffsetAndMetadata(offset)));
            kafkaConsumer.close();
        }

        private KafkaConsumer<byte[], byte[]> createKafkaConsumer(ConsumerGroupId groupId, int leaderId) {
            BrokerDetails brokerDetails = brokerStorage.readBrokerDetails(leaderId);
            String bootstrapServers = brokerDetails.getHost() + ":" + brokerDetails.getPort();
            return new KafkaConsumer<>(properties(groupId, bootstrapServers));
        }

        private Properties properties(ConsumerGroupId groupId, String bootstrapServers) {
            Properties props = new Properties();
            props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(GROUP_ID_CONFIG, groupId.asString());
            props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
            props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
            return props;
        }
    }
}
