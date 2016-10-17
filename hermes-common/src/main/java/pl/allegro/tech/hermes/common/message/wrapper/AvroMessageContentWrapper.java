package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import pl.allegro.tech.hermes.common.util.MessageId;
import pl.allegro.tech.hermes.schema.CompiledSchema;

import javax.inject.Inject;
import java.time.Clock;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter.bytesToRecord;
import static pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter.recordToBytes;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_MARKER;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_MESSAGE_ID_KEY;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_TIMESTAMP_KEY;

public class AvroMessageContentWrapper {

    private final Clock clock;

    @Inject
    public AvroMessageContentWrapper(Clock clock) {
        this.clock = clock;
    }

    @SuppressWarnings("unchecked")
    UnwrappedMessageContent unwrapContent(byte[] data, CompiledSchema<Schema> schema) {
        try {
            Map<Utf8, Utf8> metadata = (Map<Utf8, Utf8>) bytesToRecord(data, schema.getSchema()).get(METADATA_MARKER);
            MessageMetadata messageMetadata = getMetadata(metadata);

            return new UnwrappedMessageContent(messageMetadata, data, schema);
        } catch (Exception exception) {
            throw new UnwrappingException("Could not read avro message", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private MessageMetadata getMetadata(Map<Utf8, Utf8> metadata) {
        if (metadata == null) {
            long timestamp = clock.millis();
            return new MessageMetadata(timestamp, MessageId.forTimestamp(timestamp), Collections.EMPTY_MAP);
        } else {
            long timestamp = metadata.containsKey(METADATA_TIMESTAMP_KEY) ? timestampFromMetadata(metadata) : clock.millis();
            String messageId = metadata.containsKey(METADATA_MESSAGE_ID_KEY) ? messageIdFromMetadata(metadata) :
                    MessageId.forTimestamp(timestamp);

            return new MessageMetadata(timestamp, messageId, extractMetadata(metadata));
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

    private Map<Utf8, Utf8> metadataMap(String id, long timestamp, Map<String, String> externalMetadata) {
        Map<Utf8, Utf8> metadata = new HashMap<>();
        metadata.put(METADATA_MESSAGE_ID_KEY, new Utf8(id));
        metadata.put(METADATA_TIMESTAMP_KEY, new Utf8(Long.toString(timestamp)));

        externalMetadata.forEach((key, value) -> metadata.put(new Utf8(key), new Utf8(value)));
        return metadata;
    }

    private long timestampFromMetadata(Map<Utf8, Utf8> metadata) {
        return Long.parseLong(metadata.remove(METADATA_TIMESTAMP_KEY).toString());
    }

    private String messageIdFromMetadata(Map<Utf8, Utf8> metadata) {
        return metadata.remove(METADATA_MESSAGE_ID_KEY).toString();
    }

    private Map<String, String> extractMetadata(Map<Utf8, Utf8> metadata) {
        return Optional.ofNullable(metadata).orElse(Collections.emptyMap())
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toString(),
                        entry -> entry.getValue().toString()
                ));
    }
}
