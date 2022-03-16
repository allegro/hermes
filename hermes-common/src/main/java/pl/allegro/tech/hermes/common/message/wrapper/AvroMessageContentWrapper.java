package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
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

/** This class deals only with wrapping and unwrapping messages.
 *  It does not generate any Hermes ID for message in case it is missing.
 *  Missing Hermes ID will be left as empty.
 */
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
            return new MessageMetadata(timestamp, Collections.EMPTY_MAP);
        } else {
            long timestamp = metadata.containsKey(METADATA_TIMESTAMP_KEY) ? timestampFromMetadata(metadata) :
                    clock.millis();
            Map<String, String> extractedMetadata = extractMetadata(metadata);

            return metadata.containsKey(METADATA_MESSAGE_ID_KEY) ?
                    new MessageMetadata(timestamp, messageIdFromMetadata(metadata), extractedMetadata) : new MessageMetadata(timestamp, extractedMetadata);
        }
    }

    byte[] wrapContent(byte[] message, String id, long timestamp, Schema schema, Map<String, String> externalMetadata) {
        if (schema.getField(METADATA_MARKER) != null) {
            GenericRecord genericRecord = bytesToRecord(message, schema);
            try {
                genericRecord.put(METADATA_MARKER, metadataMap(id, timestamp, externalMetadata));
                return recordToBytes(genericRecord, schema);
            } catch (Exception e) {
                if (e instanceof AvroRuntimeException && e.getMessage().equals("Not a valid schema field: __metadata")) {
                    throw new AvroInvalidMetadataException("Schema does not contain mandatory __metadata field for Hermes internal metadata. Please fix topic schema.", e);
                }
                throw new WrappingException("Could not wrap avro message", e);
            }
        } else {
            return message;
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
