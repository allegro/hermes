package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.Schema;

import javax.inject.Inject;
import java.util.Map;

public class SchemaVersionAwareAvroMessageContentWrapper {

    private final AvroMessageContentWrapper avroMessageContentWrapper;

    @Inject
    public SchemaVersionAwareAvroMessageContentWrapper(AvroMessageContentWrapper avroMessageContentWrapper) {
        this.avroMessageContentWrapper = avroMessageContentWrapper;
    }

    UnwrappedMessageContent unwrapContent(byte[] data, Schema schema) {
        return avroMessageContentWrapper.unwrapContent(data, schema);
    }

    byte[] wrapContent(byte[] message, String id, long timestamp, Schema schema, Map<String, String> externalMetadata) {
        return avroMessageContentWrapper.wrapContent(message, id, timestamp, schema, externalMetadata);
    }

}
