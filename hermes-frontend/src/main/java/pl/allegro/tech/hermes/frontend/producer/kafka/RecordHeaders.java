package pl.allegro.tech.hermes.frontend.producer.kafka;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.apache.kafka.common.header.internals.RecordHeader;

class RecordHeaders {

    public static final String MESSAGE_ID_HEADER_NAME = "id";
    public static final String TIMESTAMP_HEADER_NAME = "ts";
    public static final String SCHEMA_VERSION_HEADER_NAME = "sv";

    static RecordHeader messageId(String messageId) {
        return new RecordHeader(MESSAGE_ID_HEADER_NAME, messageId.getBytes());
    }

    static RecordHeader timestamp(long timestamp) {
        return new RecordHeader(TIMESTAMP_HEADER_NAME, Longs.toByteArray(timestamp));
    }

    static RecordHeader schemaVersion(int schemaVersion) {
        return new RecordHeader(SCHEMA_VERSION_HEADER_NAME, Ints.toByteArray(schemaVersion));
    }
}
