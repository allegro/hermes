package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableMap;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.ConsumerTimeoutException;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.UnwrappedMessageContent;
import pl.allegro.tech.hermes.common.time.Clock;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceivingTimeoutException;

import java.util.List;
import java.util.Map;

public class KafkaMessageReceiver implements MessageReceiver {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageReceiver.class);

    private final ConsumerIterator<byte[], byte[]> iterator;
    private final Topic topic;
    private final ConsumerConnector consumerConnector;
    private final MessageContentWrapper contentWrapper;
    private final Timer readingTimer;
    private final Clock clock;

    public KafkaMessageReceiver(Topic topic, ConsumerConnector consumerConnector, MessageContentWrapper contentWrapper,
                                Timer readingTimer, Clock clock, KafkaNamesMapper kafkaNamesMapper, Integer kafkaStreamCount) {
        this.topic = topic;
        this.consumerConnector = consumerConnector;
        this.contentWrapper = contentWrapper;
        this.readingTimer = readingTimer;
        this.clock = clock;

        KafkaTopicName topicName = kafkaNamesMapper.toKafkaTopicName(topic);
        Map<String, Integer> topicCountMap = ImmutableMap.of(topicName.asString(), kafkaStreamCount);
        Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumerConnector.createMessageStreams(
                topicCountMap
        );
        KafkaStream<byte[], byte[]> stream = consumerMap.get(topicName.asString()).get(0);
        iterator = stream.iterator();
    }

    @Override
    public Message next() {
        try (Timer.Context readingTimerContext = readingTimer.time()) {
            MessageAndMetadata<byte[], byte[]> message = iterator.next();
            UnwrappedMessageContent unwrappedContent = contentWrapper.unwrap(message.message(), topic);

            return new Message(
                    unwrappedContent.getMessageMetadata().getId(),
                    message.offset(),
                    message.partition(),
                    topic.getQualifiedName(),
                    unwrappedContent.getContent(),
                    unwrappedContent.getMessageMetadata().getTimestamp(),
                    clock.getTime());

        } catch (ConsumerTimeoutException consumerTimeoutException) {
            throw new MessageReceivingTimeoutException("No messages received", consumerTimeoutException);
        } catch (Exception e) {
            throw new InternalProcessingException("Message received failed", e);
        }
    }

    @Override
    public void stop() {
        try {
            consumerConnector.shutdown();
        } catch (Throwable throwable) {
            logger.error("Error while shutting down", throwable);
        }
    }

}
