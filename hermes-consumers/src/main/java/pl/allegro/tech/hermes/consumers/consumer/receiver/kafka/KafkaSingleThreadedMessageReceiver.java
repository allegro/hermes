package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import com.google.common.collect.ImmutableList;
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
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder;
import pl.allegro.tech.hermes.consumers.consumer.offset.ConsumerPartitionAssignmentState;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetCommitterConsumerRebalanceListener;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker.KafkaConsumerOffsetMover;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.RetryableReceiverError;
import pl.allegro.tech.hermes.metrics.HermesCounter;

public class KafkaSingleThreadedMessageReceiver implements MessageReceiver {
  private static final Logger logger =
      LoggerFactory.getLogger(KafkaSingleThreadedMessageReceiver.class);

  private final KafkaConsumer<byte[], byte[]> consumer;
  private final KafkaConsumerRecordToMessageConverter messageConverter;

  private final BlockingQueue<ConsumerRecord<byte[], byte[]>> readQueue;
  private final KafkaConsumerOffsetMover offsetMover;

  private final HermesCounter skippedCounter;
  private final HermesCounter failuresCounter;
  private final SubscriptionLoadRecorder loadReporter;
  private volatile Subscription subscription;

  private final Duration poolTimeout;
  private final ConsumerPartitionAssignmentState partitionAssignmentState;

  public KafkaSingleThreadedMessageReceiver(
      KafkaConsumer<byte[], byte[]> consumer,
      KafkaConsumerRecordToMessageConverterFactory messageConverterFactory,
      MetricsFacade metrics,
      KafkaNamesMapper kafkaNamesMapper,
      Topic topic,
      Subscription subscription,
      Duration poolTimeout,
      int readQueueCapacity,
      SubscriptionLoadRecorder loadReporter,
      ConsumerPartitionAssignmentState partitionAssignmentState) {
    this.skippedCounter = metrics.offsetCommits().skippedCounter();
    this.failuresCounter = metrics.offsetCommits().failuresCounter();
    this.subscription = subscription;
    this.poolTimeout = poolTimeout;
    this.loadReporter = loadReporter;
    this.partitionAssignmentState = partitionAssignmentState;
    this.consumer = consumer;
    this.readQueue = new ArrayBlockingQueue<>(readQueueCapacity);
    this.offsetMover = new KafkaConsumerOffsetMover(subscription.getQualifiedName(), consumer);
    Map<String, KafkaTopic> topics =
        getKafkaTopics(topic, kafkaNamesMapper).stream()
            .collect(Collectors.toMap(t -> t.name().asString(), Function.identity()));
    this.messageConverter = messageConverterFactory.create(topic, subscription, topics);
    this.consumer.subscribe(
        topics.keySet(),
        new OffsetCommitterConsumerRebalanceListener(
            subscription.getQualifiedName(), partitionAssignmentState));
  }

  private Collection<KafkaTopic> getKafkaTopics(Topic topic, KafkaNamesMapper kafkaNamesMapper) {
    KafkaTopics kafkaTopics = kafkaNamesMapper.toKafkaTopics(topic);
    ImmutableList.Builder<KafkaTopic> topicsBuilder =
        new ImmutableList.Builder<KafkaTopic>().add(kafkaTopics.getPrimary());
    kafkaTopics.getSecondary().ifPresent(topicsBuilder::add);
    return topicsBuilder.build();
  }

  @Override
  public Optional<Message> next() {
    try {
      supplyReadQueue();
      return getMessageFromReadQueue();
    } catch (InterruptException ex) {
      // Despite that Thread.currentThread().interrupt() is called in InterruptException's
      // constructor
      // Thread.currentThread().isInterrupted() somehow returns false so we reset it.
      logger.info("Kafka consumer thread interrupted", ex);
      Thread.currentThread().interrupt();
      return Optional.empty();
    } catch (KafkaException ex) {
      logger.error(
          "Error while reading message for subscription {}", subscription.getQualifiedName(), ex);
      return Optional.empty();
    } catch (Exception ex) {
      logger.error(
          "Failed to read message for subscription {}, readQueueSize {}",
          subscription.getQualifiedName(),
          readQueue.size(),
          ex);
      return Optional.empty();
    }
  }

  private void supplyReadQueue() {
    if (readQueue.isEmpty()) {
      ConsumerRecords<byte[], byte[]> records = consumer.poll(poolTimeout);
      try {
        for (ConsumerRecord<byte[], byte[]> record : records) {
          loadReporter.recordSingleOperation();
          readQueue.add(record);
        }
      } catch (Exception ex) {
        logger.error(
            "Failed to read message for subscription {}, readQueueSize {}, records {}",
            subscription.getQualifiedName(),
            readQueue.size(),
            records.count(),
            ex);
      }
    }
  }

  private Optional<Message> getMessageFromReadQueue() {
    if (!readQueue.isEmpty()) {
      ConsumerRecord<byte[], byte[]> record = readQueue.element();
      try {
        Message message = convertToMessage(record);
        readQueue.poll();
        return Optional.of(message);
      } catch (RetryableReceiverError ex) {
        logger.warn("Cannot convert record to message... Operation will be delayed", ex);
        return Optional.empty();
      }
    }
    return Optional.empty();
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
    } catch (InterruptException ex) {
      // means that the thread was interrupted
    } catch (KafkaException ex) {
      logger.warn("KafkaException occurred during closing consumer.", ex);
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
    } catch (InterruptException ex) {
      logger.info("Kafka consumer thread interrupted", ex);
      Thread.currentThread().interrupt();
    } catch (Exception ex) {
      logger.error(
          "Error while committing offset for subscription {}", subscription.getQualifiedName(), ex);
      failuresCounter.increment();
    }
  }

  private Map<TopicPartition, OffsetAndMetadata> createOffset(
      Set<SubscriptionPartitionOffset> partitionOffsets) {
    Map<TopicPartition, OffsetAndMetadata> offsetsData = new LinkedHashMap<>();
    for (SubscriptionPartitionOffset partitionOffset : partitionOffsets) {
      TopicPartition topicAndPartition =
          new TopicPartition(
              partitionOffset.getKafkaTopicName().asString(), partitionOffset.getPartition());

      if (partitionAssignmentState.isAssignedPartitionAtCurrentTerm(
          partitionOffset.getSubscriptionPartition())) {
        if (consumer.position(topicAndPartition) >= partitionOffset.getOffset()) {
          offsetsData.put(topicAndPartition, new OffsetAndMetadata(partitionOffset.getOffset()));
        } else {
          skippedCounter.increment();
        }
      } else {
        logger.warn(
            "Consumer is not assigned to partition {} of subscription {} at current term {},"
                + " ignoring offset {} from term {} to commit",
            partitionOffset.getPartition(),
            partitionOffset.getSubscriptionName(),
            partitionAssignmentState.currentTerm(partitionOffset.getSubscriptionName()),
            partitionOffset.getOffset(),
            partitionOffset.getPartitionAssignmentTerm());
      }
    }
    return offsetsData;
  }

  @Override
  public boolean moveOffset(PartitionOffset offset) {
    return offsetMover.move(offset);
  }
}
