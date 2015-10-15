package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import kafka.api.FetchRequestBuilder;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.message.MessageAndOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.SimpleConsumerPool;
import pl.allegro.tech.hermes.management.domain.topic.SingleMessageReaderException;

import java.nio.ByteBuffer;

import static java.lang.String.format;

public class KafkaRawMessageReader {

    private static final Logger logger = LoggerFactory.getLogger(KafkaRawMessageReader.class);

    private final SimpleConsumerPool simpleConsumerPool;

    public KafkaRawMessageReader(SimpleConsumerPool simpleConsumerPool) {
        this.simpleConsumerPool = simpleConsumerPool;
    }

    public byte[] readMessage(KafkaTopic topic, int partition, long offset) {
        FetchResponse fetchResponse = fetch(topic, partition, offset);
        for (MessageAndOffset messageAndOffset : fetchResponse.messageSet(topic.name().asString(), partition)) {
            if (messageAndOffset.offset() == offset) {
                return readPayloadAsBytes(messageAndOffset);
            }
            logger.info("Found an old offset: {} Expecting: {}", messageAndOffset.offset(), offset);
        }
        throw messageReaderException(topic, partition, offset, fetchResponse, "Cannot find message");
    }

    private byte[] readPayloadAsBytes(MessageAndOffset messageAndOffset) {
        ByteBuffer payload = messageAndOffset.message().payload();
        byte[] bytes = new byte[payload.limit()];
        payload.get(bytes);
        return bytes;
    }

    private FetchResponse fetch(KafkaTopic topic, int partition, long offset) {
        SimpleConsumer simpleConsumer = simpleConsumerPool.get(topic, partition);
        FetchResponse fetchResponse = simpleConsumer.fetch(new FetchRequestBuilder()
                .clientId(simpleConsumer.clientId())
                .addFetch(topic.name().asString(), partition, offset, simpleConsumerPool.getBufferSize())
                .build()
        );
        if (fetchResponse.hasError()) {
            throw messageReaderException(topic, partition, offset, fetchResponse, "Cannot read offset");
        }
        return fetchResponse;
    }

    private SingleMessageReaderException messageReaderException(KafkaTopic topic, int partition, long offset, FetchResponse response, String message) {
        String cause = message + format("[offset %d, kafka_topic %s, partition %d, kafka_response_code: %d]",
                offset, topic.name().asString(), partition, response.errorCode(topic.name().asString(), partition));
        logger.error(cause);
        return new SingleMessageReaderException(cause);
    }
}
