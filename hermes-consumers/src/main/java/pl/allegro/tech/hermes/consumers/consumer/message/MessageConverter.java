package pl.allegro.tech.hermes.consumers.consumer.message;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;

import java.util.Map;

import static java.util.stream.Collectors.toMap;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_MESSAGE_ID_KEY;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_TIMESTAMP_KEY;

public class MessageConverter {

    public static MessageMetadata toMessageMetadata(Message message, Subscription subscription) {
        return new MessageMetadata(message.getId(),
                message.getOffset(),
                message.getPartition(),
                message.getPartitionAssignmentTerm(),
                message.getTopic(),
                subscription.getName(),
                message.getKafkaTopic().asString(),
                message.getPublishingTimestamp(),
                message.getReadingTimestamp(),
                extractExtraRequestHeaders(message));
    }

    public static MessageMetadata toMessageMetadata(Message message, Subscription subscription, String batchId) {
        return new MessageMetadata(message.getId(),
                batchId,
                message.getOffset(),
                message.getPartition(),
                message.getPartitionAssignmentTerm(),
                subscription.getQualifiedTopicName(),
                subscription.getName(),
                message.getKafkaTopic().asString(),
                message.getPublishingTimestamp(),
                message.getReadingTimestamp(),
                extractExtraRequestHeaders(message));
    }

    private static Map<String, String> extractExtraRequestHeaders(Message message) {
        return message.getExternalMetadata()
                .entrySet()
                .stream()
                .filter(MessageConverter::isExternalMetadataOriginatingInRequestHeaders)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static boolean isExternalMetadataOriginatingInRequestHeaders(Map.Entry<String, String> externalMetadataEntry) {
        String externalMetadataKey = externalMetadataEntry.getKey();
        return !externalMetadataKey.equals(METADATA_MESSAGE_ID_KEY.toString()) &&
                !externalMetadataKey.equals(METADATA_TIMESTAMP_KEY.toString());
    }
}
