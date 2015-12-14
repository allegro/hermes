package pl.allegro.tech.hermes.common.message.wrapper;

import com.google.common.collect.ImmutableMap;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import pl.allegro.tech.hermes.common.util.MessageId;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter.bytesToRecord;
import static pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter.recordToBytes;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_MARKER;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_MESSAGE_ID_KEY;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_TIMESTAMP_KEY;

public class AvroMessageContentWrapper {

    @SuppressWarnings("unchecked")
    UnwrappedMessageContent unwrapContent(byte[] data, Schema schema) {
        try {
            Map<Utf8, Utf8> metadata = (Map<Utf8, Utf8>) bytesToRecord(data, schema).get(METADATA_MARKER);

            long timestamp = getTimestamp(metadata);
            String messageId = getMessageId(metadata, timestamp);
            Map<String, String> externalMetadata = extractMetadata(metadata);

            return new UnwrappedMessageContent(new MessageMetadata(timestamp, messageId, externalMetadata), data);
        } catch (Exception exception) {
            throw new UnwrappingException("Could not read avro message", exception);
        }
    }
    byte[] wrapContent(byte[] message, String id, long timestamp, Schema schema, Map<String, String> externalMetadata) {
        try {
            GenericRecord genericRecord = bytesToRecord(message, schema);
            genericRecord.put(METADATA_MARKER, metadataMap(id, timestamp, externalMetadata));
            return recordToBytes(genericRecord, schema);
        } catch (Exception exception) {
            throw new WrappingException("Could not wrap avro message", exception);
        }
    }

    private static Map<Utf8, Utf8> metadataMap(String id, long timestamp, Map<String, String> externalMetadata) {
        ImmutableMap.Builder<Utf8, Utf8> builder = ImmutableMap.<Utf8, Utf8>builder();
        builder.put(METADATA_MESSAGE_ID_KEY, new Utf8(id));
        builder.put(METADATA_TIMESTAMP_KEY, new Utf8(Long.toString(timestamp)));
        externalMetadata.forEach((key, value) -> builder.put(new Utf8(key), new Utf8(value)));
        return builder.build();
    }

    private long getTimestamp(Map<Utf8, Utf8> metadata) {
        return metadata != null && metadata.containsKey(METADATA_TIMESTAMP_KEY) ? timestampFromMetadata(metadata) :
                System.currentTimeMillis();
    }

    private long timestampFromMetadata(Map<Utf8, Utf8> metadata) {
        return Long.parseLong(metadata.remove(METADATA_TIMESTAMP_KEY).toString());
    }

    private String getMessageId(Map<Utf8, Utf8> metadata, long timestamp) {
        return metadata != null && metadata.containsKey(METADATA_MESSAGE_ID_KEY) ? messageIdFromMetadata(metadata) :
                MessageId.forTimestamp(timestamp);
    }

    private String messageIdFromMetadata(Map<Utf8, Utf8> metadata) {
        return metadata.remove(METADATA_MESSAGE_ID_KEY).toString();
    }

    private static Map<String, String> extractMetadata(Map<Utf8, Utf8> metadata) {
        if (metadata == null) {
            return Collections.EMPTY_MAP;
        }

        return metadata.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toString(),
                        entry -> entry.getValue().toString()
                ));
    }
}
