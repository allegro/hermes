package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.ConsumerTimeoutException;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.UnwrappedMessageContent;
import pl.allegro.tech.hermes.common.time.Clock;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceivingTimeoutException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

public class KafkaMessageReceiver implements MessageReceiver {
    private final ConsumerConnector consumerConnector;
    private final MessageContentWrapper contentWrapper;
    private final Timer readingTimer;
    private final Clock clock;
    private final BlockingQueue<Message> readQueue;
    private final ExecutorService pool;
    private final KafkaNamesMapper kafkaNamesMapper;
    private boolean consuming = true;

    public KafkaMessageReceiver(Topic primaryTopic, Optional<Topic> secondaryTopic, ConsumerConnector consumerConnector, MessageContentWrapper contentWrapper,
                                Timer readingTimer, Clock clock, KafkaNamesMapper kafkaNamesMapper, Integer kafkaStreamCount) {
        this.consumerConnector = consumerConnector;
        this.contentWrapper = contentWrapper;
        this.readingTimer = readingTimer;
        this.clock = clock;
        this.kafkaNamesMapper = kafkaNamesMapper;

        ImmutableList.Builder<Topic> topicsBuilder = new ImmutableList.Builder<Topic>().add(primaryTopic);
        secondaryTopic.ifPresent(topicsBuilder::add);
        Collection<Topic> topics = topicsBuilder.build();

        Map<String, Integer> topicCountMap = topics.stream()
                .collect(Collectors.toMap((topic) -> kafkaTopic(topic).name(), (topic) -> kafkaStreamCount));
        Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumerConnector.createMessageStreams(topicCountMap);

        Map<Topic, ConsumerIterator<byte[],byte[]>> iterators = topics.stream()
                .collect(Collectors.toMap(Function.<Topic>identity(), (topic) -> iterator(consumerMap.get(kafkaTopic(topic).name()))));

        readQueue = new ArrayBlockingQueue<>(iterators.size());
        pool = Executors.newFixedThreadPool(iterators.size());

        iterators.forEach((topic, iterator) -> pool.submit(() -> {
                while (consuming) {
                    readQueue.offer(readMessage(topic, iterator));
                }
        }));
    }

    @Override
    public Message next() {
        try {
            return readQueue.take();
        } catch (InterruptedException ex) {
            throw new MessageReceivingTimeoutException("No messages received", ex);
        }
    }

    private KafkaTopic kafkaTopic(Topic topic) {
        return kafkaNamesMapper.toKafkaTopicName(topic);
    }

    private ConsumerIterator<byte[],byte[]> iterator(List<KafkaStream<byte[], byte[]>> streams) {
        return streams.get(0).iterator();
    }

    private Message readMessage(Topic topic, ConsumerIterator<byte[], byte[]> iterator) {
        try (Timer.Context readingTimerContext = readingTimer.time()) {
            MessageAndMetadata<byte[], byte[]> message = iterator.next();
            UnwrappedMessageContent unwrappedContent = contentWrapper.unwrap(message.message(), topic);

            return new Message(
                    unwrappedContent.getMessageMetadata().getId(),
                    topic.getQualifiedName(),
                    unwrappedContent.getContent(),
                    unwrappedContent.getMessageMetadata().getTimestamp(),
                    clock.getTime(),
                    new PartitionOffset(kafkaTopic(topic), message.offset(), message.partition()));

        } catch (ConsumerTimeoutException consumerTimeoutException) {
            throw new MessageReceivingTimeoutException("No messages received", consumerTimeoutException);
        } catch (Exception e) {
            throw new InternalProcessingException("Message received failed", e);
        }
    }

    @Override
    public void stop() {
        this.consuming = false;
        consumerConnector.shutdown();
    }

}
