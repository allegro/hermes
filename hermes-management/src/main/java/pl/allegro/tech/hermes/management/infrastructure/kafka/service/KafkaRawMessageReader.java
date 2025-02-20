package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import static java.lang.String.format;

import java.time.Duration;
import java.util.Collections;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.kafka.KafkaConsumerPool;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.management.domain.topic.SingleMessageReaderException;

public class KafkaRawMessageReader {

  private static final Logger logger = LoggerFactory.getLogger(KafkaRawMessageReader.class);

  private final KafkaConsumerPool consumerPool;
  private final int pollTimeoutMillis;

  public KafkaRawMessageReader(KafkaConsumerPool consumerPool, int pollTimeoutMillis) {
    this.consumerPool = consumerPool;
    this.pollTimeoutMillis = pollTimeoutMillis;
  }

  private static SingleMessageReaderException messageNotFoundException(
      KafkaTopic topic, int partition, long offset) {
    String cause = buildErrorMessage(topic, partition, offset, "Cannot find message");
    logger.error(cause);
    return new SingleMessageReaderException(cause);
  }

  private static SingleMessageReaderException pollingException(
      KafkaTopic topic, int partition, long offset, Throwable throwable) {
    String cause =
        buildErrorMessage(topic, partition, offset, "Error during polling kafka message");
    logger.error(cause, throwable);
    return new SingleMessageReaderException(cause, throwable);
  }

  private static String buildErrorMessage(
      KafkaTopic topic, int partition, long offset, String message) {
    return format(
        "%s [offset %d, kafka_topic %s, partition %d]",
        message, offset, topic.name().asString(), partition);
  }

  byte[] readMessage(KafkaTopic topic, int partition, long offset) {
    KafkaConsumer<byte[], byte[]> kafkaConsumer = consumerPool.get(topic, partition);
    TopicPartition topicPartition = new TopicPartition(topic.name().asString(), partition);

    try {
      kafkaConsumer.assign(Collections.singleton(topicPartition));
      kafkaConsumer.poll(Duration.ZERO);
      kafkaConsumer.seek(topicPartition, offset);
      ConsumerRecords<byte[], byte[]> records =
          kafkaConsumer.poll(Duration.ofMillis(pollTimeoutMillis));
      for (ConsumerRecord<byte[], byte[]> record : records.records(topicPartition)) {
        if (record.offset() == offset) {
          return record.value();
        }
        logger.info("Found an old offset: {} Expecting: {}", record.offset(), offset);
      }
      throw messageNotFoundException(topic, partition, offset);
    } catch (Exception e) {
      throw pollingException(topic, partition, offset, e);
    }
  }
}
