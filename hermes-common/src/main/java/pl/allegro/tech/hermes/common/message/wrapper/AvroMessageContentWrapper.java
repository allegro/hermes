package pl.allegro.tech.hermes.common.message.wrapper;

import com.google.common.collect.Maps;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import pl.allegro.tech.hermes.api.TraceInfo;

import java.util.Map;

import static java.util.Optional.ofNullable;
import static pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter.bytesToRecord;
import static pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter.recordToBytes;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_MARKER;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_MESSAGE_ID_KEY;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_TIMESTAMP_KEY;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_TRACE_ID_KEY;

public class AvroMessageContentWrapper {

    @SuppressWarnings("unchecked")
    UnwrappedMessageContent unwrapContent(byte[] data, Schema schema) {
        try {
            GenericRecord record = bytesToRecord(data, schema);
            Map<Utf8, Utf8> metadata = (Map<Utf8, Utf8>) record.get(METADATA_MARKER);
            return new UnwrappedMessageContent(
                new MessageMetadata(
                    Long.parseLong(metadata.get(METADATA_TIMESTAMP_KEY).toString()),
                    metadata.get(METADATA_MESSAGE_ID_KEY).toString(),
                    ofNullable(metadata.get(METADATA_TRACE_ID_KEY)).map(Object::toString).orElse(null)),
                data);
        } catch (Exception exception) {
            throw new UnwrappingException("Could not read avro message", exception);
        }
    }

    byte[] wrapContent(byte[] message, String id, TraceInfo traceInfo, long timestamp, Schema schema) {
        try {
            GenericRecord genericRecord = bytesToRecord(message, schema);
            Map<Utf8, String> metadata = Maps.newHashMap();
            metadata.put(METADATA_TIMESTAMP_KEY, Long.toString(timestamp));
            metadata.put(METADATA_MESSAGE_ID_KEY, id);
            ofNullable(traceInfo.getTraceId()).ifPresent((val) -> metadata.put(METADATA_TRACE_ID_KEY, val));

            genericRecord.put(METADATA_MARKER, metadata);
            return recordToBytes(genericRecord, schema);
        } catch (Exception exception) {
            throw new WrappingException("Could not wrap avro message", exception);
        }
    }

}
