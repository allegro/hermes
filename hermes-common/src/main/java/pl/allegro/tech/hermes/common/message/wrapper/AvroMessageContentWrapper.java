package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;

import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;
import static pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter.bytesToRecord;
import static pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter.recordToBytes;

public class AvroMessageContentWrapper {

    public static final String METADATA_MARKER = "__metadata";
    public static final Utf8 METADATA_TIMESTAMP_KEY = new Utf8("timestamp");
    public static final Utf8 METADATA_MESSAGE_ID_KEY = new Utf8("messageId");

    @SuppressWarnings("unchecked")
    public UnwrappedMessageContent unwrapContent(byte[] data, Schema schema) {
        try {
            GenericRecord record = bytesToRecord(data, schema);
            Map<Utf8, Utf8> metadata = (Map<Utf8, Utf8>) record.get(METADATA_MARKER);
            return new UnwrappedMessageContent(
                new MessageMetadata(
                    Long.valueOf(metadata.get(METADATA_TIMESTAMP_KEY).toString()),
                    metadata.get(METADATA_MESSAGE_ID_KEY).toString()),
                data);
        } catch (Exception exception) {
            throw new UnwrappingException("Could not read avro message", exception);
        }
    }

    public byte[] wrapContent(byte[] message, String id, long timestamp, Schema schema) {
        try {
            GenericRecord genericRecord = bytesToRecord(message, schema);
            genericRecord.put(METADATA_MARKER, of(
                METADATA_TIMESTAMP_KEY, Long.toString(timestamp),
                METADATA_MESSAGE_ID_KEY, id
            ));
            return recordToBytes(genericRecord, schema);
        } catch (Exception exception) {
            throw new WrappingException("Could not wrap avro message", exception);
        }
    }

}
