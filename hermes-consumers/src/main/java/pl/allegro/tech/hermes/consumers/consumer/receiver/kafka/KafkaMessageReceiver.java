package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.ConsumerTimeoutException;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopics;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.UnwrappedMessageContent;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceivingTimeoutException;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class KafkaMessageReceiver implements MessageReceiver {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageReceiver.class);

    private final ConsumerConnector consumerConnector;
    private final MessageContentWrapper messageContentWrapper;
    private final Timer readingTimer;
    private final Clock clock;
    private final BlockingQueue<Message> readQueue;
    private final ExecutorService pool;
    private final Integer readTimeout;
    private final Topic topic;
    private volatile boolean consuming = true;

    public KafkaMessageReceiver(Topic topic, ConsumerConnector consumerConnector, MessageContentWrapper messageContentWrapper,
                                Timer readingTimer, Clock clock, KafkaNamesMapper kafkaNamesMapper,
                                Integer kafkaStreamCount, Integer readTimeout, SubscriptionName subscriptionName) {
        this.topic = topic;
        this.consumerConnector = consumerConnector;
        this.messageContentWrapper = messageContentWrapper;
        this.readingTimer = readingTimer;
        this.clock = clock;
        this.readTimeout = readTimeout;

        Collection<KafkaTopic> topics = getKafkaTopics(topic, kafkaNamesMapper);

        Map<String, Integer> topicCountMap = topics.stream()
                .collect(Collectors.toMap((kafkaTopic) -> kafkaTopic.name().asString(), (kafkaTopic) -> kafkaStreamCount));
        Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumerConnector.createMessageStreams(topicCountMap);

        Map<KafkaTopic, ConsumerIterator<byte[],byte[]>> iterators = topics.stream()
                .collect(Collectors.toMap(Function.<KafkaTopic>identity(), (kafkaTopic) -> iterator(consumerMap.get(kafkaTopic.name().asString()))));

        readQueue = new ArrayBlockingQueue<>(iterators.size());
        pool = Executors.newFixedThreadPool(iterators.size());

        iterators.forEach((kafkaTopic, iterator) -> pool.submit(() -> {
                Thread.currentThread().setName("Kafka-message-receiver-" + kafkaTopic.contentType() + "-" + subscriptionName);
                while (consuming) {
                    try {
                        readQueue.put(readMessage(kafkaTopic, iterator));
                    } catch (InterruptedException | MessageReceivingTimeoutException ignored) {
                        // intentional ignore of exception
                    } catch (Throwable throwable) {
                        logger.error("Error while reading message", throwable);
                    }
                }
        }));
    }

    private Collection<KafkaTopic> getKafkaTopics(Topic topic, KafkaNamesMapper kafkaNamesMapper) {
        KafkaTopics kafkaTopics = kafkaNamesMapper.toKafkaTopics(topic);

        ImmutableList.Builder<KafkaTopic> topicsBuilder = new ImmutableList.Builder<KafkaTopic>().add(kafkaTopics.getPrimary());
        kafkaTopics.getSecondary().ifPresent(topicsBuilder::add);
        return topicsBuilder.build();
    }

    @Override
    public Message next() {
        try {
            Message message = readQueue.poll(readTimeout, TimeUnit.MILLISECONDS);
            if (message == null) {
                throw new MessageReceivingTimeoutException("No messages received");
            } else {
                return message;
            }
        } catch (InterruptedException ex) {
            throw new MessageReceivingTimeoutException("No messages received", ex);
        }
    }

    private ConsumerIterator<byte[],byte[]> iterator(List<KafkaStream<byte[], byte[]>> streams) {
        return streams.get(0).iterator();
    }

    private Message readMessage(KafkaTopic kafkaTopic, ConsumerIterator<byte[], byte[]> iterator) {
        MessageAndMetadata<byte[], byte[]> message = null;
        try (Timer.Context readingTimerContext = readingTimer.time()) {
            message = iterator.next();
            UnwrappedMessageContent unwrappedContent = messageContentWrapper.unwrap(message.message(), topic, kafkaTopic.contentType());

            return new Message(
                    unwrappedContent.getMessageMetadata().getId(),
                    topic.getQualifiedName(),
                    unwrappedContent.getContent(),
                    kafkaTopic.contentType(),
                    unwrappedContent.getMessageMetadata().getTimestamp(),
                    clock.millis(),
                    new PartitionOffset(kafkaTopic.name(), message.offset(), message.partition()),
                    unwrappedContent.getMessageMetadata().getExternalMetadata()
            );

        } catch (ConsumerTimeoutException consumerTimeoutException) {
            throw new MessageReceivingTimeoutException("No messages received", consumerTimeoutException);
        } catch (Exception e) {
            if (message != null) {
                logger.error("Error while receiving message. Last read message: %s Partition: %d Offset: %d",
                    new String(message.message()), message.partition(), message.offset(), e);
            }
            throw new InternalProcessingException("Message received failed", e);
        }
    }

    @Override
    public void stop() {
        this.consuming = false;
        try {
            consumerConnector.shutdown();
            pool.shutdown();
        } catch (Throwable throwable) {
            logger.error("Error while shutting down", throwable);
        }
    }

}
