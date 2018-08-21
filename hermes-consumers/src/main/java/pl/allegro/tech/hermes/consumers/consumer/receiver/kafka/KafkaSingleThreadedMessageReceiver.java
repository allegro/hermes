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
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopics;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.UnsupportedContentTypeException;
import pl.allegro.tech.hermes.common.message.wrapper.UnwrappedMessageContent;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker.PartitionAssigningAwareRetransmitter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;

import java.time.Clock;
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

    private KafkaConsumer<byte[], byte[]> consumer;
    private final MessageContentWrapper messageContentWrapper;
    private final Clock clock;

    private final BlockingQueue<Message> readQueue;
    private final PartitionAssigningAwareRetransmitter retransmitter;

    private final HermesMetrics metrics;
    private Topic topic;
    private volatile Subscription subscription;

    private Map<String, KafkaTopic> topics;

    private final int pollTimeout;

    public KafkaSingleThreadedMessageReceiver(KafkaConsumer<byte[], byte[]> consumer,
                                              MessageContentWrapper messageContentWrapper,
                                              HermesMetrics metrics,
                                              KafkaNamesMapper kafkaNamesMapper,
                                              Topic topic,
                                              Subscription subscription,
                                              Clock clock,
                                              int pollTimeout,
                                              int readQueueCapacity,
                                              int retransmissionQueueCapacity) {
        this.metrics = metrics;
        this.topic = topic;
        this.subscription = subscription;
        this.pollTimeout = pollTimeout;
        this.topics = getKafkaTopics(topic, kafkaNamesMapper).stream()
                .collect(Collectors.toMap(t -> t.name().asString(), Function.identity()));
        this.consumer = consumer;
        this.messageContentWrapper = messageContentWrapper;
        this.clock = clock;
        this.readQueue = new ArrayBlockingQueue<>(readQueueCapacity);
        this.retransmitter = new PartitionAssigningAwareRetransmitter(
                subscription.getQualifiedName(),
                retransmissionQueueCapacity,
                consumer);
        this.consumer.subscribe(topics.keySet(), retransmitter);
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
                ConsumerRecords<byte[], byte[]> records = consumer.poll(pollTimeout);
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
        KafkaTopic kafkaTopic = topics.get(record.topic());
        UnwrappedMessageContent unwrappedContent = getUnwrappedMessageContent(record, kafkaTopic.contentType());
        return new Message(
                unwrappedContent.getMessageMetadata().getId(),
                topic.getQualifiedName(),
                unwrappedContent.getContent(),
                kafkaTopic.contentType(),
                unwrappedContent.getSchema(),
                unwrappedContent.getMessageMetadata().getTimestamp(),
                clock.millis(),
                new PartitionOffset(kafkaTopic.name(), record.offset(), record.partition()),
                unwrappedContent.getMessageMetadata().getExternalMetadata(),
                subscription.getHeaders()
        );
    }

    private UnwrappedMessageContent getUnwrappedMessageContent(ConsumerRecord<byte[], byte[]> message,
                                                               ContentType contentType) {
        if (contentType == ContentType.AVRO) {
            return messageContentWrapper.unwrapAvro(message.value(), topic);
        } else if (contentType == ContentType.JSON) {
            return messageContentWrapper.unwrapJson(message.value());
        }
        throw new UnsupportedContentTypeException(topic);
    }

    @Override
    public void stop() {
        try {
            consumer.close();
        } catch (IllegalStateException ex) {
            // means it was already closed
        }
    }

    @Override
    public void update(Subscription newSubscription) {
        this.subscription = newSubscription;
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

            if (consumer.position(topicAndPartition) >= partitionOffset.getOffset()) {
                offsetsData.put(topicAndPartition, new OffsetAndMetadata(partitionOffset.getOffset()));
            }
        }
        return offsetsData;
    }

    @Override
    public void moveOffset(SubscriptionPartitionOffset offset) {
        retransmitter.moveOffsetOrSchedule(offset);
    }
}
