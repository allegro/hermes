package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import kafka.api.FetchRequest;
import kafka.api.FetchRequestBuilder;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.message.MessageAndOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.SimpleConsumerPool;
import pl.allegro.tech.hermes.management.domain.topic.SingleMessageReader;
import pl.allegro.tech.hermes.management.domain.topic.SingleMessageReaderException;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class KafkaSingleMessageReader implements SingleMessageReader {

    private static final Logger logger = LoggerFactory.getLogger(KafkaSingleMessageReader.class);

    private final SimpleConsumerPool simpleConsumerPool;

    public KafkaSingleMessageReader(SimpleConsumerPool simpleConsumerPool) {
        this.simpleConsumerPool = simpleConsumerPool;
    }

    @Override
    public String readMessage(TopicName topic, int partition, long offset) {
        SimpleConsumer simpleConsumer = simpleConsumerPool.get(topic.qualifiedName(), partition);

        FetchResponse fetchResponse = simpleConsumer.fetch(
            createFetchRequest(simpleConsumer.clientId(), topic, partition, offset, simpleConsumerPool.getBufferSize())
        );

        String topicName = topic.qualifiedName();
        if (fetchResponse.hasError()) {
            logger.error("Cannot read offset {} from topic/partition {}/{}. Error code: {}",
                    offset, topic.qualifiedName(), partition, fetchResponse.errorCode(topicName, partition));
            throw new SingleMessageReaderException(String.format("Cannot read offset %d from topic/partition %s/%d. Error code: %d",
                    offset, topic.qualifiedName(), partition, fetchResponse.errorCode(topicName, partition)));
        }

        for (MessageAndOffset messageAndOffset : fetchResponse.messageSet(topicName, partition)) {
            long currentOffset = messageAndOffset.offset();
            if (currentOffset < offset) {
                logger.info("Found an old offset: {} Expecting: {}", currentOffset, offset);
                continue;
            }
            ByteBuffer payload = messageAndOffset.message().payload();

            byte[] bytes = new byte[payload.limit()];
            payload.get(bytes);
            if (currentOffset == offset) {
                return new String(bytes, Charset.forName("UTF-8"));
            }
        }

        logger.error("Cannot find message by offset {} from topic/partition {}/{}. Error code: {}",
                offset, topic.qualifiedName(), partition, fetchResponse.errorCode(topicName, partition));
        throw new SingleMessageReaderException(String.format(
                "Cannot find message by offset %d from topic/partition %s/%d. Error code: %d",
                offset, topic.qualifiedName(), partition, fetchResponse.errorCode(topicName, partition)));
    }

    private FetchRequest createFetchRequest(String clientId, TopicName topic, int partition, long offset, int bufferSize) {
        return  new FetchRequestBuilder()
                .clientId(clientId)
                .addFetch(topic.qualifiedName(), partition, offset, bufferSize)
                .build();
    }
}
