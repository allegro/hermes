package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import kafka.api.FetchRequestBuilder;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.message.MessageAndOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.SimpleConsumerPool;
import pl.allegro.tech.hermes.common.message.converter.AvroToJsonConverter;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageContentWrapper;
import pl.allegro.tech.hermes.management.domain.topic.SingleMessageReader;
import pl.allegro.tech.hermes.management.domain.topic.SingleMessageReaderException;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static java.lang.String.format;

public class KafkaSingleMessageReader implements SingleMessageReader {

    private static final Logger logger = LoggerFactory.getLogger(KafkaSingleMessageReader.class);

    private final SimpleConsumerPool simpleConsumerPool;
    private final AvroMessageContentWrapper avroMessageContentWrapper;

    public KafkaSingleMessageReader(SimpleConsumerPool simpleConsumerPool, AvroMessageContentWrapper avroMessageContentWrapper) {
        this.simpleConsumerPool = simpleConsumerPool;
        this.avroMessageContentWrapper = avroMessageContentWrapper;
    }

    @Override
    public String readMessage(Topic topic, int partition, long offset) {
        byte[] bytes = readMessage(topic.getQualifiedName(), partition, offset);
        if (topic.getContentType() == Topic.ContentType.AVRO) {
            bytes = convertAvroToJson(topic.getMessageSchema(), bytes);
        }
        return new String(bytes, Charset.forName("UTF-8"));
    }

    private byte[] convertAvroToJson(String schema, byte[] bytes) {
        return new AvroToJsonConverter(avroMessageContentWrapper.getWrappedSchema(schema)).convert(bytes);
    }

    private byte[] readMessage(String topicName, int partition, long offset) {
        FetchResponse fetchResponse = fetch(topicName, partition, offset);
        for (MessageAndOffset messageAndOffset : fetchResponse.messageSet(topicName, partition)) {
            if (messageAndOffset.offset() == offset) {
                return readPayloadAsBytes(messageAndOffset);
            }
            logger.info("Found an old offset: {} Expecting: {}", messageAndOffset.offset(), offset);
        }
        throw messageReaderException(topicName, partition, offset, fetchResponse, "Cannot find message");
    }

    private byte[] readPayloadAsBytes(MessageAndOffset messageAndOffset) {
        ByteBuffer payload = messageAndOffset.message().payload();
        byte[] bytes = new byte[payload.limit()];
        payload.get(bytes);
        return bytes;
    }

    private FetchResponse fetch(String topicName, int partition, long offset) {
        SimpleConsumer simpleConsumer = simpleConsumerPool.get(topicName, partition);
        FetchResponse fetchResponse = simpleConsumer.fetch(new FetchRequestBuilder()
                .clientId(simpleConsumer.clientId())
                .addFetch(topicName, partition, offset, simpleConsumerPool.getBufferSize())
                .build()
        );
        if (fetchResponse.hasError()) {
            throw messageReaderException(topicName, partition, offset, fetchResponse, "Cannot read offset");
        }
        return fetchResponse;
    }

    private SingleMessageReaderException messageReaderException(String topicName, int partition, long offset, FetchResponse response, String message) {
        String cause = message + format("[offset %d, topic %s, partition %d, kafka_response_code: %d]",
                offset, topicName, partition, response.errorCode(topicName, partition));
        logger.error(cause);
        return new SingleMessageReaderException(cause);
    }
}
