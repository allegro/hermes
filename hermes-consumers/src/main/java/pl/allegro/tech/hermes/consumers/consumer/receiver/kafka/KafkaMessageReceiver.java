package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableMap;
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

import java.util.List;
import java.util.Map;

public class KafkaMessageReceiver implements MessageReceiver {
    private final ConsumerIterator<byte[], byte[]> iterator;
    private final Topic topic;
    private final ConsumerConnector consumerConnector;
    private final MessageContentWrapper contentWrapper;
    private final Timer readingTimer;
    private final Clock clock;
    private final KafkaTopic kafkaTopic;

    public KafkaMessageReceiver(Topic topic, ConsumerConnector consumerConnector, MessageContentWrapper contentWrapper,
                                Timer readingTimer, Clock clock, KafkaNamesMapper kafkaNamesMapper, Integer kafkaStreamCount) {
        this.topic = topic;
        this.consumerConnector = consumerConnector;
        this.contentWrapper = contentWrapper;
        this.readingTimer = readingTimer;
        this.clock = clock;

        this.kafkaTopic = kafkaNamesMapper.toKafkaTopicName(topic);
        Map<String, Integer> topicCountMap = ImmutableMap.of(kafkaTopic.name(), kafkaStreamCount);
        Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumerConnector.createMessageStreams(
                topicCountMap
        );
        KafkaStream<byte[], byte[]> stream = consumerMap.get(kafkaTopic.name()).get(0);
        iterator = stream.iterator();
    }

    @Override
    public Message next() {
        try (Timer.Context readingTimerContext = readingTimer.time()) {
            MessageAndMetadata<byte[], byte[]> message = iterator.next();
            UnwrappedMessageContent unwrappedContent = contentWrapper.unwrap(message.message(), topic);

            return new Message(
                    unwrappedContent.getMessageMetadata().getId(),
                    topic.getQualifiedName(),
                    unwrappedContent.getContent(),
                    unwrappedContent.getMessageMetadata().getTimestamp(),
                    clock.getTime(),
                    new PartitionOffset(kafkaTopic, message.offset(), message.partition()));

        } catch (ConsumerTimeoutException consumerTimeoutException) {
            throw new MessageReceivingTimeoutException("No messages received", consumerTimeoutException);
        } catch (Exception e) {
            throw new InternalProcessingException("Message received failed", e);
        }
    }

    @Override
    public void stop() {
        consumerConnector.shutdown();
    }

}
