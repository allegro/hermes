package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import com.google.common.collect.ImmutableList;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.InterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopics;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.offset.ConsumerPartitionAssignmentState;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetCommitterConsumerRebalanceListener;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker.KafkaConsumerOffsetMover;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

public class KafkaSingleThreadedMessageReceiver implements MessageReceiver {
    private static final Logger logger = LoggerFactory.getLogger(KafkaSingleThreadedMessageReceiver.class);

    private final KafkaConsumer<byte[], byte[]> consumer;
    private final KafkaMessageConverter messageConverter;

    private final BlockingQueue<Message> readQueue;
    private final KafkaConsumerOffsetMover offsetMover;

    private final HermesMetrics metrics;
    private volatile Subscription subscription;

    private final int pollTimeout;
    private final ConsumerPartitionAssignmentState partitionAssignmentState;

    public KafkaSingleThreadedMessageReceiver(KafkaConsumer<byte[], byte[]> consumer,
                                              KafkaMessageConverterFactory messageConverterFactory,
                                              HermesMetrics metrics,
                                              KafkaNamesMapper kafkaNamesMapper,
                                              Topic topic,
                                              Subscription subscription,
                                              int pollTimeout,
                                              int readQueueCapacity,
                                              ConsumerPartitionAssignmentState partitionAssignmentState) {
        this.metrics = metrics;
        this.subscription = subscription;
        this.pollTimeout = pollTimeout;
        this.partitionAssignmentState = partitionAssignmentState;
        this.consumer = consumer;
        this.readQueue = new ArrayBlockingQueue<>(readQueueCapacity);
        this.offsetMover = new KafkaConsumerOffsetMover(subscription.getQualifiedName(), consumer);
        Map<String, KafkaTopic> topics = getKafkaTopics(topic, kafkaNamesMapper).stream()
                .collect(Collectors.toMap(t -> t.name().asString(), Function.identity()));
        this.messageConverter = messageConverterFactory.create(topic, subscription, topics);
        this.consumer.subscribe(topics.keySet(),
                new OffsetCommitterConsumerRebalanceListener(subscription.getQualifiedName(), partitionAssignmentState));
    }

    private Collection<KafkaTopic> getKafkaTopics(Topic topic, KafkaNamesMapper kafkaNamesMapper) {
        KafkaTopics kafkaTopics = kafkaNamesMapper.toKafkaTopics(topic);
        ImmutableList.Builder<KafkaTopic> topicsBuilder = new ImmutableList.Builder<KafkaTopic>().add(kafkaTopics.getPrimary());
        kafkaTopics.getSecondary().ifPresent(topicsBuilder::add);
        return topicsBuilder.build();
    }

    @Override
    public Optional<Message> next() {
        try {
            if (readQueue.isEmpty()) {
                ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofMillis(pollTimeout));
                try {
                    for (ConsumerRecord<byte[], byte[]> record : records) {
                        readQueue.add(convertToMessage(record));
                    }
                } catch (Exception ex) {
                    logger.error("Failed to read message for subscription {}, readQueueSize {}, records {}",
                            subscription.getQualifiedName(),
                            readQueue.size(),
                            records.count(),
                            ex);
                }
            }
            return Optional.ofNullable(readQueue.poll());
        } catch (InterruptException ex ) {
            // Despite that Thread.currentThread().interrupt() is called in InterruptException's constructor
            // Thread.currentThread().isInterrupted() somehow returns false so we reset it.
            logger.info("Kafka consumer thread interrupted", ex);
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (KafkaException ex) {
            logger.error("Error while reading message for subscription {}", subscription.getQualifiedName(), ex);
            return Optional.empty();
        } catch (Exception ex) {
            logger.error("Failed to read message for subscription {}, readQueueSize {}",
                    subscription.getQualifiedName(),
                    readQueue.size(),
                    ex);
            return Optional.empty();
        }
    }

    private Message convertToMessage(ConsumerRecord<byte[], byte[]> record) {
        long currentTerm = partitionAssignmentState.currentTerm(subscription.getQualifiedName());
        return messageConverter.convertToMessage(record, currentTerm);
    }

    @Override
    public void stop() {
        try {
            consumer.close();
        } catch (IllegalStateException ex) {
            // means it was already closed
        } finally {
            partitionAssignmentState.revokeAll(subscription.getQualifiedName());
        }
    }

    @Override
    public void update(Subscription newSubscription) {
        this.subscription = newSubscription;
        messageConverter.update(subscription);
    }

    @Override
    public void commit(Set<SubscriptionPartitionOffset> offsets) {
        try {
            consumer.commitSync(createOffset(offsets));
        } catch (InterruptException ex ) {
            logger.info("Kafka consumer thread interrupted", ex);
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            logger.error("Error while committing offset for subscription {}", subscription.getQualifiedName(), ex);
            metrics.counter("offset-committer.failed").inc();
        }
    }

    private Map<TopicPartition, OffsetAndMetadata> createOffset(Set<SubscriptionPartitionOffset> partitionOffsets) {
        Map<TopicPartition, OffsetAndMetadata> offsetsData = new LinkedHashMap<>();
        for (SubscriptionPartitionOffset partitionOffset : partitionOffsets) {
            TopicPartition topicAndPartition = new TopicPartition(
                    partitionOffset.getKafkaTopicName().asString(),
                    partitionOffset.getPartition());

            if (partitionAssignmentState.isAssignedPartitionAtCurrentTerm(partitionOffset.getSubscriptionPartition())) {
                if (consumer.position(topicAndPartition) >= partitionOffset.getOffset()) {
                    offsetsData.put(topicAndPartition, new OffsetAndMetadata(partitionOffset.getOffset()));
                } else {
                    metrics.counter("offset-committer.skipped").inc();
                }
            } else {
                logger.warn("Consumer is not assigned to partition {} of subscription {} at current term {}, ignoring offset {} from term {} to commit",
                        partitionOffset.getPartition(), partitionOffset.getSubscriptionName(),
                        partitionAssignmentState.currentTerm(partitionOffset.getSubscriptionName()),
                        partitionOffset.getOffset(), partitionOffset.getPartitionAssignmentTerm());
            }
        }
        return offsetsData;
    }

    @Override
    public boolean moveOffset(PartitionOffset offset) {
        return offsetMover.move(offset);
    }
}
