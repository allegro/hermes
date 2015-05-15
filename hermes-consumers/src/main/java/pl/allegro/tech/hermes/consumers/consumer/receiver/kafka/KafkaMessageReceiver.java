package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import com.google.common.collect.ImmutableMap;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.ConsumerTimeoutException;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.json.MessageContentWrapper;
import pl.allegro.tech.hermes.common.json.UnwrappedMessageContent;
import pl.allegro.tech.hermes.consumers.consumer.message.RawMessage;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;

public class KafkaMessageReceiver implements MessageReceiver {
    private static final String ID = "id";
    private static final String TIMESTAMP = "timestamp";

    private final ConsumerIterator<byte[], byte[]> iterator;
    private final ConsumerConnector consumerConnector;
    private final MessageContentWrapper contentWrapper;
    private final String topicName;

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMessageReceiver.class);

    public KafkaMessageReceiver(String topicName, ConsumerConnector consumerConnector, ConfigFactory configFactory,
                                MessageContentWrapper contentWrapper) {
        this.topicName = topicName;
        this.consumerConnector = consumerConnector;
        this.contentWrapper = contentWrapper;

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
    public RawMessage next() {
        try {
            MessageAndMetadata<byte[], byte[]> message = iterator.next();
            UnwrappedMessageContent unwrappedContent = contentWrapper.unwrapContent(message.message());
            return new RawMessage(
                    unwrappedContent.getStringFromMetadata(ID),
                    message.offset(),
                    message.partition(),
                    message.topic(),
                    unwrappedContent.getContent(),
                    unwrappedContent.getLongFromMetadata(TIMESTAMP)
            );
        } catch (ConsumerTimeoutException consumerTimeoutException) {
            LOGGER.debug("No messages received");
            return new RawMessage();
        } catch (Exception e) {
            return catchInterruptedIfNeeded(e);
        }
    }

    //we must do this, because InterruptedException goes through scala which doesn't declare checked exceptions
    private RawMessage catchInterruptedIfNeeded(Exception e) {
        if (e instanceof InterruptedException) {
            LOGGER.warn(format("Reading message from topic %s was interrupted.", topicName), e);
            return new RawMessage();
        } else {
            throw new InternalProcessingException("Message received failed", e);
        }
    }

    @Override
    public void stop() {
        consumerConnector.shutdown();
    }

}
