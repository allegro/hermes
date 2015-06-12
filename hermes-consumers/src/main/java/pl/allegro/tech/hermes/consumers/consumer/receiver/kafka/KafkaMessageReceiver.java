package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableMap;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.ConsumerTimeoutException;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.MessageWithMetadata;
import pl.allegro.tech.hermes.common.time.Clock;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceivingTimeoutException;

import java.util.List;
import java.util.Map;

public class KafkaMessageReceiver implements MessageReceiver {
    private final ConsumerIterator<byte[], byte[]> iterator;
    private final ConsumerConnector consumerConnector;
    private final MessageContentWrapper contentWrapper;
    private final Timer readingTimer;
    private final Clock clock;

    public KafkaMessageReceiver(String topicName, ConsumerConnector consumerConnector, ConfigFactory configFactory,
                                MessageContentWrapper contentWrapper, Timer readingTimer, Clock clock) {
        this.consumerConnector = consumerConnector;
        this.contentWrapper = contentWrapper;
        this.readingTimer = readingTimer;
        this.clock = clock;

        Map<String, Integer> topicCountMap = ImmutableMap.of(
                topicName, configFactory.getIntProperty(Configs.KAFKA_STREAM_COUNT)
        );
        Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumerConnector.createMessageStreams(
                topicCountMap
        );
        KafkaStream<byte[], byte[]> stream = consumerMap.get(topicName).get(0);
        iterator = stream.iterator();
    }

    @Override
    public Message next() {
        Timer.Context readingTimerContext = readingTimer.time();
        try {
            MessageAndMetadata<byte[], byte[]> message = iterator.next();
            MessageWithMetadata unwrappedContent = contentWrapper.unwrapContent(message.message());

            return new Message(
                    unwrappedContent.getMessageMetadata().getId(),
                    message.offset(),
                    message.partition(),
                    message.topic(),
                    unwrappedContent.getContent(),
                    unwrappedContent.getMessageMetadata().getTimestamp(),
                    clock.getTime());

        } catch (ConsumerTimeoutException consumerTimeoutException) {
            throw new MessageReceivingTimeoutException("No messages received", consumerTimeoutException);
        } catch (Exception e) {
            throw new InternalProcessingException("Message received failed", e);
        } finally {
            readingTimerContext.close();
        }
    }

    @Override
    public void stop() {
        consumerConnector.shutdown();
    }

}
